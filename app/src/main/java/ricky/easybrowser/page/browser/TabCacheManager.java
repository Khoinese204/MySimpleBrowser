package ricky.easybrowser.page.browser;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.LruCache;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.util.ArrayList;
import java.util.List;

import ricky.easybrowser.R;
import ricky.easybrowser.contract.IBrowser;
import ricky.easybrowser.contract.ITab;
import ricky.easybrowser.contract.ITabQuickView;
import ricky.easybrowser.entity.bo.TabInfo;
import ricky.easybrowser.page.tab.NewTabFragmentV2;

/**
 * Bộ nhớ cache trang dùng thuật toán LRU. Đảm nhận việc lưu trữ và điều khiển logic hiển thị của các tab.
 */
public class TabCacheManager implements IBrowser.ITabController {

    private final Context mContext;
    private final FragmentManager fm;
    private int browserLayoutId;

    private ITabQuickView.Observer observer;

    private LruCache<TabInfo, Fragment> lruCache;
    private final List<TabInfo> infoList = new ArrayList<>();

    public TabCacheManager(Context context, FragmentManager manager, int maxSize, int layoutId) {
        this.mContext = context;
        this.fm = manager;
        this.browserLayoutId = layoutId;
        lruCache = new LruCache<TabInfo, Fragment>(maxSize) {
            @Override
            protected void entryRemoved(boolean evicted, TabInfo key, Fragment oldValue, Fragment newValue) {
                /**
                 * Sau khi trang tab đã bị loại bỏ hoặc thay thế, thực hiện hoạt động xóa.
                 */
                if (fm == null || key == null) {
                    return;
                }

                if (oldValue != null) {
                    fm.beginTransaction().remove(oldValue).commitAllowingStateLoss();
                }
            }
        };
    }

    /**
     * Phục hồi bộ nhớ cache trang tab, sử dụng thông tin TabInfo được phục hồi từ Fragment để tạo đối tượng TabInfo.
     * <p>
     * Phương pháp này chỉ phục hồi một trang tab, lớp trên có thể cần gọi trong vòng lặp.
     *
     * @param infoCopy Đối tượng TabInfo được phục hồi từ tham số Fragment, khác với giá trị hash trong danh sách phục hồi, cần kiểm tra khi đặt.
     * @param fragment Fragment đích
     */
    private void restoreTabCache(TabInfo infoCopy, @Nullable Fragment fragment) {
        int prevIndex = -1;
        for (int i = 0; i < infoList.size(); i++) {
            if (infoCopy.equals(infoList.get(i))) {
                prevIndex = i;
                break;
            }
        }
        if (prevIndex >= 0) {
            // Nếu có bộ nhớ cache trước đó, đặt trực tiếp vào bộ nhớ cache mà không cập nhật danh sách, cần lấy TabInfo thực sự từ infoList.
            TabInfo info = infoList.get(prevIndex);
            lruCache.put(info, fragment);
        } else {
            // Mục này không tồn tại trong danh sách bộ nhớ cache.
            infoList.add(infoCopy);
            lruCache.put(infoCopy, fragment);
        }
    }

    private void addToCache(TabInfo info, Fragment fragment) {
        int prevIndex = -1;
        for (int i = 0; i < infoList.size(); i++) {
            if (info.equals(infoList.get(i))) {
                prevIndex = i;
                break;
            }
        }
        if (prevIndex >= 0) {
            // Nếu trước đó đã có bộ nhớ cache, thì đặt trực tiếp vào bộ nhớ cache mà không cập nhật danh sách.
            lruCache.put(info, fragment);
        } else {
            infoList.add(info);
            lruCache.put(info, fragment);
        }
    }

    private Fragment getFromCache(TabInfo info) {
        return lruCache.get(info);
    }

    private void removeFromCache(TabInfo info) {
        lruCache.remove(info);  // Ném ra lỗi khi tag là null

        // Chỉ khi người dùng thực hiện tác động tự động, thì mới loại bỏ tag khỏi danh sách được sử dụng bởi recyclerview
        for (int i = 0; i < infoList.size(); i++) {
            if (info.equals(infoList.get(i))) {
                infoList.remove(i);
                break;
            }
        }
    }

    private void closeAllTabs() {
        lruCache.evictAll();
        infoList.clear();
    }

    /**
     * Khi người dùng nhấp vào tab, chuyển đến trang mục tiêu.
     *
     * @param info Thông tin về tab được chọn
     */
    private void switchToTab(TabInfo info) {
        Fragment current = findVisibleFragment(fm);
        Fragment target = getFromCache(info);

        if (current == null) {
            // Hiện không có Fragment nào được hiển thị, hiển thị trực tiếp, tương ứng với trường hợp một trang đã bị đóng
            fm.beginTransaction().show(target).commit();
            return;
        }

        if (target != null) {
            // Nhấp vào trang đã được lưu trữ trong bộ nhớ cache, thay thế hiển thị bằng Fragment mới
            fm.beginTransaction().hide(current).show(target).commit();
        } else {
            // Không có trang nào được lưu trữ trong cache, trang gốc đã bị hủy bỏ. Tạo lại Fragment mới, tái sử dụng tag và đưa vào bộ nhớ cache
            NewTabFragmentV2 fragmentToAdd = NewTabFragmentV2.newInstance();
            TabInfo fragmentInfo = info;
            fm.beginTransaction().hide(current).add(browserLayoutId, fragmentToAdd, info.getTag()).commit();
            addToCache(fragmentInfo, fragmentToAdd);
        }
    }

    private void addNewTab(TabInfo info, boolean backstage) {
        if (fm == null || info == null) {
            return;
        }
        Fragment current = findVisibleFragment(fm);
        NewTabFragmentV2 fragmentToAdd = NewTabFragmentV2.newInstance(info.getTitle(), info.getTag(), info.getUri());
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.add(browserLayoutId, fragmentToAdd);
        if (current != null && !backstage) {
            transaction.hide(current);
        } else if (current != null) {
            transaction.hide(fragmentToAdd);
        }
        transaction.commit();
        addToCache(info, fragmentToAdd);
        if (observer != null) {
            observer.updateQuickView();
        }
    }

    /**
     * Đóng trang tab
     * <ul>
     * <li>Nếu tất cả các trang tab đều đã được đóng, thì tạo một trang tab mới</li>
     * <li>Nếu đóng trang tab đầu tiên, hiển thị trang tab mới đầu tiên</li>
     * <li>Nếu đóng trang tab ở vị trí giữa, hiển thị trang tab ở vị trí trước</li>
     * </ul>
     *
     * @param info Thông tin về trang tab cần đóng
     */
    private void closeTab(TabInfo info) {
        int orgIndex = findTabIndex(info);
        removeFromCache(info);
        if (observer != null) {
            observer.updateQuickView();
        }

        if (infoList.size() <= 0 && observer != null) {
            TabInfo tabInfo = TabInfo.create(
                    System.currentTimeMillis() + "",
                    mContext.getString(R.string.new_tab_welcome));
            onTabCreate(tabInfo, false);
            return;
        }

        if (orgIndex <= 0) {
            switchToTab(infoList.get(0));
        } else {
            switchToTab(infoList.get(orgIndex - 1));
        }
    }

    private int findTabIndex(TabInfo info) {
        int index = -1;
        for (int i = 0; i < infoList.size(); i++) {
            if (info.equals(infoList.get(i))) {
                index = i;
                break;
            }
        }
        return index;
    }

    private int findTabByTag(String tag) {
        int index = -1;
        for (int i = 0; i < infoList.size(); i++) {
            if (tag.equals(infoList.get(i).getTag())) {
                index = i;
                break;
            }
        }
        return index;
    }

    /**
     * Tìm kiếm Fragment hiển thị trong FragmentManager hiện tại
     *
     * @param fm FragmentManager hiện tại
     * @return Fragment hiển thị, nếu có
     */
    private Fragment findVisibleFragment(FragmentManager fm) {
        if (fm == null) {
            return null;
        }
        Fragment current = null;
        List<Fragment> fragments = fm.getFragments();
        for (int i = 0; i < fragments.size(); i++) {
            final Fragment tmp = fragments.get(i);
            if ((!tmp.isHidden()) && (tmp instanceof ITab)) {
                current = fragments.get(i);
            }
        }
        return current;
    }

    @Override
    public void attach(ITabQuickView.Observer observer) {
        this.observer = observer;
    }

    @Override
    public void detach() {
        this.observer = null;
    }

    @Override
    public List<TabInfo> provideInfoList() {
        return this.infoList;
    }

    @Override
    public void updateTabInfo(TabInfo tabInfo) {
        try {
            int i = findTabIndex(tabInfo);
            if (i < 0) {
                return;
            }
            String nTitle = tabInfo.getTitle();

            infoList.get(i).setTitle(nTitle);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (observer != null) {
            observer.updateQuickView();
        }
    }

    @Override
    public void onTabSelected(TabInfo tabInfo) {
        switchToTab(tabInfo);
    }

    @Override
    public void onTabClose(TabInfo tabInfo) {
        closeTab(tabInfo);
    }

    @Override
    public void onTabCreate(TabInfo tabInfo, boolean backstage) {
        addNewTab(tabInfo, backstage);
    }

    @Override
    public void onTabGoHome() {
        Fragment target = findVisibleFragment(fm);
        if (target == null) {
            return;
        }

        if (target instanceof ITab) {
            ((ITab) target).gotoHomePage();
        }
    }

    @Override
    public void onTabGoForward() {
        Fragment target = findVisibleFragment(fm);
        if (target == null) {
            return;
        }

        if (target instanceof ITab) {
            ((ITab) target).goForward();
        }
    }

    @Override
    public void onTabLoadUrl(String url) {
        Fragment target = findVisibleFragment(fm);
        if (target == null) {
            return;
        }

        if (target instanceof ITab) {
            ((ITab) target).loadUrl(url);
        }
    }

    @Override
    public void onRestoreTabCache(TabInfo infoCopy, @Nullable Fragment fragment) {
        restoreTabCache(infoCopy, fragment);
    }

    @Override
    public void onCloseAllTabs() {
        closeAllTabs();
    }

    @Override
    public void onDestroy() {

    }

    @Override
    public TabInfo getCurrentTab() {
        if (this.fm == null) {
            return null;
        }

        Fragment fragment = findVisibleFragment(this.fm);
        if (fragment instanceof ITab) {
            TabInfo info = ((ITab) fragment).provideTabInfo();
            return info;
        }
        return null;
    }

    @Override
    public Bitmap getPreviewForTab(TabInfo tabInfo) {
        Fragment fragment = getFromCache(tabInfo);
        if (fragment instanceof ITab) {
            return ((ITab) fragment).getTabPreview();
        }
        return null;
    }
}

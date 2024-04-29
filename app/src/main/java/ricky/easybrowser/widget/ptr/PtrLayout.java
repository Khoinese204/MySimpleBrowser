package ricky.easybrowser.widget.ptr;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class PtrLayout extends SwipeRefreshLayout {

    public int pageSize = 20;
    public boolean hasMore = false;
    public boolean loading = false;

    private RecyclerView mRecyclerView;

    private View loadMoreView;

    private OnLoadListener mOnLoadListener;

    public PtrLayout(Context context) {
        super(context);
    }

    public PtrLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        findRecyclerView();
        super.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (!loading && mOnLoadListener != null) {
                    mOnLoadListener.onRefresh();
                }
            }
        });
    }

    @Override
    public void setOnRefreshListener(OnRefreshListener listener) {
        throw new RuntimeException("USE PtrLayout::setOnLoadListener INSTEAD!!!");
    }

    private void findRecyclerView() {
        if (getChildCount() > 0) {
            View target = getChildAt(0);
            if (target instanceof RecyclerView) {
                mRecyclerView = (RecyclerView) target;
                mRecyclerView.clearOnScrollListeners();
                mRecyclerView.addOnScrollListener(new PtrOnScrollListener());
            }
        }
    }

    public OnLoadListener getOnLoadListener() {
        return mOnLoadListener;
    }

    /**
     * Thiết lập trình nghe tải
     */
    public void setOnLoadListener(OnLoadListener ltn) {
        this.mOnLoadListener = ltn;
    }

    public View getLoadMoreView() {
        return loadMoreView;
    }

    /**
     * Đặt để tải thêm Chế độ xem tùy chỉnh tương ứng
     * @param loadMoreView được sử dụng để hiển thị Chế độ xem tải nhiều văn bản/hình ảnh hơn
     */
    public void setLoadMoreView(View loadMoreView) {
        this.loadMoreView = loadMoreView;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int size) {
        this.pageSize = size;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    /**
     * Đặt xem có nhiều dữ liệu hơn hay không. Chức năng gọi lại tải sẽ chỉ có hiệu lực khi có nhiều dữ liệu hơn.
     */
    public void setHasMore(boolean more) {
        this.hasMore = more;
    }

    private boolean isLoading() {
        return loading;
    }

    private void setLoading(boolean isLoading) {
        this.loading = isLoading;
    }

    /**
     * Sau khi tải, đặt trạng thái dữ liệu
     */
    public void loadFinish(boolean emptyResult, boolean more) {
        loading = false;
        hasMore = more;
        if (loadMoreView != null) {
            loadMoreView.setVisibility(View.GONE);
        }
    }

    class PtrOnScrollListener extends RecyclerView.OnScrollListener {

        RecyclerView.LayoutManager layoutManager;
        int lastVisibleItem = 0;

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            if (recyclerView.getAdapter() == null) {
                lastVisibleItem = -1;
                return;
            }
            if (!loading
                    && (lastVisibleItem + 1) == mRecyclerView.getAdapter().getItemCount()) {
                if (loading || !hasMore) {
                    return;
                }

                if (mOnLoadListener != null) {
                    mOnLoadListener.onLoadMore();
                    if (loadMoreView != null) {
                        loadMoreView.setVisibility(View.VISIBLE);
                    }
                    loading = true;
                }
            }
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            if (recyclerView.getAdapter() == null) {
                lastVisibleItem = -1;
                return;
            }
            if (layoutManager == null) {
                layoutManager = recyclerView.getLayoutManager();
            }

            if (layoutManager instanceof LinearLayoutManager) {
                lastVisibleItem = ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition();
            } else if (layoutManager instanceof StaggeredGridLayoutManager) {
                int[] spanItem = ((StaggeredGridLayoutManager) layoutManager).findLastCompletelyVisibleItemPositions(null);
                lastVisibleItem = spanItem[0];
            }
        }
    }

    /**
     * Trình nghe tải
     */
    public interface OnLoadListener {
        /**
         * Chức năng tải thêm
         */
        void onLoadMore();

        /**
         * Chức năng làm mới
         */
        void onRefresh();
    }
}
package ricky.easybrowser.page.history;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.schedulers.Schedulers;
import ricky.easybrowser.EasyApplication;
import ricky.easybrowser.R;
import ricky.easybrowser.common.BrowserConst;
import ricky.easybrowser.contract.IBrowser;
import ricky.easybrowser.contract.IHistory;
import ricky.easybrowser.entity.dao.AppDatabase;
import ricky.easybrowser.entity.dao.History;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {


    private Context mContext;
    private final List<History> dataList = new ArrayList<>();

    private OnHistoryItemClickListener itemClickListener;

    private View itemView;

    public HistoryAdapter(Context context) {
        this.mContext = context;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        itemView = LayoutInflater.from(mContext).inflate(R.layout.layout_history_item, parent, false);
        HistoryViewHolder vh = new HistoryViewHolder(itemView);
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        if (dataList.size() <= position) {
            return;
        }
        final History entity = dataList.get(position);
        if (entity == null) {
            return;
        }
        holder.title.setText(entity.title);
        holder.url.setText(entity.url);

        holder.deleteIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int clickedPosition = holder.getAdapterPosition();
                if (clickedPosition != RecyclerView.NO_POSITION) {
                    // Lấy đối tượng History tương ứng với mục bị xóa
                    History historyToDelete = dataList.get(clickedPosition);
                    // Xóa mục khỏi danh sách trên UI
                    dataList.remove(clickedPosition);
                    notifyItemRemoved(clickedPosition);
                    notifyItemRangeChanged(clickedPosition, dataList.size());
                    // Xóa mục khỏi database trên 1 luồng khác
                    Disposable disposable = Completable.fromAction(new Action() {
                        @Override
                        public void run() throws Exception {
                            final EasyApplication application = (EasyApplication) mContext.getApplicationContext();
                            AppDatabase db = application.getAppDatabase();
                            db.historyDao().deleteHistory(historyToDelete);
                        }
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe();
                }
            }
        });



        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (itemClickListener != null) {
                    itemClickListener.onHistoryItemClick(entity);
                }
            }
        });

    }
    public void clearAllItems() {
        int size = dataList.size();
            // Xóa tất cả các mục từ UI
            dataList.clear();
            notifyItemRangeRemoved(0, size);

            // Xóa tất cả các mục từ database trên 1 luồng khác
            Disposable disposable = Completable.fromAction(new Action() {
                        @Override
                        public void run() throws Exception {
                            final EasyApplication application = (EasyApplication) mContext.getApplicationContext();
                            AppDatabase db = application.getAppDatabase();
                            db.historyDao().deleteAll();
                        }
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe();
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public void clearDataList() {
        dataList.clear();
    }

    public void appendDataList(List<History> list) {
        dataList.addAll(list);
    }

    public void setItemClickListener(OnHistoryItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {

        TextView title;
        TextView url;
        ImageView deleteIcon;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.history_title);
            url = itemView.findViewById(R.id.history_url);
            deleteIcon = itemView.findViewById(R.id.delete_icon);
        }
    }

    interface OnHistoryItemClickListener {
        void onHistoryItemClick(History entity);
    }
}

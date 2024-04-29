package ricky.easybrowser.page.history;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.List;

import ricky.easybrowser.R;
import ricky.easybrowser.entity.dao.History;

public class HistoryActivity extends AppCompatActivity {

    private static final String F_TAG = "history_fragment";

    private Toolbar toolbar;
    private ImageView history_trash_all;

    HistoryFragment historyFragment;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        toolbar = findViewById(R.id.toolbar);
        history_trash_all = findViewById(R.id.history_trash);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.history);

        }

        getSupportFragmentManager().beginTransaction()
                .add(R.id.content_frame, new HistoryFragment(), F_TAG).commit();

        history_trash_all.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(HistoryActivity.this);
                builder.setTitle("Xác nhận xóa lịch sử");
                builder.setMessage("Bạn có chắc chắn muốn xóa tất cả lịch sử xem không?");
                builder.setPositiveButton("Có", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        historyFragment = (HistoryFragment) getSupportFragmentManager().findFragmentByTag(F_TAG);
                        if (historyFragment != null)
                            historyFragment.clearAllItems();
                    }
                });
                builder.setNegativeButton("Không", null);
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

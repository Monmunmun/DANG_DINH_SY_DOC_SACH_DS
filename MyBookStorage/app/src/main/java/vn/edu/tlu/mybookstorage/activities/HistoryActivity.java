package vn.edu.tlu.mybookstorage.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vn.edu.tlu.mybookstorage.R;
import vn.edu.tlu.mybookstorage.adapters.HistoryBookAdapter;
import vn.edu.tlu.mybookstorage.models.ReadingHistoryEntry;

public class HistoryActivity extends AppCompatActivity {

    private static final String TAG = "HistoryActivity";

    private RecyclerView recyclerView;
    private HistoryBookAdapter historyAdapter;
    private List<ReadingHistoryEntry> historyList;
    private ProgressBar progressBar;
    private TextView tvNoHistory;

    private FirebaseAuth mAuth;
    private DatabaseReference mHistoryRef;
    private String mUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Lịch sử đọc");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        recyclerView = findViewById(R.id.recyclerViewHistory);
        progressBar = findViewById(R.id.progressBarHistory);
        tvNoHistory = findViewById(R.id.tvNoHistory);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        historyList = new ArrayList<>();
        historyAdapter = new HistoryBookAdapter(historyList, new HistoryBookAdapter.OnHistoryItemClickListener() {
            @Override
            public void onHistoryItemClick(ReadingHistoryEntry entry) {
                openBookFromHistory(entry);
            }
        });
        recyclerView.setAdapter(historyAdapter);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            mUserId = currentUser.getUid();
            mHistoryRef = FirebaseDatabase.getInstance().getReference("users")
                    .child(mUserId)
                    .child("history");
            loadReadingHistory();
        } else {
            Log.e(TAG, "Người dùng chưa đăng nhập. Không thể tải lịch sử.");
            progressBar.setVisibility(View.GONE);
            tvNoHistory.setVisibility(View.VISIBLE);
            tvNoHistory.setText("Bạn cần đăng nhập để xem lịch sử đọc.");
            Toast.makeText(this, "Vui lòng đăng nhập để xem lịch sử.", Toast.LENGTH_LONG).show();
        }
    }

    private void loadReadingHistory() {
        progressBar.setVisibility(View.VISIBLE);
        tvNoHistory.setVisibility(View.GONE);

        Query query = mHistoryRef.orderByChild("timestamp");

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                historyList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot historySnapshot : snapshot.getChildren()) {
                        ReadingHistoryEntry entry = historySnapshot.getValue(ReadingHistoryEntry.class);
                        if (entry != null) {
                            historyList.add(entry);
                        }
                    }
                    Collections.reverse(historyList);
                    historyAdapter.notifyDataSetChanged();
                    Log.d(TAG, "Đã tải " + historyList.size() + " mục lịch sử.");
                    if (historyList.isEmpty()) {
                        tvNoHistory.setVisibility(View.VISIBLE);
                        tvNoHistory.setText("Chưa có lịch sử đọc nào.");
                    } else {
                        tvNoHistory.setVisibility(View.GONE);
                    }
                } else {
                    Log.d(TAG, "Không tìm thấy lịch sử đọc.");
                    tvNoHistory.setVisibility(View.VISIBLE);
                    tvNoHistory.setText("Chưa có lịch sử đọc nào.");
                }
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Lỗi khi tải lịch sử đọc: " + error.getMessage());
                progressBar.setVisibility(View.GONE);
                tvNoHistory.setVisibility(View.VISIBLE);
                tvNoHistory.setText("Lỗi khi tải lịch sử: " + error.getMessage());
                Toast.makeText(HistoryActivity.this, "Lỗi khi tải lịch sử: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void openBookFromHistory(ReadingHistoryEntry entry) {

        Intent intent = new Intent(this, BookReaderActivity.class);
        intent.putExtra(BookReaderActivity.EXTRA_BOOK_ID, entry.getBookId());
        intent.putExtra(BookReaderActivity.EXTRA_BOOK_TITLE, entry.getBookTitle());
        intent.putExtra(BookReaderActivity.EXTRA_BOOK_URL, entry.getFilePath());
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
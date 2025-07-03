package vn.edu.tlu.mybookstorage.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
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
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import vn.edu.tlu.mybookstorage.R;
import vn.edu.tlu.mybookstorage.adapters.FavouriteBookAdapter;
import vn.edu.tlu.mybookstorage.models.Book;

public class FavoritesActivity extends AppCompatActivity implements FavouriteBookAdapter.OnFavouriteBookActionListener {

    private static final String TAG = "FavoritesActivity";

    private RecyclerView favoritesRecyclerView;
    private TextView noFavoritesTextView;
    private FavouriteBookAdapter favoriteBookAdapter;
    private List<Book> favoriteBookList;

    private FirebaseAuth mAuth;
    private DatabaseReference mBooksRef;
    private DatabaseReference mFavoritesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Sách Yêu Thích");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        favoritesRecyclerView = findViewById(R.id.favoritesRecyclerView);
        noFavoritesTextView = findViewById(R.id.noFavoritesTextView);

        mAuth = FirebaseAuth.getInstance();
        mBooksRef = FirebaseDatabase.getInstance().getReference("books");
        mFavoritesRef = FirebaseDatabase.getInstance().getReference("favorites");

        favoriteBookList = new ArrayList<>();
        favoritesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        favoriteBookAdapter = new FavouriteBookAdapter(favoriteBookList, this);
        favoritesRecyclerView.setAdapter(favoriteBookAdapter);

        loadFavoriteBookIds();
    }

    /**
     * Tải danh sách ID sách yêu thích từ Firebase Realtime Database.
     */
    private void loadFavoriteBookIds() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Bạn cần đăng nhập để xem danh sách yêu thích.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        mFavoritesRef.child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> bookIds = new ArrayList<>();
                for (DataSnapshot idSnapshot : snapshot.getChildren()) {
                    String bookId = idSnapshot.getValue(String.class);
                    if (bookId != null) {
                        bookIds.add(bookId);
                    }
                }
                fetchBookDetails(bookIds);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Lỗi khi tải ID sách yêu thích: " + error.getMessage());
                Toast.makeText(FavoritesActivity.this, "Lỗi khi tải danh sách yêu thích.", Toast.LENGTH_SHORT).show();
                updateNoFavoritesMessage();
            }
        });
    }

    /**
     * Tải thông tin chi tiết của từng cuốn sách dựa trên ID.
     */
    private void fetchBookDetails(List<String> bookIds) {
        if (bookIds == null || bookIds.isEmpty()) {
            favoriteBookList.clear();
            favoriteBookAdapter.notifyDataSetChanged();
            updateNoFavoritesMessage();
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;
        String userId = currentUser.getUid();

        favoriteBookList.clear();
        final int[] booksLoadedCount = {0};
        for (String bookId : bookIds) {
            mBooksRef.child(userId).child(bookId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Book book = snapshot.getValue(Book.class);
                    if (book != null) {
                        favoriteBookList.add(book);
                    }
                    booksLoadedCount[0]++;

                    if (booksLoadedCount[0] == bookIds.size()) {
                        favoriteBookAdapter.updateBookList(favoriteBookList);
                        updateNoFavoritesMessage();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Lỗi khi tải chi tiết sách: " + bookId, error.toException());
                    booksLoadedCount[0]++;
                    if (booksLoadedCount[0] == bookIds.size()) {
                        favoriteBookAdapter.updateBookList(favoriteBookList);
                        updateNoFavoritesMessage();
                    }
                }
            });
        }
    }

    /**
     * Cập nhật trạng thái hiển thị của thông báo "Chưa có sách yêu thích".
     */
    private void updateNoFavoritesMessage() {
        if (favoriteBookList.isEmpty()) {
            noFavoritesTextView.setVisibility(View.VISIBLE);
            favoritesRecyclerView.setVisibility(View.GONE);
        } else {
            noFavoritesTextView.setVisibility(View.GONE);
            favoritesRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onRemoveFromFavoritesClick(Book book) {
        removeBookFromFavorites(book);
    }

    @Override
    public void onBookItemClick(Book book) {
        Intent intent = new Intent(this, BookReaderActivity.class);
        intent.putExtra(BookReaderActivity.EXTRA_BOOK_URL, book.getFilePath());
        intent.putExtra(BookReaderActivity.EXTRA_BOOK_TITLE, book.getTitle());
        startActivity(intent);
    }

    /**
     * Xóa sách khỏi danh sách yêu thích trên Firebase.
     */
    private void removeBookFromFavorites(Book book) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();
        String bookIdToRemove = book.getBookId();

        mFavoritesRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> currentBookIds = new ArrayList<>();
                for (DataSnapshot idSnapshot : snapshot.getChildren()) {
                    currentBookIds.add(idSnapshot.getValue(String.class));
                }

                if (currentBookIds.contains(bookIdToRemove)) {
                    currentBookIds.remove(bookIdToRemove);

                    mFavoritesRef.child(userId).setValue(currentBookIds)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(FavoritesActivity.this, "Đã xóa '" + book.getTitle() + "' khỏi danh sách yêu thích.", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(FavoritesActivity.this, "Lỗi khi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Lỗi khi xóa sách khỏi yêu thích", error.toException());
            }
        });
    }
}
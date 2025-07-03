package vn.edu.tlu.mybookstorage.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import vn.edu.tlu.mybookstorage.adapters.BooksInCollectionAdapter;
import vn.edu.tlu.mybookstorage.models.Book;

public class CollectionDetailActivity extends AppCompatActivity implements BooksInCollectionAdapter.OnBookRemoveListener {

    private static final String TAG = "CollectionDetailAct";
    public static final String EXTRA_COLLECTION_ID = "collection_id";
    public static final String EXTRA_COLLECTION_NAME = "collection_name";

    private RecyclerView booksInCollectionRecyclerView;
    private TextView noBooksInCollectionTextView;
    private BooksInCollectionAdapter booksInCollectionAdapter;
    private List<Book> bookList;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String currentUserId;
    private String collectionId;
    private String collectionName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collection_detail);

        booksInCollectionRecyclerView = findViewById(R.id.booksInCollectionRecyclerView);
        noBooksInCollectionTextView = findViewById(R.id.noBooksInCollectionTextView);
        Toolbar toolbar = findViewById(R.id.collectionDetailToolbar);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        } else {
            Toast.makeText(this, "Bạn cần đăng nhập để xem chi tiết bộ sưu tập.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Intent intent = getIntent();
        if (intent != null) {
            collectionId = intent.getStringExtra(EXTRA_COLLECTION_ID);
            collectionName = intent.getStringExtra(EXTRA_COLLECTION_NAME);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(collectionName);
            }
        }

        if (collectionId == null) {
            Toast.makeText(this, "Không tìm thấy ID bộ sưu tập.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bookList = new ArrayList<>();

        booksInCollectionAdapter = new BooksInCollectionAdapter(bookList, this);
        booksInCollectionRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        booksInCollectionRecyclerView.setAdapter(booksInCollectionAdapter);

        loadBooksInCollection();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void loadBooksInCollection() {
        mDatabase.child("collections").child(currentUserId).child(collectionId).child("bookIds")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<String> bookIdsInCollection = new ArrayList<>();
                        for (DataSnapshot idSnapshot : snapshot.getChildren()) {
                            String bookId = idSnapshot.getValue(String.class);
                            if (bookId != null) {
                                bookIdsInCollection.add(bookId);
                            }
                        }

                        if (bookIdsInCollection.isEmpty()) {
                            bookList.clear();
                            booksInCollectionAdapter.updateBookList(bookList);
                            updateUI();
                            return;
                        }

                        loadBookDetails(bookIdsInCollection);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Lỗi khi đọc bookIds trong bộ sưu tập: " + error.getMessage());
                        Toast.makeText(CollectionDetailActivity.this, "Lỗi khi tải sách trong bộ sưu tập.", Toast.LENGTH_SHORT).show();
                        updateUI();
                    }
                });
    }

    private void loadBookDetails(List<String> bookIds) {
        bookList.clear();
        final int[] booksLoadedCount = {0};
        int totalBooksToLoad = bookIds.size();

        for (String bookId : bookIds) {
            mDatabase.child("books").child(currentUserId).child(bookId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Book book = snapshot.getValue(Book.class);
                            if (book != null) {
                                bookList.add(book);
                            }
                            booksLoadedCount[0]++;

                            if (booksLoadedCount[0] == totalBooksToLoad) {
                                booksInCollectionAdapter.updateBookList(bookList);
                                updateUI();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "Lỗi khi tải chi tiết sách: " + error.getMessage());
                            booksLoadedCount[0]++;

                            if (booksLoadedCount[0] == totalBooksToLoad) {
                                booksInCollectionAdapter.updateBookList(bookList);
                                updateUI();
                            }
                        }
                    });
        }
    }


    private void updateUI() {
        if (bookList.isEmpty()) {
            noBooksInCollectionTextView.setVisibility(View.VISIBLE);
            booksInCollectionRecyclerView.setVisibility(View.GONE);
        } else {
            noBooksInCollectionTextView.setVisibility(View.GONE);
            booksInCollectionRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onRemoveBookClick(Book book) {

        if (currentUserId == null || collectionId == null || book.getBookId() == null) {
            Toast.makeText(this, "Lỗi: Không thể xóa sách. Thiếu thông tin.", Toast.LENGTH_SHORT).show();
            return;
        }

        mDatabase.child("collections").child(currentUserId).child(collectionId).child("bookIds")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<String> currentBookIds = new ArrayList<>();
                        for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                            String bookId = childSnapshot.getValue(String.class);
                            if (bookId != null) {
                                currentBookIds.add(bookId);
                            }
                        }


                        if (currentBookIds.remove(book.getBookId())) {
                            mDatabase.child("collections").child(currentUserId).child(collectionId).child("bookIds")
                                    .setValue(currentBookIds)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(CollectionDetailActivity.this, "'" + book.getTitle() + "' đã được xóa khỏi bộ sưu tập.", Toast.LENGTH_SHORT).show();

                                        bookList.remove(book);
                                        booksInCollectionAdapter.updateBookList(bookList);
                                        updateUI();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(CollectionDetailActivity.this, "Lỗi khi xóa sách khỏi bộ sưu tập: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        Log.e(TAG, "Lỗi khi xóa sách khỏi bộ sưu tập", e);
                                    });
                        } else {
                            Toast.makeText(CollectionDetailActivity.this, "Sách không tồn tại trong bộ sưu tập này.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(CollectionDetailActivity.this, "Lỗi khi kiểm tra bộ sưu tập để xóa: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Lỗi Firebase khi xóa sách khỏi bộ sưu tập", error.toException());
                    }
                });
    }

    @Override
    public void onBookItemClick(Book book) {

        Intent intent = new Intent(this, BookReaderActivity.class);
        intent.putExtra(BookReaderActivity.EXTRA_BOOK_URL, book.getFilePath());
        intent.putExtra(BookReaderActivity.EXTRA_BOOK_TITLE, book.getTitle());
        startActivity(intent);
    }
}
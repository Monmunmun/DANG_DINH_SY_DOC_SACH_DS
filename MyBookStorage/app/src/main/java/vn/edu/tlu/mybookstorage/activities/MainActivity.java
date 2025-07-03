package vn.edu.tlu.mybookstorage.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import android.view.Menu;
import androidx.appcompat.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import vn.edu.tlu.mybookstorage.R;
import vn.edu.tlu.mybookstorage.adapters.BookAdapter;
import vn.edu.tlu.mybookstorage.models.Book;
import vn.edu.tlu.mybookstorage.models.Collection;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener,
        BookAdapter.OnBookActionListener {

    private static final String TAG = "MainActivity";
    private static final int PICK_PDF_REQUEST = 1;

    // Các thành phần UI
    private RecyclerView booksRecyclerView;
    private TextView noBooksTextView;
    private FloatingActionButton fabAddBook;
    private BookAdapter bookAdapter;
    private List<Book> bookList;
    private DrawerLayout drawer;

    // Các biến Firebase
    private FirebaseAuth mAuth; //Firebase Authentication
    private GoogleSignInClient mGoogleSignInClient; //Google Sign-In Client
    private StorageReference mStorageRef; //Firebase Storage
    private DatabaseReference mDatabaseRef; //Firebase Realtime Database
    private DatabaseReference mCollectionsRef;
    private DatabaseReference mFavoritesRef;

    private List<Collection> userCollections;
    private List<Book> fullBookList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        mStorageRef = FirebaseStorage.getInstance().getReference("books");
        mDatabaseRef = FirebaseDatabase.getInstance().getReference("books");
        mCollectionsRef = FirebaseDatabase.getInstance().getReference("collections");
        mFavoritesRef = FirebaseDatabase.getInstance().getReference("favorites");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        booksRecyclerView = findViewById(R.id.booksRecyclerView);
        noBooksTextView = findViewById(R.id.noBooksTextView);
        fabAddBook = findViewById(R.id.fabAddBook);

        fullBookList = new ArrayList<>();
        bookList = new ArrayList<>();
        userCollections = new ArrayList<>();

        booksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        bookAdapter = new BookAdapter(bookList, this);
        booksRecyclerView.setAdapter(bookAdapter);

        loadBooksFromDatabase();

        loadUserCollections();

        fabAddBook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openFilePicker();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setQueryHint("Tìm kiếm sách...");

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                performSearch(newText);
                return true;
            }
        });

        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                bookList.clear();
                bookList.addAll(fullBookList);
                bookAdapter.notifyDataSetChanged();
                updateNoBooksMessage();
                return true;
            }
        });

        return true;
    }

    private void performSearch(String query) {
        bookList.clear();

        if (query.isEmpty()) {
            bookList.addAll(fullBookList);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (Book book : fullBookList) {
                if (book.getTitle().toLowerCase().contains(lowerCaseQuery)) {
                    bookList.add(book);
                }
            }
        }
        bookAdapter.notifyDataSetChanged();
        updateNoBooksMessage();
    }

    /**
     * Tải danh sách sách của người dùng hiện tại từ Firebase Realtime Database.
     */
    private void loadBooksFromDatabase() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "Người dùng chưa đăng nhập, không thể tải sách.");
            updateNoBooksMessage();
            return;
        }

        String userId = currentUser.getUid();
        mDatabaseRef.child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                fullBookList.clear();
                bookList.clear();
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    Book book = postSnapshot.getValue(Book.class);
                    if (book != null) {
                        fullBookList.add(book);
                    }
                }
                bookList.addAll(fullBookList);
                bookAdapter.notifyDataSetChanged();
                updateNoBooksMessage();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Tải sách thất bại từ Realtime Database: " + error.getMessage());
                Toast.makeText(MainActivity.this, "Lỗi khi tải sách: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                updateNoBooksMessage();
            }
        });
    }

    /**
     * Tải danh sách bộ sưu tập của người dùng hiện tại từ Firebase Realtime Database.
     */
    private void loadUserCollections() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "Người dùng chưa đăng nhập, không thể tải bộ sưu tập.");
            return;
        }

        String userId = currentUser.getUid();
        mCollectionsRef.child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userCollections.clear();
                for (DataSnapshot collectionSnapshot : snapshot.getChildren()) {
                    Collection collection = collectionSnapshot.getValue(Collection.class);
                    if (collection != null) {
                        collection.setId(collectionSnapshot.getKey());
                        userCollections.add(collection);
                    }
                }
                Log.d(TAG, "Đã tải " + userCollections.size() + " bộ sưu tập của người dùng.");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Lỗi khi tải bộ sưu tập của người dùng: " + error.getMessage());
                Toast.makeText(MainActivity.this, "Lỗi khi tải bộ sưu tập.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Mở trình chọn file của hệ thống để người dùng chọn file PDF.
     */
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "Chọn file PDF để tải lên"), PICK_PDF_REQUEST);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "Vui lòng cài đặt một trình quản lý file.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_PDF_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri pdfUri = data.getData();
            promptForBookTitle(pdfUri);
        }
    }

    /**
     * Hiển thị hộp thoại để người dùng nhập tên sách.
     */
    private void promptForBookTitle(final Uri pdfUri) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Nhập tên sách");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Ví dụ: Sách của mình");
        builder.setView(input);

        builder.setPositiveButton("Tải lên", (dialog, which) -> {
            String bookTitle = input.getText().toString().trim();
            if (bookTitle.isEmpty()) {
                Toast.makeText(MainActivity.this, "Tên sách không được để trống!", Toast.LENGTH_SHORT).show();
            } else {
                uploadPdfToStorage(pdfUri, bookTitle);
            }
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    /**
     * Trích xuất ảnh bìa từ trang đầu tiên của file PDF và tải lên Storage.
     */
    private void generateAndUploadCover(Uri pdfUri, String bookId, OnSuccessListener<Uri> listener) {
        try {
            ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(pdfUri, "r");
            if (pfd == null) {
                Log.e(TAG, "Không thể mở file PDF");
                listener.onSuccess(Uri.parse(""));
                return;
            }
            PdfRenderer renderer = new PdfRenderer(pfd);
            if (renderer.getPageCount() <= 0) {
                renderer.close();
                pfd.close();
                Log.e(TAG, "File PDF không có trang nào.");
                listener.onSuccess(Uri.parse(""));
                return;
            }
            PdfRenderer.Page page = renderer.openPage(0);

            Bitmap bitmap = Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

            page.close();
            renderer.close();
            pfd.close();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] data = baos.toByteArray();

            String userId = mAuth.getCurrentUser().getUid();
            StorageReference coverRef = mStorageRef.child(userId).child("covers").child(bookId + ".jpg");

            coverRef.putBytes(data)
                    .addOnSuccessListener(taskSnapshot -> {
                        coverRef.getDownloadUrl().addOnSuccessListener(listener)
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Lỗi lấy URL ảnh bìa", e);
                                    listener.onSuccess(Uri.parse(""));
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Lỗi tải ảnh bìa lên Storage", e);
                        listener.onSuccess(Uri.parse(""));
                    });

        } catch (IOException e) {
            Log.e(TAG, "Lỗi khi render PDF", e);
            listener.onSuccess(Uri.parse(""));
        }
    }

    /**
     * Tải file PDF lên Firebase Storage và lưu thông tin vào Realtime Database.
     */
    private void uploadPdfToStorage(Uri pdfUri, String bookTitle) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Bạn cần đăng nhập để tải sách lên.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(MainActivity.this, GoogleSignInActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        String userId = currentUser.getUid();
        String bookId = UUID.randomUUID().toString();
        String fileName = bookId + ".pdf";

        StorageReference fileRef = mStorageRef.child(userId).child(fileName);

        Toast.makeText(this, "Đang tải lên " + bookTitle + "...", Toast.LENGTH_LONG).show();

        generateAndUploadCover(pdfUri, bookId, coverUri -> {
            String coverImageUrl = (coverUri != null) ? coverUri.toString() : "https://placehold.co/150x200/cccccc/333333?text=PDF";

            fileRef.putFile(pdfUri)
                    .addOnSuccessListener(taskSnapshot -> {

                        fileRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                            String fileUrl = downloadUri.toString();
                            saveBookMetadataToDatabase(bookId, userId, bookTitle, fileUrl, coverImageUrl);
                            Toast.makeText(MainActivity.this, "Tải sách thành công!", Toast.LENGTH_SHORT).show();
                        }).addOnFailureListener(e -> {
                            Toast.makeText(MainActivity.this, "Lỗi lấy URL tải xuống: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Lỗi lấy URL tải xuống", e);
                        });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(MainActivity.this, "Tải sách thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Lỗi tải sách lên Storage", e);
                    });
        });
    }


    /**
     * Lưu thông tin sách vào Firebase Realtime Database.
     */
    private void saveBookMetadataToDatabase(String bookId, String userId, String title, String filePath, String coverImageUrl) {
        Map<String, Object> bookData = new HashMap<>();
        bookData.put("bookId", bookId);
        bookData.put("uid", userId);
        bookData.put("title", title);
        bookData.put("filePath", filePath);
        bookData.put("uploadDate", System.currentTimeMillis());
        bookData.put("coverImage", coverImageUrl);

        mDatabaseRef.child(userId).child(bookId).setValue(bookData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Thông tin sách đã được lưu vào Realtime Database!"))
                .addOnFailureListener(e -> Log.w(TAG, "Lỗi khi lưu thông tin sách vào Realtime Database", e));
    }

    /**
     * Phương thức khi một mục trong Navigation Drawer được chọn.
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_home) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }  else if (id == R.id.nav_favorites) {
            Intent intent = new Intent(this, FavoritesActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_collection) {
            Intent intent = new Intent(this, CollectionListActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_time) {
            Intent intent = new Intent(this, TimerActivity.class);
            startActivity(intent);
        }else if (id == R.id.nav_history) {
            Intent intent = new Intent(this, HistoryActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.nav_logout) {
            signOut();
        }
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onDeleteBookClick(Book book) {
        showDeleteConfirmationDialog(book);
    }

    /**
     * Hộp thoại xác nhận xóa sách.
     */
    private void showDeleteConfirmationDialog(final Book book) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa cuốn sách '" + book.getTitle() + "' không? Hành động này không thể hoàn tác.")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    deleteBookFromFirebase(book);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    /**
     * Xóa sách khỏi Firebase Storage và Realtime Database.
     */
    private void deleteBookFromFirebase(final Book book) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Bạn cần đăng nhập để xóa sách.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        String bookId = book.getBookId();
        String fileUrl = book.getFilePath();

        Toast.makeText(this, "Đang xóa sách...", Toast.LENGTH_SHORT).show();

        StorageReference fileRef = FirebaseStorage.getInstance().getReferenceFromUrl(fileUrl);

        fileRef.delete().addOnSuccessListener(aVoid -> {
            Log.d(TAG, "File đã được xóa khỏi Storage.");
            mDatabaseRef.child(userId).child(bookId).removeValue()
                    .addOnSuccessListener(aVoid1 -> {
                        Log.d(TAG, "Thông tin sách đã được xóa khỏi Database.");
                        Toast.makeText(MainActivity.this, "Đã xóa sách thành công!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Lỗi khi xóa thông tin sách khỏi Database", e);
                        Toast.makeText(MainActivity.this, "Lỗi khi xóa thông tin sách: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Lỗi khi xóa file khỏi Storage", e);
            Toast.makeText(MainActivity.this, "Lỗi khi xóa file: " + e.getMessage(), Toast.LENGTH_LONG).show();
            if (e.getMessage().contains("Object does not exist")) {
                Log.w(TAG, "File không tồn tại trong Storage, tiếp tục xóa dữ liệu khỏi Database.");
                mDatabaseRef.child(userId).child(bookId).removeValue()
                        .addOnSuccessListener(aVoid1 -> {
                            Toast.makeText(MainActivity.this, "Đã xóa dữ liệu thành công (file không tồn tại).", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e2 -> {
                            Log.e(TAG, "Lỗi khi xóa thông tin sách khỏi Database", e2);
                            Toast.makeText(MainActivity.this, "Lỗi khi xóa thông tin sách: " + e2.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
        });
    }


    /**
     * Phương thức xử lý đăng xuất khỏi Firebase và Google.
     */
    private void signOut() {
        mAuth.signOut();
        mGoogleSignInClient.signOut()
                .addOnCompleteListener(this, task -> {
                    Toast.makeText(MainActivity.this, "Đăng xuất thành công.", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(MainActivity.this, GoogleSignInActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Phương thức này sẽ cập nhật trạng thái hiển thị của thông báo "Chưa có sách"
     */
    private void updateNoBooksMessage() {
        if (bookList.isEmpty()) {
            noBooksTextView.setVisibility(View.VISIBLE);
            booksRecyclerView.setVisibility(View.GONE);
        } else {
            noBooksTextView.setVisibility(View.GONE);
            booksRecyclerView.setVisibility(View.VISIBLE);
        }
    }


    @Override
    public void onBookClick(Book book) {
        Intent intent = new Intent(this, BookReaderActivity.class);
        intent.putExtra(BookReaderActivity.EXTRA_BOOK_URL, book.getFilePath());
        intent.putExtra(BookReaderActivity.EXTRA_BOOK_TITLE, book.getTitle());
        startActivity(intent);
    }

    @Override
    public void onAddToCollectionClick(Book book) {
        if (userCollections.isEmpty()) {
            Toast.makeText(this, "Bạn chưa có bộ sưu tập nào. Vui lòng tạo bộ sưu tập trước.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, CollectionListActivity.class);
            startActivity(intent);
        } else {
            showAddToCollectionDialog(book);
        }
    }

    @Override
    public void onEditBookClick(Book book) {

        showEditBookTitleDialog(book);
    }

    private void showEditBookTitleDialog(final Book book) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Sửa tên sách");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(book.getTitle());
        builder.setView(input);

        builder.setPositiveButton("Lưu", (dialog, which) -> {
            String newTitle = input.getText().toString().trim();
            if (newTitle.isEmpty()) {
                Toast.makeText(MainActivity.this, "Tên sách không được để trống!", Toast.LENGTH_SHORT).show();
            } else if (!newTitle.equals(book.getTitle())) {
                updateBookTitle(book, newTitle);
            } else {
                Toast.makeText(MainActivity.this, "Tên sách không thay đổi.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    @Override
    public void onAddToFavoritesClick(Book book) {
        addBookToFavorites(book);
    }

    /**
     * Thêm sách vào danh sách yêu thích trên Firebase.
     */
    private void addBookToFavorites(Book book) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Bạn cần đăng nhập để thêm sách vào danh sách yêu thích.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        String bookId = book.getBookId();
        DatabaseReference userFavoritesRef = mFavoritesRef.child(userId);
        userFavoritesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> favoriteBookIds = new ArrayList<>();
                for (DataSnapshot idSnapshot : snapshot.getChildren()) {
                    favoriteBookIds.add(idSnapshot.getValue(String.class));
                }
                if (favoriteBookIds.contains(bookId)) {
                    Toast.makeText(MainActivity.this, "'" + book.getTitle() + "' đã có trong danh sách yêu thích.", Toast.LENGTH_SHORT).show();
                } else {
                    favoriteBookIds.add(bookId);
                    userFavoritesRef.setValue(favoriteBookIds)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(MainActivity.this, "Đã thêm '" + book.getTitle() + "' vào danh sách yêu thích.", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(MainActivity.this, "Lỗi khi thêm sách vào yêu thích: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "Lỗi khi thêm sách vào yêu thích", e);
                            });
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Lỗi kiểm tra danh sách yêu thích", error.toException());
                Toast.makeText(MainActivity.this, "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Cập nhật tên sách trên Firebase Realtime Database.
     */
    private void updateBookTitle(Book book, String newTitle) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Bạn cần đăng nhập để sửa sách.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        String bookId = book.getBookId();

        if (bookId == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy ID sách.", Toast.LENGTH_SHORT).show();
            return;
        }

        mDatabaseRef.child(userId).child(bookId).child("title").setValue(newTitle)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(MainActivity.this, "Đã cập nhật tên sách thành công!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Lỗi khi cập nhật tên sách: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Lỗi khi cập nhật tên sách", e);
                });
    }



    /**
     * Hiển thị dialog cho phép người dùng chọn bộ sưu tập để thêm sách vào.
     */
    private void showAddToCollectionDialog(final Book book) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Thêm '" + book.getTitle() + "' vào bộ sưu tập");

        String[] collectionNames = new String[userCollections.size()];
        for (int i = 0; i < userCollections.size(); i++) {
            collectionNames[i] = userCollections.get(i).getName();
        }

        builder.setItems(collectionNames, (dialog, which) -> {

            Collection selectedCollection = userCollections.get(which);
            addBookToCollection(book, selectedCollection);
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    /**
     * Thêm sách vào bộ sưu tập đã chọn trong Firebase Realtime Database.
     */
    private void addBookToCollection(Book book, Collection collection) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Lỗi: Bạn cần đăng nhập để thực hiện hành động này.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        String collectionId = collection.getId();
        String bookId = book.getBookId();

        if (collectionId == null || bookId == null) {
            Toast.makeText(this, "Lỗi: Thông tin sách hoặc bộ sưu tập không hợp lệ.", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference collectionBookRef = mCollectionsRef.child(userId)
                .child(collectionId).child("bookIds");

        collectionBookRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> currentBookIds = new ArrayList<>();
                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    currentBookIds.add(childSnapshot.getValue(String.class));
                }

                if (currentBookIds.contains(bookId)) {
                    Toast.makeText(MainActivity.this, "'" + book.getTitle() + "' đã có trong bộ sưu tập '" + collection.getName() + "'.", Toast.LENGTH_SHORT).show();
                } else {
                    currentBookIds.add(bookId);
                    mCollectionsRef.child(userId).child(collectionId).child("bookIds").setValue(currentBookIds)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(MainActivity.this, "'" + book.getTitle() + "' đã được thêm vào bộ sưu tập '" + collection.getName() + "'.", Toast.LENGTH_SHORT).show();
                                Log.d(TAG, "Đã thêm sách " + bookId + " vào bộ sưu tập " + collectionId);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(MainActivity.this, "Lỗi khi thêm sách vào bộ sưu tập: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "Lỗi khi thêm sách vào bộ sưu tập", e);
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Lỗi kiểm tra bộ sưu tập: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Lỗi Firebase khi kiểm tra sách trong bộ sưu tập", error.toException());
            }
        });
    }

}
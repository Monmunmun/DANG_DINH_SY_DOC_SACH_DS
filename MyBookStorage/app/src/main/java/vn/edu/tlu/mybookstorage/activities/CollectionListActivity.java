package vn.edu.tlu.mybookstorage.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vn.edu.tlu.mybookstorage.R;
import vn.edu.tlu.mybookstorage.adapters.CollectionAdapter;
import vn.edu.tlu.mybookstorage.models.Collection;

public class CollectionListActivity extends AppCompatActivity implements CollectionAdapter.OnCollectionInteractionListener {

    private static final String TAG = "CollectionListActivity";

    private RecyclerView collectionsRecyclerView;
    private CollectionAdapter collectionAdapter;
    private List<Collection> collectionList;
    private TextView noCollectionsTextView;
    private FloatingActionButton fabAddCollection;
    private Toolbar collectionToolbar;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collection_list);

        collectionsRecyclerView = findViewById(R.id.collectionsRecyclerView);
        noCollectionsTextView = findViewById(R.id.noCollectionsTextView);
        fabAddCollection = findViewById(R.id.fabAddCollection);
        collectionToolbar = findViewById(R.id.collectionToolbar);

        setSupportActionBar(collectionToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Bộ Sưu Tập Của Bạn");
        }

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        } else {
            Toast.makeText(this, "Bạn cần đăng nhập để xem bộ sưu tập.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        collectionList = new ArrayList<>();

        collectionAdapter = new CollectionAdapter(collectionList, this);
        collectionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        collectionsRecyclerView.setAdapter(collectionAdapter);

        fabAddCollection.setOnClickListener(v -> showAddCollectionDialog());

        loadCollectionsFromFirebase();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    /**
     * Tải dữ liệu bộ sưu tập từ Firebase Realtime Database cho người dùng hiện tại.
     */
    private void loadCollectionsFromFirebase() {
        mDatabase.child("collections").child(currentUserId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        collectionList.clear();
                        if (snapshot.exists()) {
                            for (DataSnapshot collectionSnapshot : snapshot.getChildren()) {
                                Collection collection = collectionSnapshot.getValue(Collection.class);
                                if (collection != null) {

                                    collection.setId(collectionSnapshot.getKey());
                                    collectionList.add(collection);
                                }
                            }
                            Log.d(TAG, "Đã tải " + collectionList.size() + " bộ sưu tập.");
                        } else {
                            Log.d(TAG, "Không có bộ sưu tập nào cho người dùng này.");
                        }
                        collectionAdapter.updateCollections(collectionList);
                        updateUI();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Lỗi khi đọc bộ sưu tập từ Firebase: " + error.getMessage());
                        Toast.makeText(CollectionListActivity.this, "Lỗi khi tải bộ sưu tập.", Toast.LENGTH_SHORT).show();
                        updateUI();
                    }
                });
    }

    /**
     * Hiển thị AlertDialog để người dùng nhập tên bộ sưu tập mới.
     */
    private void showAddCollectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Tạo Bộ Sưu Tập Mới");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Tên bộ sưu tập");
        builder.setView(input);

        builder.setPositiveButton("Tạo", (dialog, which) -> {
            String collectionName = input.getText().toString().trim();
            if (!collectionName.isEmpty()) {
                createCollection(collectionName);
            } else {
                Toast.makeText(CollectionListActivity.this, "Tên bộ sưu tập không được trống.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    /**
     * Tạo một bộ sưu tập mới trong Firebase Realtime Database.
     */
    private void createCollection(String name) {
        if (currentUserId == null) {
            Toast.makeText(this, "Lỗi: Người dùng chưa đăng nhập.", Toast.LENGTH_SHORT).show();
            return;
        }

        String collectionId = mDatabase.child("collections").child(currentUserId).push().getKey();
        if (collectionId == null) {
            Toast.makeText(this, "Lỗi: Không thể tạo ID bộ sưu tập.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> collectionData = new HashMap<>();
        collectionData.put("name", name);
        collectionData.put("userId", currentUserId);
        collectionData.put("bookIds", new ArrayList<String>());

        mDatabase.child("collections").child(currentUserId).child(collectionId).setValue(collectionData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(CollectionListActivity.this, "Đã tạo bộ sưu tập: " + name, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi tạo bộ sưu tập: " + e.getMessage());
                    Toast.makeText(CollectionListActivity.this, "Lỗi khi tạo bộ sưu tập.", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Hộp thoại cho phép người dùng chỉnh sửa tên của một bộ sưu tập hiện có.
     */
    private void showEditCollectionDialog(final Collection collection) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Sửa Tên Bộ Sưu Tập");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(collection.getName());
        builder.setView(input);

        builder.setPositiveButton("Lưu", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                editCollection(collection.getId(), newName);
            } else {
                Toast.makeText(CollectionListActivity.this, "Tên bộ sưu tập không được trống.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    /**
     * Chỉnh sửa tên của một bộ sưu tập trong Firebase.
     */
    private void editCollection(String collectionId, String newName) {
        if (currentUserId == null || collectionId == null) {
            Toast.makeText(this, "Lỗi: Không thể sửa tên bộ sưu tập. Thiếu thông tin.", Toast.LENGTH_SHORT).show();
            return;
        }

        mDatabase.child("collections").child(currentUserId).child(collectionId).child("name").setValue(newName)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(CollectionListActivity.this, "Đã cập nhật tên bộ sưu tập thành: " + newName, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi cập nhật tên bộ sưu tập: " + e.getMessage());
                    Toast.makeText(CollectionListActivity.this, "Lỗi khi cập nhật tên bộ sưu tập.", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Hộp thoại xác nhận trước khi xóa một bộ sưu tập.
     */
    private void showDeleteConfirmationDialog(final Collection collection) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa Bộ Sưu Tập")
                .setMessage("Bạn có chắc muốn xóa bộ sưu tập '" + collection.getName() + "' không? Tất cả sách trong bộ sưu tập này sẽ không bị xóa khỏi kho sách của bạn.")
                .setPositiveButton("Xóa", (dialog, which) -> deleteCollection(collection.getId(), collection.getName()))
                .setNegativeButton("Hủy", null)
                .show();
    }

    /**
     * Xóa một bộ sưu tập khỏi Firebase.
     */
    private void deleteCollection(String collectionId, String collectionName) {
        if (currentUserId == null || collectionId == null) {
            Toast.makeText(this, "Lỗi: Không thể xóa bộ sưu tập. Thiếu thông tin.", Toast.LENGTH_SHORT).show();
            return;
        }

        mDatabase.child("collections").child(currentUserId).child(collectionId).removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(CollectionListActivity.this, "Đã xóa bộ sưu tập: " + collectionName, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi xóa bộ sưu tập: " + e.getMessage());
                    Toast.makeText(CollectionListActivity.this, "Lỗi khi xóa bộ sưu tập.", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Cập nhật UI dựa trên việc có bộ sưu tập để hiển thị hay không.
     */
    private void updateUI() {
        if (collectionList.isEmpty()) {
            noCollectionsTextView.setVisibility(View.VISIBLE);
            collectionsRecyclerView.setVisibility(View.GONE);
        } else {
            noCollectionsTextView.setVisibility(View.GONE);
            collectionsRecyclerView.setVisibility(View.VISIBLE);
        }
    }


    @Override
    public void onCollectionClick(Collection collection) {
        Toast.makeText(this, "Mở bộ sưu tập: " + collection.getName(), Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, CollectionDetailActivity.class);
        intent.putExtra(CollectionDetailActivity.EXTRA_COLLECTION_ID, collection.getId());
        intent.putExtra(CollectionDetailActivity.EXTRA_COLLECTION_NAME, collection.getName());
        startActivity(intent);
    }

    @Override
    public boolean onCollectionLongClick(Collection collection) {
        CharSequence[] options = {"Sửa", "Xóa"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chọn hành động cho '" + collection.getName() + "'");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    showEditCollectionDialog(collection);
                } else if (which == 1) {
                    showDeleteConfirmationDialog(collection);
                }
            }
        });
        builder.show();
        return true;
    }
}

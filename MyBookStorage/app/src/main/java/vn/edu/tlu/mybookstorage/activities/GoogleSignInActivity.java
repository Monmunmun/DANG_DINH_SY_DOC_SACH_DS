package vn.edu.tlu.mybookstorage.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

import vn.edu.tlu.mybookstorage.R;

public class GoogleSignInActivity extends AppCompatActivity {

    private static final String TAG = "GoogleSignInActivity";
    private static final int RC_SIGN_IN = 9001;

    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    private SignInButton signInButton;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_login);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        signInButton = findViewById(R.id.signInButton);
        progressBar = findViewById(R.id.progressBar);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {

            updateUI(currentUser);
        } else {

            signInButton.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Bắt đầu quá trình đăng nhập Google.
     */
    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
        progressBar.setVisibility(View.VISIBLE);
        signInButton.setVisibility(View.GONE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "Google Sign-In thành công, ID: " + account.getId());
                firebaseAuthWithGoogle(account.getIdToken());

            } catch (ApiException e) {
                Log.w(TAG, "Đăng nhập Google thất bại, mã lỗi: " + e.getStatusCode(), e);
                Toast.makeText(this, "Đăng nhập Google thất bại. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                signInButton.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * Xác thực với Firebase bằng ID Token của Google.
     */
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            Log.d(TAG, "Firebase Auth thành công: " + user.getUid());
                            Toast.makeText(GoogleSignInActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();

                            saveUserToRealtimeDatabase(user);

                            updateUI(user);
                        } else {
                            Log.w(TAG, "Firebase Auth thất bại", task.getException());
                            Toast.makeText(GoogleSignInActivity.this, "Xác thực với Firebase thất bại.", Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                            signInButton.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    /**
     * Lưu thông tin người dùng vào Firebase Realtime Database.
     */
    private void saveUserToRealtimeDatabase(FirebaseUser user) {
        if (user != null) {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("uid", user.getUid());
            userMap.put("email", user.getEmail());
            userMap.put("name", user.getDisplayName());
            userMap.put("avatarUrl", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");
            userMap.put("lastLogin", System.currentTimeMillis());

            mDatabase.child("users").child(user.getUid()).setValue(userMap)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Thông tin người dùng đã được lưu vào Realtime Database!"))
                    .addOnFailureListener(e -> Log.w(TAG, "Lỗi khi lưu thông tin người dùng vào Realtime Database", e));
        }
    }

    /**
     * Cập nhật giao diện người dùng sau khi đăng nhập .
     */
    private void updateUI(@Nullable FirebaseUser user) {
        if (user != null) {
            Intent intent = new Intent(GoogleSignInActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        } else {
            progressBar.setVisibility(View.GONE);
            signInButton.setVisibility(View.VISIBLE);
        }
    }

    // Phương thức để đăng xuất
    public void signOut() {
        mAuth.signOut();
        mGoogleSignInClient.signOut()
                .addOnCompleteListener(this, task -> {
                    Toast.makeText(GoogleSignInActivity.this, "Đăng xuất thành công.", Toast.LENGTH_SHORT).show();
                    updateUI(null);
                });
    }
}

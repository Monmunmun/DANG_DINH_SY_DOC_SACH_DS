package vn.edu.tlu.mybookstorage.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.speech.tts.UtteranceProgressListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnErrorListener;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;

// Import các lớp của PdfBox-Android
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Locale;

import vn.edu.tlu.mybookstorage.R;
import vn.edu.tlu.mybookstorage.models.ReadingHistoryEntry;

public class BookReaderActivity extends AppCompatActivity {

    private static final String TAG = "BookReaderActivity";

    private PDFView pdfView;
    private ProgressBar progressBarReader;
    private Toolbar readerToolbar;
    private FloatingActionButton fabStopReading;
    private String currentBookId;

    public static final String EXTRA_BOOK_URL = "bookUrl";
    public static final String EXTRA_BOOK_TITLE = "bookTitle";
    public static final String EXTRA_BOOK_ID = "bookId";
    public static final String EXTRA_BOOK_COVER_IMAGE = "extra_book_cover_image";

    private static final String PREFS_NAME = "BookReaderPrefs";
    private static final String KEY_CURRENT_PAGE = "currentPage_";
    private static final String KEY_NIGHT_MODE = "nightMode";
    private static final String KEY_ZOOM_LEVEL = "zoomLevel";
    private static final String KEY_SCROLL_INTERVAL = "scrollInterval";
    private static final String KEY_SCROLL_DISTANCE = "scrollDistance";

    private static final String KEY_SPEECH_RATE = "speechRate";
    private static final String KEY_VOICE_GENDER = "voiceGender";
    private static final int VOICE_MALE = 1;
    private static final int VOICE_FEMALE = 2;
    private static final int VOICE_DEFAULT = 0;


    private TextToSpeech textToSpeech;
    private boolean isTtsInitialized = false;
    private String currentBookTitle = "Sách đang đọc";
    private String currentPdfFilePath;
    private int currentPage = 0;

    private SharedPreferences sharedPreferences;
    private boolean isNightMode = false;
    private float currentZoomLevel = 1.0f;

    private Handler autoScrollHandler;
    private Runnable autoScrollRunnable;
    private boolean isAutoScrolling = false;
    private int scrollInterval = 2000;
    private int scrollDistance = 50;

    private float currentSpeechRate = 1.0f;
    private int currentVoiceGender = VOICE_DEFAULT;

    private DatabaseReference mHistoryRef;
    private FirebaseAuth mAuth;
    private String mUserId;
    private String currentBookFilePath;
    private String currentBookCoverImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_reader);

        pdfView = findViewById(R.id.pdfView);
        progressBarReader = findViewById(R.id.progressBarReader);
        readerToolbar = findViewById(R.id.readerToolbar);
        fabStopReading = findViewById(R.id.fabStopReading);
        fabStopReading.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopSpeaking();
            }
        });

        setSupportActionBar(readerToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        String bookUrl = getIntent().getStringExtra(EXTRA_BOOK_URL);
        String bookTitle = getIntent().getStringExtra(EXTRA_BOOK_TITLE);
        String bookId = getIntent().getStringExtra(EXTRA_BOOK_ID);
        String coverImage = getIntent().getStringExtra(EXTRA_BOOK_COVER_IMAGE);

        currentBookFilePath = bookUrl;
        currentBookTitle = bookTitle;
        currentBookId = bookId;
        currentBookCoverImage = coverImage;

        Log.d(TAG, "onCreate: Nhận được bookUrl: " + (bookUrl != null ? bookUrl : "NULL"));
        Log.d(TAG, "onCreate: Nhận được bookTitle: " + (bookTitle != null ? bookTitle : "NULL"));
        Log.d(TAG, "onCreate: Nhận được bookId: " + (bookId != null ? bookId : "NULL"));

        if (bookTitle != null && !bookTitle.isEmpty()) {
            readerToolbar.setTitle(bookTitle);
            currentBookTitle = bookTitle;
        } else {
            readerToolbar.setTitle("Đọc sách");
        }

        if (bookId != null && !bookId.isEmpty()) {
            currentBookId = bookId;
        }

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        loadReadingSettings();

        PDFBoxResourceLoader.init(getApplicationContext());

        initTextToSpeech();

        autoScrollHandler = new Handler();
        autoScrollRunnable = new Runnable() {
            @Override
            public void run() {

                float currentPosition = pdfView.getPositionOffset();

                float scrollOffset = (float) scrollDistance / pdfView.getHeight() * 0.01f;

                float newPosition = currentPosition + scrollOffset;

                if (newPosition >= 1.0f) {
                    stopAutoScroll();
                } else {
                    pdfView.setPositionOffset(newPosition);
                    autoScrollHandler.postDelayed(this, scrollInterval);
                }
            }
        };

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            mUserId = currentUser.getUid();
            mHistoryRef = FirebaseDatabase.getInstance().getReference("users")
                    .child(mUserId)
                    .child("history");
            Log.d(TAG, "Đã khởi tạo Firebase History Ref cho user: " + mUserId);
        } else {
            Log.e(TAG, "Không có người dùng đang đăng nhập. Không thể lưu lịch sử.");
        }


        if (bookUrl != null && !bookUrl.isEmpty()) {
            String fileNameForCache;
            if (bookId != null && !bookId.isEmpty()) {
                fileNameForCache = bookId + ".pdf";
            } else {
                try {
                    fileNameForCache = URLEncoder.encode(bookUrl, "UTF-8").replaceAll("[^a-zA-Z0-9.\\-]", "_") + ".pdf";
                } catch (Exception e) {
                    Log.e(TAG, "Lỗi mã hóa URL cho tên file cache", e);
                    fileNameForCache = "cached_pdf_" + System.currentTimeMillis() + ".pdf";
                }
            }

            File cachedFile = new File(getFilesDir(), fileNameForCache);

            if (cachedFile.exists()) {
                Log.d(TAG, "Sách đã có trong bộ nhớ đệm: " + cachedFile.getName());
                Toast.makeText(this, "Đang mở sách ", Toast.LENGTH_SHORT).show();
                currentPdfFilePath = cachedFile.getAbsolutePath();
                loadPdfFromFile(cachedFile);
            } else {
                Log.d(TAG, "Sách chưa có trong bộ nhớ đệm. Bắt đầu tải xuống: " + bookUrl);
                Toast.makeText(this, "Đang tải sách từ máy chủ...", Toast.LENGTH_SHORT).show();

                new DownloadPdfTask(this, pdfView, progressBarReader, fileNameForCache) {
                    @Override
                    protected void onPostExecute(File file) {
                        super.onPostExecute(file);
                        if (file != null) {
                            currentPdfFilePath = file.getAbsolutePath();
                        }
                    }
                }.execute(bookUrl);
            }
        } else {
            Toast.makeText(this, "Không tìm thấy đường dẫn sách.", Toast.LENGTH_SHORT).show();
            progressBarReader.setVisibility(View.GONE);
            readerToolbar.setVisibility(View.VISIBLE);
        }
    }

    private void initTextToSpeech() {
        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    // Đặt ngôn ngữ cho TTS
                    int result = textToSpeech.setLanguage(new Locale("vi", "VN"));

                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e(TAG, "Ngôn ngữ không được hỗ trợ hoặc thiếu dữ liệu ngôn ngữ.");
                        Toast.makeText(BookReaderActivity.this, "Ngôn ngữ đọc không được hỗ trợ.", Toast.LENGTH_LONG).show();
                        isTtsInitialized = false;
                    } else {
                        isTtsInitialized = true;
                        Log.d(TAG, "TextToSpeech đã khởi tạo thành công.");
                        textToSpeech.setSpeechRate(currentSpeechRate);
                        textToSpeech.setPitch(1.0f);
                        updateVoice(currentVoiceGender);

                        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                            @Override
                            public void onStart(String utteranceId) {
                                Log.d(TAG, "Bắt đầu đọc: " + utteranceId);
                                runOnUiThread(() -> Toast.makeText(BookReaderActivity.this, "Bắt đầu nghe đọc...", Toast.LENGTH_SHORT).show());
                            }

                            @Override
                            public void onDone(String utteranceId) {
                                Log.d(TAG, "Hoàn thành đọc: " + utteranceId);
                                runOnUiThread(() -> {
                                    Toast.makeText(BookReaderActivity.this, "Đã hoàn thành nghe đọc.", Toast.LENGTH_SHORT).show();
                                    if (fabStopReading != null) {
                                        fabStopReading.hide();
                                    }
                                });
                            }

                            @Override
                            public void onError(String utteranceId) {
                                Log.e(TAG, "Lỗi khi đọc: " + utteranceId);
                                runOnUiThread(() -> {
                                    Toast.makeText(BookReaderActivity.this, "Lỗi khi nghe đọc.", Toast.LENGTH_SHORT).show();
                                    if (fabStopReading != null) {
                                        fabStopReading.hide();
                                    }
                                });
                            }
                        });
                    }
                } else {
                    Log.e(TAG, "Khởi tạo TextToSpeech thất bại. Status: " + status);
                    Toast.makeText(BookReaderActivity.this, "Không thể khởi tạo tính năng đọc sách.", Toast.LENGTH_LONG).show();
                    isTtsInitialized = false;
                }
            }
        });
    }

    /**
     * Bắt đầu đọc văn bản.
     */
    private void startSpeaking(String text) {
        if (isTtsInitialized) {
            if (textToSpeech.isSpeaking()) {
                textToSpeech.stop();
            }

            HashMap<String, String> params = new HashMap<>();
            params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "pdf_reader_utterance");

            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params);
            if (fabStopReading != null) {
                fabStopReading.show();
            }
        } else {
            Toast.makeText(this, "Tính năng nghe đọc chưa sẵn sàng. Vui lòng thử lại sau.", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "TextToSpeech chưa được khởi tạo.");
        }
    }

    /**
     * Dừng quá trình đọc hiện tại.
     */
    private void stopSpeaking() {
        if (textToSpeech != null && textToSpeech.isSpeaking()) {
            textToSpeech.stop();
            Toast.makeText(this, "Đã dừng nghe đọc.", Toast.LENGTH_SHORT).show();
            fabStopReading.hide();
        }
    }

    // Chức năng tự động cuộn
    /**
     * Bắt đầu tự động cuộn PDF.
     */
    private void startAutoScroll() {
        if (!isAutoScrolling) {
            isAutoScrolling = true;
            Toast.makeText(this, "Bắt đầu tự động cuộn...", Toast.LENGTH_SHORT).show();
            autoScrollHandler.post(autoScrollRunnable);
        }
    }

    /**
     * Dừng tự động cuộn PDF.
     */
    private void stopAutoScroll() {
        if (isAutoScrolling) {
            isAutoScrolling = false;
            Toast.makeText(this, "Đã dừng tự động cuộn.", Toast.LENGTH_SHORT).show();
            autoScrollHandler.removeCallbacks(autoScrollRunnable);
        }
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            Log.d(TAG, "TextToSpeech đã tắt.");
        }
        stopAutoScroll();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.reader_toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_read_aloud) {
            // Kiểm tra xem đã có đường dẫn file PDF cục bộ chưa
            if (currentPdfFilePath != null && !currentPdfFilePath.isEmpty()) {
                new ExtractTextFromPdfTask().execute(currentPdfFilePath);
            } else {
                Toast.makeText(this, "Không thể đọc. Sách chưa được tải hoặc lưu trữ.", Toast.LENGTH_SHORT).show();
            }
            return true;
        } else if (id == R.id.action_autoscroll) {
            showAutoScrollSettingsDialog();
            return true;
        } else if (id == R.id.action_font_settings) {
            showReadingSettingsDialog();
            return true;
        } else if (id == R.id.action_tts_settings) {
            showTtsSettingsDialog();
            return true;
        } else if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void loadPdfFromFile(File file) {
        progressBarReader.setVisibility(View.VISIBLE);

        pdfView.fromFile(file)
                .enableSwipe(true)
                .swipeHorizontal(false)
                .enableDoubletap(true)
                .defaultPage(currentPage)
                .enableAnnotationRendering(false)
                .scrollHandle(new DefaultScrollHandle(this))
                .enableAntialiasing(true)
                .spacing(0)
                .nightMode(isNightMode)
                .onLoad(new OnLoadCompleteListener() {
                    @Override
                    public void loadComplete(int nbPages) {
                        progressBarReader.setVisibility(View.GONE);
                        Log.d(TAG, "Sách đã tải hoàn tất. Số trang: " + nbPages);
                        pdfView.zoomTo(currentZoomLevel);
                    }
                })
                .onError(new OnErrorListener() {
                    @Override
                    public void onError(Throwable t) {
                        progressBarReader.setVisibility(View.GONE);
                        Toast.makeText(BookReaderActivity.this, "Lỗi khi hiển thị sách: " + t.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Lỗi khi hiển thị sách từ file cục bộ: ", t);
                        // Xóa file cache nếu nó bị lỗi
                        if (file.exists()) {
                            boolean deleted = file.delete();
                            if (deleted) Log.d(TAG, "Đã xóa file cache bị lỗi.");
                        }
                    }
                })
                .onPageChange(new com.github.barteksc.pdfviewer.listener.OnPageChangeListener() {
                    @Override
                    public void onPageChanged(int page, int pageCount) {
                        currentPage = page;
                        Log.d(TAG, "Page changed to: " + (page + 1));
                    }
                })

                .load();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
    private void showReadingSettingsDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_reading_settings, null);

        SeekBar zoomSeekBar = dialogView.findViewById(R.id.zoomSeekBar);
        Switch nightModeSwitch = dialogView.findViewById(R.id.nightModeSwitch);
        TextView zoomPercentageTextView = dialogView.findViewById(R.id.zoomPercentageTextView);

        int initialProgress = (int) (currentZoomLevel * 100);
        zoomSeekBar.setProgress(initialProgress);
        zoomPercentageTextView.setText(initialProgress + "%");

        nightModeSwitch.setChecked(isNightMode);

        zoomSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentZoomLevel = progress / 100.0f;
                pdfView.zoomTo(currentZoomLevel);
                zoomPercentageTextView.setText(progress + "%");
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                saveReadingSettings();
            }
        });

        nightModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isNightMode = isChecked;
            loadPdfFromFile(new File(currentPdfFilePath));
            saveReadingSettings();
        });

        new AlertDialog.Builder(this)
                .setTitle("Cài đặt đọc")
                .setView(dialogView)
                .setPositiveButton("Đóng", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * Hộp thoại cài đặt tự động cuộn.
     */
    private void showAutoScrollSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_autoscroll_settings, null);
        builder.setView(dialogView);

        EditText etScrollInterval = dialogView.findViewById(R.id.et_scroll_interval);
        EditText etScrollDistance = dialogView.findViewById(R.id.et_scroll_distance);
        Button btnStart = dialogView.findViewById(R.id.btn_start_scroll);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel_scroll);
        TextView tvCurrentStatus = dialogView.findViewById(R.id.tv_autoscroll_status);

        if (isAutoScrolling) {
            tvCurrentStatus.setText("Đang cuộn tự động. Tốc độ: " + scrollInterval + "ms, Khoảng cách: " + scrollDistance + "px");
            btnStart.setText("Cập nhật và Bắt đầu");
        } else {
            tvCurrentStatus.setText("Tự động cuộn đang Dừng.");
            btnStart.setText("Bắt đầu");
        }

        etScrollInterval.setText(String.valueOf(sharedPreferences.getInt(KEY_SCROLL_INTERVAL, 2000)));
        etScrollDistance.setText(String.valueOf(sharedPreferences.getInt(KEY_SCROLL_DISTANCE, 50)));

        AlertDialog dialog = builder.create();

        btnStart.setOnClickListener(v -> {
            String intervalStr = etScrollInterval.getText().toString();
            String distanceStr = etScrollDistance.getText().toString();

            if (intervalStr.isEmpty() || distanceStr.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin.", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                int newInterval = Integer.parseInt(intervalStr);
                int newDistance = Integer.parseInt(distanceStr);

                if (newInterval <= 0 || newDistance <= 0) {
                    Toast.makeText(this, "Giá trị phải lớn hơn 0.", Toast.LENGTH_SHORT).show();
                    return;
                }

                scrollInterval = newInterval;
                scrollDistance = newDistance;
                saveReadingSettings();

                stopAutoScroll();
                startAutoScroll();
                dialog.dismiss();
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Giá trị không hợp lệ.", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> {
            stopAutoScroll();
            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * Hộp thoại cài đặt giọng nói TTS
     */
    private void showTtsSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_tts_settings, null);
        builder.setView(dialogView);
        builder.setTitle("Cài đặt giọng đọc");

        SeekBar speedSeekBar = dialogView.findViewById(R.id.speedSeekBar);
        TextView speedValueTextView = dialogView.findViewById(R.id.tv_speed_value);
        RadioGroup voiceRadioGroup = dialogView.findViewById(R.id.voiceRadioGroup);
        RadioButton maleVoiceRadio = dialogView.findViewById(R.id.radio_voice_male);
        RadioButton femaleVoiceRadio = dialogView.findViewById(R.id.radio_voice_female);
        RadioButton defaultVoiceRadio = dialogView.findViewById(R.id.radio_voice_default);

        // Thiết lập SeekBar cho tốc độ đọc
        float[] speedValues = {0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f};
        int initialProgress = 3; // Mặc định là 1.0x
        for (int i = 0; i < speedValues.length; i++) {
            if (speedValues[i] == currentSpeechRate) {
                initialProgress = i;
                break;
            }
        }
        speedSeekBar.setProgress(initialProgress);
        speedValueTextView.setText(currentSpeechRate + "x");

        speedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float newSpeed = speedValues[progress];
                speedValueTextView.setText(newSpeed + "x");
                textToSpeech.setSpeechRate(newSpeed);
                currentSpeechRate = newSpeed;
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                saveReadingSettings();
            }
        });

        if (currentVoiceGender == VOICE_MALE) {
            maleVoiceRadio.setChecked(true);
        } else if (currentVoiceGender == VOICE_FEMALE) {
            femaleVoiceRadio.setChecked(true);
        } else {
            defaultVoiceRadio.setChecked(true);
        }

        voiceRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int newVoiceGender = VOICE_DEFAULT;
            if (checkedId == R.id.radio_voice_male) {
                newVoiceGender = VOICE_MALE;
            } else if (checkedId == R.id.radio_voice_female) {
                newVoiceGender = VOICE_FEMALE;
            }

            currentVoiceGender = newVoiceGender;
            updateVoice(currentVoiceGender);
            saveReadingSettings();
        });

        builder.setPositiveButton("Đóng", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    // Phương thức để thay đổi giọng
    private void updateVoice(int voiceGender) {
        if (!isTtsInitialized) {
            return;
        }
        Locale targetLocale = new Locale("vi", "VN");
        for (Voice voice : textToSpeech.getVoices()) {
            if (voice.getLocale().equals(targetLocale)) {
                if (voiceGender == VOICE_MALE && voice.getFeatures().contains(TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS) && voice.getFeatures().contains("male")) {
                    textToSpeech.setVoice(voice);
                    Log.d(TAG, "Đã đặt giọng Nam.");
                    return;
                } else if (voiceGender == VOICE_FEMALE && voice.getFeatures().contains(TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS) && voice.getFeatures().contains("female")) {
                    textToSpeech.setVoice(voice);
                    Log.d(TAG, "Đã đặt giọng Nữ.");
                    return;
                }
            }
        }
        textToSpeech.setLanguage(targetLocale);
        Log.d(TAG, "Không tìm thấy giọng Nam/Nữ phù hợp, đã quay lại giọng mặc định của ngôn ngữ.");
    }



    /**
     * Tải cài đặt đọc từ SharedPreferences.
     */
    private void loadReadingSettings() {
        if (currentBookId != null) {
            currentPage = sharedPreferences.getInt(KEY_CURRENT_PAGE + currentBookId, 0);
        }
        isNightMode = sharedPreferences.getBoolean(KEY_NIGHT_MODE, false);
        currentZoomLevel = sharedPreferences.getFloat(KEY_ZOOM_LEVEL, 1.0f);
        scrollInterval = sharedPreferences.getInt(KEY_SCROLL_INTERVAL, 2000);
        scrollDistance = sharedPreferences.getInt(KEY_SCROLL_DISTANCE, 50);

        currentSpeechRate = sharedPreferences.getFloat(KEY_SPEECH_RATE, 1.0f);
        currentVoiceGender = sharedPreferences.getInt(KEY_VOICE_GENDER, VOICE_DEFAULT);
    }

    /**
     * Lưu cài đặt đọc vào SharedPreferences.
     */
    private void saveReadingSettings() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (currentBookId != null) {
            editor.putInt(KEY_CURRENT_PAGE + currentBookId, currentPage);
        }
        editor.putBoolean(KEY_NIGHT_MODE, isNightMode);
        editor.putFloat(KEY_ZOOM_LEVEL, currentZoomLevel);
        editor.putInt(KEY_SCROLL_INTERVAL, scrollInterval);
        editor.putInt(KEY_SCROLL_DISTANCE, scrollDistance);
        editor.putFloat(KEY_SPEECH_RATE, currentSpeechRate);
        editor.putInt(KEY_VOICE_GENDER, currentVoiceGender);

        editor.apply();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopAutoScroll();
        saveReadingSettings();
        saveReadingHistory();
    }

    /**
     * AsyncTask để tải file PDF từ URL web và lưu vào bộ nhớ đệm của ứng dụng.
     */
    private static class DownloadPdfTask extends AsyncTask<String, Void, File> {

        private static final String INNER_TAG = "DownloadPdfTask";
        private Context context;
        private PDFView pdfView;
        private ProgressBar progressBar;
        private String fileNameForCache;

        public DownloadPdfTask(@NonNull Context context, @NonNull PDFView pdfView,
                               @NonNull ProgressBar progressBar, @NonNull String fileNameForCache) {
            this.context = context;
            this.pdfView = pdfView;
            this.progressBar = progressBar;
            this.fileNameForCache = fileNameForCache;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected File doInBackground(String... urls) {
            String pdfUrl = urls[0];
            File downloadedFile = null;
            InputStream inputStream = null;
            FileOutputStream outputStream = null;

            try {
                downloadedFile = new File(context.getFilesDir(), fileNameForCache);

                Log.d(INNER_TAG, "Bắt đầu tải file mới từ URL: " + pdfUrl);
                URL url = new URL(pdfUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                // Kiểm tra mã phản hồi HTTP
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    Log.e(INNER_TAG, "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage());
                    return null;
                }

                inputStream = connection.getInputStream();
                outputStream = new FileOutputStream(downloadedFile);

                byte[] buffer = new byte[4096];
                int bytesRead;
                long totalBytesDownloaded = 0;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytesDownloaded += bytesRead;
                }
                Log.d(INNER_TAG, "Tải file hoàn tất. Kích thước: " + totalBytesDownloaded + " bytes");
                return downloadedFile;

            } catch (IOException e) {
                Log.e(INNER_TAG, "Lỗi khi tải PDF từ URL: " + e.getMessage(), e);
                if (downloadedFile != null && downloadedFile.exists()) {
                    Log.w(INNER_TAG, "Xóa file cache không hoàn chỉnh/bị lỗi.");
                    downloadedFile.delete();
                }
                return null;
            } finally {
                try {
                    if (inputStream != null) inputStream.close();
                    if (outputStream != null) outputStream.close();
                } catch (IOException e) {
                    Log.e(INNER_TAG, "Lỗi khi đóng luồng: " + e.getMessage());
                }
            }
        }

        @Override
        protected void onPostExecute(File file) {
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }

            if (file != null) {
                Log.d(INNER_TAG, "Đang hiển thị PDF từ file: " + file.getAbsolutePath());
                pdfView.fromFile(file)
                        .enableSwipe(true)
                        .swipeHorizontal(false)
                        .enableDoubletap(true)
                        .defaultPage(0)
                        .enableAnnotationRendering(false)
                        .scrollHandle(new DefaultScrollHandle(context))
                        .enableAntialiasing(true)
                        .spacing(0)
                        .onLoad(new OnLoadCompleteListener() {
                            @Override
                            public void loadComplete(int nbPages) {
                                Toast.makeText(context, "Sách đã sẵn sàng.", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .onError(new OnErrorListener() {
                            @Override
                            public void onError(Throwable t) {
                                Toast.makeText(context, "Lỗi khi hiển thị sách đã tải: " + t.getMessage(), Toast.LENGTH_LONG).show();
                                Log.e(INNER_TAG, "Lỗi khi hiển thị sách đã tải: ", t);
                                if (file.exists()) {
                                    file.delete();
                                }
                            }
                        })
                        .load();
            } else {
                Toast.makeText(context, "Không thể tải sách từ URL. Vui lòng kiểm tra kết nối mạng.", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * AsyncTask để trích xuất văn bản từ file PDF cục bộ bằng PdfBox-Android.
     */
    private class ExtractTextFromPdfTask extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBarReader.setVisibility(View.VISIBLE);
            Toast.makeText(BookReaderActivity.this, "Đang trích xuất văn bản để đọc...", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected String doInBackground(String... filePaths) {
            String filePath = filePaths[0];
            File pdfFile = new File(filePath);
            String extractedText = null;
            PDDocument document = null;

            if (!pdfFile.exists()) {
                Log.e(TAG, "File PDF không tồn tại: " + filePath);
                return null;
            }

            try {
                document = PDDocument.load(pdfFile);
                PDFTextStripper pdfStripper = new PDFTextStripper();
                extractedText = pdfStripper.getText(document);
                Log.d(TAG, "Đã trích xuất văn bản từ PDF.");
            } catch (IOException e) {
                Log.e(TAG, "Lỗi khi trích xuất văn bản từ PDF: " + e.getMessage(), e);
            } finally {
                if (document != null) {
                    try {
                        document.close();
                        Log.d(TAG, "Đã đóng tài liệu PDF sau khi trích xuất.");
                    } catch (IOException e) {
                        Log.e(TAG, "Lỗi khi đóng tài liệu PDF: " + e.getMessage());
                    }
                }
            }
            return extractedText;
        }

        @Override
        protected void onPostExecute(String extractedText) {
            progressBarReader.setVisibility(View.GONE);
            if (extractedText != null && !extractedText.trim().isEmpty()) {
                startSpeaking(extractedText);
            } else {
                Toast.makeText(BookReaderActivity.this, "Không tìm thấy văn bản trong sách này để đọc.", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Lưu lịch sử đọc vào Firebase Realtime Database.
     */
    private void saveReadingHistory() {
        if (mUserId != null && currentBookId != null && !currentBookId.isEmpty()) {
            ReadingHistoryEntry historyEntry = new ReadingHistoryEntry(
                    currentBookId,
                    currentBookTitle,
                    currentBookFilePath,
                    currentBookCoverImage,
                    currentPage,
                    System.currentTimeMillis()
            );

            mHistoryRef.child(currentBookId)
                    .setValue(historyEntry)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Lưu lịch sử đọc thành công cho sách: " + currentBookId);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Lỗi khi lưu lịch sử đọc: " + e.getMessage());
                    });
        } else {
            Log.w(TAG, "Không thể lưu lịch sử đọc: User ID hoặc Book ID không có sẵn.");
        }
    }
}
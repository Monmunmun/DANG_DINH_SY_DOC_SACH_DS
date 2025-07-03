package vn.edu.tlu.mybookstorage.activities;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.Locale;

import vn.edu.tlu.mybookstorage.R;

public class TimerActivity extends AppCompatActivity implements TimerService.OnTimerUpdateListener {

    private TextView selectedTimeTextView;
    private SeekBar timerSeekBar;
    private Button preset30minButton;
    private Button preset60minButton;
    private Button preset90minButton;
    private Button startButton;
    private Button cancelButton;

    private TimerService timerService;
    private boolean isBound = false;

    private long currentSeekBarTimeMillis = 30 * 60 * 1000;

    private static final String CHANNEL_ID_ALERT = "timer_alert_channel";
    private static final int NOTIFICATION_ID_ALERT = 1;

    private static final int PERMISSION_REQUEST_CODE_NOTIFICATIONS = 101;
    private static final int PERMISSION_REQUEST_CODE_FOREGROUND_SERVICE_DATA_SYNC = 102;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            TimerService.LocalBinder binder = (TimerService.LocalBinder) service;
            timerService = binder.getService();
            isBound = true;
            timerService.setTimerUpdateListener(TimerActivity.this);

            if (timerService.isTimerRunning()) {
                currentSeekBarTimeMillis = timerService.getTimeLeftInMillis();
                updateCountDownText(currentSeekBarTimeMillis);
                timerSeekBar.setProgress((int) (currentSeekBarTimeMillis / (60 * 1000)));
                updateButtonsState(true);
            } else {
                if (timerService.getInitialSetTimeMillis() > 0) {
                    currentSeekBarTimeMillis = timerService.getInitialSetTimeMillis();
                } else {
                    currentSeekBarTimeMillis = 30 * 60 * 1000;
                }
                updateCountDownText(currentSeekBarTimeMillis);
                timerSeekBar.setProgress((int) (currentSeekBarTimeMillis / (60 * 1000)));
                updateButtonsState(false);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            if (timerService != null) {
                timerService.setTimerUpdateListener(null);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);

        Toolbar timerToolbar = findViewById(R.id.timerToolbar);
        setSupportActionBar(timerToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Hẹn giờ đọc sách");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        selectedTimeTextView = findViewById(R.id.selectedTimeTextView);
        timerSeekBar = findViewById(R.id.timerSeekBar);
        preset30minButton = findViewById(R.id.preset30minButton);
        preset60minButton = findViewById(R.id.preset60minButton);
        preset90minButton = findViewById(R.id.preset90minButton);
        startButton = findViewById(R.id.startButton);
        cancelButton = findViewById(R.id.cancelButton);

        timerSeekBar.setMax(180);
        timerSeekBar.setProgress((int) (currentSeekBarTimeMillis / (60 * 1000)));
        updateCountDownText(currentSeekBarTimeMillis);

        timerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && (timerService == null || !timerService.isTimerRunning())) {
                    if (progress < 1) {
                        progress = 1;
                        seekBar.setProgress(1);
                    }
                    currentSeekBarTimeMillis = (long) progress * 60 * 1000;
                    updateCountDownText(currentSeekBarTimeMillis);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        preset30minButton.setOnClickListener(v -> setPresetTime(30));
        preset60minButton.setOnClickListener(v -> setPresetTime(60));
        preset90minButton.setOnClickListener(v -> setPresetTime(90));

        startButton.setOnClickListener(v -> {
            if (checkAndRequestPermissions()) {
                if (isBound && timerService != null) {
                    timerService.startTimer(currentSeekBarTimeMillis);
                    updateButtonsState(true);
                } else {
                    bindAndStartService();
                }
            } else {
                Toast.makeText(TimerActivity.this, "Cần cấp quyền để sử dụng hẹn giờ!", Toast.LENGTH_SHORT).show();
            }
        });
        cancelButton.setOnClickListener(v -> {
            if (isBound && timerService != null) {
                timerService.cancelTimer();
                currentSeekBarTimeMillis = 30 * 60 * 1000;
                timerSeekBar.setProgress((int) (currentSeekBarTimeMillis / (60 * 1000)));
                updateCountDownText(currentSeekBarTimeMillis);
                updateButtonsState(false);
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (checkAndRequestPermissions()) {
            bindAndStartService();
        }
    }

    /**
     * Bind và Start Service.
     */
    private void bindAndStartService() {
        Intent intent = new Intent(this, TimerService.class);
        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * Kiểm tra và yêu cầu các quyền cần thiết.
     */
    private boolean checkAndRequestPermissions() {
        boolean allPermissionsGranted = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_CODE_NOTIFICATIONS);
                allPermissionsGranted = false;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_DATA_SYNC) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.FOREGROUND_SERVICE_DATA_SYNC}, PERMISSION_REQUEST_CODE_FOREGROUND_SERVICE_DATA_SYNC);
                allPermissionsGranted = false;
            }
        }

        return allPermissionsGranted;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE_NOTIFICATIONS || requestCode == PERMISSION_REQUEST_CODE_FOREGROUND_SERVICE_DATA_SYNC) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                bindAndStartService();
                if (timerService != null && timerService.isTimerRunning()) {
                    updateButtonsState(true);
                } else {
                    updateButtonsState(false);
                }
            } else {
                Toast.makeText(this, "Quyền bị từ chối. Một số chức năng hẹn giờ có thể không hoạt động.", Toast.LENGTH_LONG).show();
                updateButtonsState(false);
            }
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
        if (isBound) {
            timerService.setTimerUpdateListener(null);
            unbindService(serviceConnection);
            isBound = false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTimeUpdate(long millisUntilFinished) {
        currentSeekBarTimeMillis = millisUntilFinished;
        updateCountDownText(millisUntilFinished);
        timerSeekBar.setProgress((int) (millisUntilFinished / (60 * 1000)));
    }

    @Override
    public void onTimerFinished() {
        currentSeekBarTimeMillis = 30 * 60 * 1000;
        updateCountDownText(currentSeekBarTimeMillis);
        timerSeekBar.setProgress((int) (currentSeekBarTimeMillis / (60 * 1000)));
        updateButtonsState(false);

    }

    private void updateCountDownText(long millis) {
        int minutes = (int) (millis / 1000) / 60;
        int seconds = (int) (millis / 1000) % 60;
        String timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        selectedTimeTextView.setText(timeLeftFormatted);

        if (seconds == 0) {
            selectedTimeTextView.setText(String.format(Locale.getDefault(), "%d", minutes));
        }
    }

    private void setPresetTime(int minutes) {
        if (timerService == null || !timerService.isTimerRunning()) {
            currentSeekBarTimeMillis = (long) minutes * 60 * 1000;
            timerSeekBar.setProgress(minutes);
            updateCountDownText(currentSeekBarTimeMillis);
            updateButtonsState(false);
        }
    }

    private void updateButtonsState(boolean isRunning) {
        timerSeekBar.setEnabled(!isRunning);
        preset30minButton.setEnabled(!isRunning);
        preset60minButton.setEnabled(!isRunning);
        preset90minButton.setEnabled(!isRunning);
        startButton.setEnabled(!isRunning);
        cancelButton.setEnabled(isRunning);
    }


}
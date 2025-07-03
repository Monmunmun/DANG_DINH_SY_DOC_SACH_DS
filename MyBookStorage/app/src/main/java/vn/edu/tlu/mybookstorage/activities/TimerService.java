package vn.edu.tlu.mybookstorage.activities;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import java.util.Locale;

public class TimerService extends Service {

    private CountDownTimer countdownTimer;
    private long timeLeftInMillis = 0;
    private boolean isTimerRunning = false;
    private long initialSetTimeMillis = 30 * 60 * 1000;

    private final IBinder binder = new LocalBinder();

    public interface OnTimerUpdateListener {
        void onTimeUpdate(long millisUntilFinished);
        void onTimerFinished();
    }
    private OnTimerUpdateListener timerUpdateListener;

    private static final String CHANNEL_ID_SERVICE = "timer_service_channel";
    private static final int NOTIFICATION_ID_SERVICE = 2;

    private static final String CHANNEL_ID_ALERT = "timer_alert_channel";
    private static final int NOTIFICATION_ID_ALERT = 1;


    public class LocalBinder extends Binder {
        TimerService getService() {
            return TimerService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createServiceNotificationChannel();
        createAlertNotificationChannel();
        startForeground(NOTIFICATION_ID_SERVICE, buildServiceNotification("Hẹn giờ đang chờ...", ""));
        timeLeftInMillis = initialSetTimeMillis;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (countdownTimer != null) {
            countdownTimer.cancel();
        }
        stopForeground(true);
    }

    /**
     * Bắt đầu hẹn giờ.
     */
    public void startTimer(long durationMillis) {
        if (isTimerRunning) {
            if (countdownTimer != null) {
                countdownTimer.cancel();
            }
        }

        this.initialSetTimeMillis = durationMillis;
        this.timeLeftInMillis = durationMillis;
        isTimerRunning = true;

        countdownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateServiceNotification();
                if (timerUpdateListener != null) {
                    timerUpdateListener.onTimeUpdate(timeLeftInMillis);
                }
            }

            @Override
            public void onFinish() {
                isTimerRunning = false;
                timeLeftInMillis = initialSetTimeMillis;

                showTimerFinishedAlertNotification();

                if (timerUpdateListener != null) {
                    timerUpdateListener.onTimerFinished();
                }
                stopSelf(); 
            }
        }.start();

        updateServiceNotification();
    }

    /**
     * Hủy hẹn giờ.
     */
    public void cancelTimer() {
        if (countdownTimer != null) {
            countdownTimer.cancel();
        }
        isTimerRunning = false;
        timeLeftInMillis = initialSetTimeMillis;
        stopSelf();
    }

    /**
     * Lấy thời gian còn lại hiện tại.
     */
    public long getTimeLeftInMillis() {
        return timeLeftInMillis;
    }

    /**
     * Kiểm tra xem hẹn giờ có đang chạy không.
     */
    public boolean isTimerRunning() {
        return isTimerRunning;
    }

    /**
     * Lấy thời gian đã đặt ban đầu.
     */
    public long getInitialSetTimeMillis() {
        return initialSetTimeMillis;
    }

    /**
     * Đặt listener để nhận cập nhật từ Service.
     */
    public void setTimerUpdateListener(OnTimerUpdateListener listener) {
        this.timerUpdateListener = listener;
    }

    /**
     * Tạo kênh thông báo cho foreground service (Android Oreo trở lên).
     */
    private void createServiceNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Timer Service Channel";
            String description = "Channel for Timer Service notifications";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID_SERVICE, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * Xây dựng đối tượng Notification cho foreground service.
     */
    private Notification buildServiceNotification(String title, String content) {
        Intent notificationIntent = new Intent(this, TimerActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID_SERVICE)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setSilent(true)
                .build();
    }

    /**
     * Cập nhật thông báo foreground với thời gian hiện tại.
     */
    private void updateServiceNotification() {
        String contentText;
        if (isTimerRunning) {
            long minutes = (timeLeftInMillis / 1000) / 60;
            long seconds = (timeLeftInMillis / 1000) % 60;
            contentText = String.format(Locale.getDefault(), "Thời gian còn lại: %02d:%02d", minutes, seconds);
        } else {
            contentText = "Hẹn giờ không hoạt động";
        }
        Notification notification = buildServiceNotification("Hẹn giờ đọc sách", contentText);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID_SERVICE, notification);
    }

    /**
     * Tạo kênh thông báo cho alert khi hết giờ
     */
    private void createAlertNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Timer Alert Channel";
            String description = "Channel for Timer completion alerts";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID_ALERT, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * Hiển thị thông báo alert khi hẹn giờ kết thúc từ Service.
     */
    private void showTimerFinishedAlertNotification() {
        Intent intent = new Intent(this, TimerActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID_ALERT)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Hết giờ!")
                .setContentText("Thời gian đọc sách của bạn đã kết thúc.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID_ALERT, builder.build());
    }
}
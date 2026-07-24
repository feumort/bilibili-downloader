package com.xq.bilibilidownloader;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

public class DownloadService extends Service {

    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "bilibili_download";
    private Runnable serviceCallback;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, buildNotification());

        serviceCallback = this::updateNotification;
        DownloadManager.getInstance().addCallback(serviceCallback);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (serviceCallback != null) {
            DownloadManager.getInstance().removeCallback(serviceCallback);
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void updateNotification() {
        DownloadManager dm = DownloadManager.getInstance();
        if (!dm.hasActiveTasks()) {
            stopForeground(true);
            stopSelf();
        } else {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.notify(NOTIFICATION_ID, buildNotification());
            }
        }
    }

    private Notification buildNotification() {
        DownloadManager dm = DownloadManager.getInstance();
        int active = dm.getActiveCount();
        String contentText;
        if (active > 0) {
            contentText = "正在下载 " + active + " 个视频";
        } else {
            contentText = "下载完成";
        }

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("B站下载器")
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setOngoing(active > 0)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "下载服务", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("显示下载进度");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }
}

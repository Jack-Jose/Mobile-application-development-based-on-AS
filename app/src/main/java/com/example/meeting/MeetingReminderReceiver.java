package com.example.meeting;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class MeetingReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return; // 空指针防护

        String title = "会议即将开始";
        String msg = intent.getStringExtra("msg");
        if (msg == null) msg = "您预约的会议室即将使用"; // 默认提示

        // 1. 创建通知渠道（Android O+ 必须）
        String channelId = "meeting_remind";
        String channelName = "会议提醒";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("会议室预约提醒通知");
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }

        // 2. 构建通知（兼容低版本）
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setContentTitle(title)
                .setContentText(msg)
                .setSmallIcon(R.mipmap.ic_launcher) // 确保mipmap存在ic_launcher资源
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true) // 点击后自动取消
                .setDefaults(NotificationCompat.DEFAULT_ALL); // 震动+声音+灯光

        // 3. 发送通知（检查通知权限）
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        if (notificationManager.areNotificationsEnabled()) {
            Notification notification = builder.build();
            long reservationId = intent.getLongExtra("reservation_id", 0);
            notificationManager.notify(Long.hashCode(reservationId), notification);
        }
    }
}

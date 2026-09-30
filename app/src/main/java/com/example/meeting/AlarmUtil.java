package com.example.meeting;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.annotation.NonNull;

public class AlarmUtil {
    @SuppressLint("ScheduleExactAlarm")
    public static void setAlarm(@NonNull Context context, long triggerTime, String msg) {
        if (context == null || triggerTime < System.currentTimeMillis()) return;

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent intent = new Intent(context, MeetingReminderReceiver.class);
        intent.putExtra("msg", msg);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pi = PendingIntent.getBroadcast(context, 100, intent, flags);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pi);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pi);
        }
    }
}
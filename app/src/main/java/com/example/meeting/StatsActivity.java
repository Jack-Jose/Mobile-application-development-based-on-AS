package com.example.meeting;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class StatsActivity extends AppCompatActivity {
    private TextView tvTotalRooms, tvTotalReservations, tvTodayReservations,
            tvUsageRate, tvPeakHour, tvTopRoom;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        tvTotalRooms = findViewById(R.id.tv_total_rooms);
        tvTotalReservations = findViewById(R.id.tv_total_reservations);
        tvTodayReservations = findViewById(R.id.tv_today_reservations);
        tvUsageRate = findViewById(R.id.tv_usage_rate);
        tvPeakHour = findViewById(R.id.tv_peak_hour);
        tvTopRoom = findViewById(R.id.tv_top_room);

        loadStats();
    }

    private void loadStats() {
        Cursor roomCursor = getContentResolver().query(MeetingProvider.URI_ROOM,
                null, null, null, null);
        int roomCount = roomCursor != null ? roomCursor.getCount() : 0;
        tvTotalRooms.setText(String.valueOf(roomCount));
        if (roomCursor != null) roomCursor.close();

        Cursor resCursor = getContentResolver().query(MeetingProvider.URI_RES,
                null, null, null, null);
        int totalRes = resCursor != null ? resCursor.getCount() : 0;
        tvTotalReservations.setText(String.valueOf(totalRes));
        if (resCursor != null) resCursor.close();

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0);
        long todayStart = cal.getTimeInMillis();
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59);
        long todayEnd = cal.getTimeInMillis();

        Cursor todayCursor = getContentResolver().query(MeetingProvider.URI_RES,
                null,
                DBHelper.RES_START + ">=? AND " + DBHelper.RES_START + "<?",
                new String[]{String.valueOf(todayStart), String.valueOf(todayEnd)},
                null);
        int todayCount = todayCursor != null ? todayCursor.getCount() : 0;
        tvTodayReservations.setText(String.valueOf(todayCount));
        if (todayCursor != null) todayCursor.close();

        Cursor usedRoomCursor = getContentResolver().query(MeetingProvider.URI_RES,
                new String[]{"DISTINCT " + DBHelper.RES_ROOM_ID},
                DBHelper.RES_START + ">=? AND " + DBHelper.RES_START + "<?",
                new String[]{String.valueOf(todayStart), String.valueOf(todayEnd)},
                null);
        int usedRooms = usedRoomCursor != null ? usedRoomCursor.getCount() : 0;
        if (usedRoomCursor != null) usedRoomCursor.close();
        int usagePercent = roomCount > 0 ? usedRooms * 100 / roomCount : 0;
        tvUsageRate.setText(usagePercent + "%");

        tvPeakHour.setText(calcPeakHour());
        tvTopRoom.setText(calcTopRoom());
    }

    private String calcPeakHour() {
        Cursor c = getContentResolver().query(MeetingProvider.URI_RES,
                new String[]{DBHelper.RES_START}, null, null, null);
        if (c == null || c.getCount() == 0) { if (c != null) c.close(); return "暂无数据"; }

        int[] hourCount = new int[24];
        Calendar cal = Calendar.getInstance();
        while (c.moveToNext()) {
            cal.setTimeInMillis(c.getLong(0));
            hourCount[cal.get(Calendar.HOUR_OF_DAY)]++;
        }
        c.close();

        int peakIdx = 0;
        for (int i = 1; i < 24; i++) if (hourCount[i] > hourCount[peakIdx]) peakIdx = i;
        return hourCount[peakIdx] > 0
                ? peakIdx + ":00 - " + (peakIdx + 1) + ":00（" + hourCount[peakIdx] + "次）"
                : "暂无数据";
    }

    private String calcTopRoom() {
        Cursor c = getContentResolver().query(MeetingProvider.URI_RES,
                new String[]{DBHelper.RES_ROOM_ID}, null, null, null);
        if (c == null || c.getCount() == 0) { if (c != null) c.close(); return "暂无数据"; }

        Map<Integer, Integer> countMap = new HashMap<>();
        while (c.moveToNext()) {
            int rid = c.getInt(0);
            countMap.put(rid, countMap.getOrDefault(rid, 0) + 1);
        }
        c.close();

        int topRoom = -1, topCount = 0;
        for (Map.Entry<Integer, Integer> e : countMap.entrySet()) {
            if (e.getValue() > topCount) { topCount = e.getValue(); topRoom = e.getKey(); }
        }

        if (topRoom > 0) {
            Cursor roomCursor = getContentResolver().query(MeetingProvider.URI_ROOM,
                    new String[]{DBHelper.ROOM_NAME}, DBHelper.ROOM_ID + "=?",
                    new String[]{String.valueOf(topRoom)}, null);
            String name = "会议室" + topRoom;
            if (roomCursor != null && roomCursor.moveToFirst()) name = roomCursor.getString(0);
            if (roomCursor != null) roomCursor.close();
            return name + "（" + topCount + "次）";
        }
        return "暂无数据";
    }
}
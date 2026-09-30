package com.example.meeting;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private int uid, role;
    private String uname;
    private SharedPreferences prefs;
    private TextView tvWelcome, tvDate, tvTodayBookings, tvAvailRooms, tvMyCount, tvPendingCount, tvRoleBadge;
    private LinearLayout cardApproval, noticeContainer;
    private BottomNavigationView nav;
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("MM月dd日 EEEE", Locale.CHINA);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_main);
        } catch (Exception e) {
            Log.e(TAG, "setContentView failed", e);
            finish();
            return;
        }

        prefs = getSharedPreferences("meeting_prefs", MODE_PRIVATE);
        uid = getIntent().getIntExtra("uid", prefs.getInt("uid", 1));
        uname = getIntent().getStringExtra("uname");
        if (uname == null || uname.isEmpty()) uname = prefs.getString("uname", "用户");
        role = getIntent().getIntExtra("role", prefs.getInt("role", 0));

        // Safe findViewById with null checks
        tvWelcome = findViewById(R.id.tv_welcome);
        tvDate = findViewById(R.id.tv_date);
        tvRoleBadge = findViewById(R.id.tv_role_badge);

        if (tvWelcome != null) tvWelcome.setText("欢迎回来，" + uname);
        if (tvDate != null) tvDate.setText(dateFmt.format(new Date()));
        if (tvRoleBadge != null) {
            tvRoleBadge.setText(DBHelper.adminName(role));
            int badgeColor = role >= 4 ? 0xFFF59E0B : role >= 2 ? 0xFF2563EB : role >= 1 ? 0xFF059669 : 0xFF94A3B8;
            tvRoleBadge.setBackgroundColor(badgeColor);
        }

        tvTodayBookings = findViewById(R.id.tv_today_bookings);
        tvAvailRooms = findViewById(R.id.tv_avail_rooms);
        tvMyCount = findViewById(R.id.tv_my_count);
        tvPendingCount = findViewById(R.id.tv_pending_count);
        cardApproval = findViewById(R.id.card_approval);
        noticeContainer = findViewById(R.id.notice_container);

        if (cardApproval != null) {
            if (role >= 1) {
                cardApproval.setVisibility(View.VISIBLE);
                cardApproval.setOnClickListener(v -> {
                    try { startActivity(new Intent(this, ApprovalActivity.class)); }
                    catch (Exception e) { Log.e(TAG, "Approval launch", e); }
                });
            } else {
                cardApproval.setVisibility(View.GONE);
            }
        }

        // Safe card listeners
        View cardBook = findViewById(R.id.card_book);
        View cardRooms = findViewById(R.id.card_rooms);
        View cardStats = findViewById(R.id.card_stats);
        View cardMy = findViewById(R.id.card_my);
        View cardAll = findViewById(R.id.card_all);
        View cardProfile = findViewById(R.id.card_profile);

        if (cardBook != null) cardBook.setOnClickListener(v -> safeStart(BookingActivity.class));
        if (cardRooms != null) cardRooms.setOnClickListener(v -> safeStart(RoomManagementActivity.class));
        if (cardStats != null) cardStats.setOnClickListener(v -> safeStart(StatsActivity.class));
        if (cardMy != null) cardMy.setOnClickListener(v -> safeStart(MyReservationsActivity.class));
        if (cardAll != null) cardAll.setOnClickListener(v -> safeStart(AllReservationsActivity.class));
        if (cardProfile != null) cardProfile.setOnClickListener(v -> safeStart(ProfileActivity.class));

        nav = findViewById(R.id.bottom_nav);
        if (nav != null) {
            nav.setOnItemSelectedListener(this::onNavItemSelected);
        }
    }

    private void safeStart(Class<?> cls) {
        try {
            startActivity(new Intent(this, cls));
        } catch (Exception e) {
            Log.e(TAG, "Failed to start " + cls.getSimpleName(), e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            loadDashboardData();
        } catch (Exception e) {
            Log.e(TAG, "loadDashboardData failed", e);
        }
        if (nav != null && nav.getMenu() != null && nav.getMenu().size() > 0) {
            nav.getMenu().getItem(0).setChecked(true);
        }
    }

    private void loadDashboardData() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0);
        long todayStart = cal.getTimeInMillis();
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59);
        long todayEnd = cal.getTimeInMillis();

        Cursor c1 = null, c2 = null, c3 = null, c4 = null;
        try {
            c1 = getContentResolver().query(MeetingProvider.URI_RES,
                    null, DBHelper.RES_START + ">=? AND " + DBHelper.RES_START + "<?",
                    new String[]{String.valueOf(todayStart), String.valueOf(todayEnd)}, null);
            if (tvTodayBookings != null) tvTodayBookings.setText(c1 != null ? String.valueOf(c1.getCount()) : "0");

            c2 = getContentResolver().query(MeetingProvider.URI_ROOM,
                    null, DBHelper.ROOM_STATUS + "=0", null, null);
            if (tvAvailRooms != null) tvAvailRooms.setText(c2 != null ? String.valueOf(c2.getCount()) : "0");

            c3 = getContentResolver().query(MeetingProvider.URI_RES,
                    null, DBHelper.RES_USER_ID + "=?", new String[]{String.valueOf(uid)}, null);
            if (tvMyCount != null) tvMyCount.setText(c3 != null ? String.valueOf(c3.getCount()) : "0");

            if (role >= 1 && tvPendingCount != null) {
                c4 = getContentResolver().query(MeetingProvider.URI_RES,
                        null, DBHelper.RES_STATUS + "=0", null, null);
                tvPendingCount.setText(String.valueOf(c4 != null ? c4.getCount() : 0));
            }
        } catch (Exception e) {
            Log.e(TAG, "Dashboard query error", e);
        } finally {
            try { if (c1 != null) c1.close(); } catch (Exception ignored) {}
            try { if (c2 != null) c2.close(); } catch (Exception ignored) {}
            try { if (c3 != null) c3.close(); } catch (Exception ignored) {}
            try { if (c4 != null) c4.close(); } catch (Exception ignored) {}
        }

        try { loadNotices(); } catch (Exception e) { Log.e(TAG, "loadNotices", e); }
    }

    private void loadNotices() {
        if (noticeContainer == null) return;
        noticeContainer.removeAllViews();
        Cursor c = null;
        try {
            c = getContentResolver().query(MeetingProvider.URI_NOTICE,
                    null, null, null, DBHelper.NOTICE_TIME + " DESC LIMIT 3");
            if (c != null && c.getCount() > 0) {
                while (c.moveToNext()) {
                    String title = c.getString(c.getColumnIndexOrThrow(DBHelper.NOTICE_TITLE));
                    String content = c.getString(c.getColumnIndexOrThrow(DBHelper.NOTICE_CONTENT));
                    View notice = getLayoutInflater().inflate(R.layout.item_notice, noticeContainer, false);
                    TextView tvTitle = notice.findViewById(R.id.tv_notice_title);
                    TextView tvContent = notice.findViewById(R.id.tv_notice_content);
                    if (tvTitle != null) tvTitle.setText(title);
                    if (tvContent != null) tvContent.setText(content);
                    noticeContainer.addView(notice);
                }
            } else {
                View empty = getLayoutInflater().inflate(R.layout.item_notice, noticeContainer, false);
                TextView tvTitle = empty.findViewById(R.id.tv_notice_title);
                TextView tvContent = empty.findViewById(R.id.tv_notice_content);
                if (tvTitle != null) tvTitle.setText("暂无公告");
                if (tvContent != null) tvContent.setText("系统运行正常");
                noticeContainer.addView(empty);
            }
        } catch (Exception e) {
            Log.e(TAG, "Notice load error", e);
        } finally {
            if (c != null) { try { c.close(); } catch (Exception ignored) {} }
        }
    }

    private boolean onNavItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Intent intent = null;
        if (id == R.id.nav_home) {
            return true;
        } else if (id == R.id.nav_book) {
            intent = new Intent(this, BookingActivity.class);
        } else if (id == R.id.nav_rooms) {
            intent = new Intent(this, RoomManagementActivity.class);
        } else if (id == R.id.nav_my) {
            intent = new Intent(this, MyReservationsActivity.class);
        } else if (id == R.id.nav_stats) {
            intent = new Intent(this, StatsActivity.class);
        }
        if (intent != null) {
            intent.putExtra("uid", uid);
            intent.putExtra("uname", uname);
            intent.putExtra("role", role);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            try {
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            } catch (Exception e) {
                Log.e(TAG, "Nav start failed", e);
            }
            return true;
        }
        return false;
    }
}

package com.example.meeting;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {
    private TextView tvUsername, tvRole, tvDept, tvPhone, tvSubStatus, tvSubExpiry;
    private Button btnLogout;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        prefs = getSharedPreferences("meeting_prefs", MODE_PRIVATE);
        tvUsername = findViewById(R.id.tv_profile_username);
        tvRole = findViewById(R.id.tv_profile_role);
        tvDept = findViewById(R.id.tv_profile_dept);
        tvPhone = findViewById(R.id.tv_profile_phone);
        tvSubStatus = findViewById(R.id.tv_profile_sub_status);
        tvSubExpiry = findViewById(R.id.tv_profile_sub_expiry);
        btnLogout = findViewById(R.id.btn_logout);

        int uid = prefs.getInt("uid", -1);
        if (uid > 0) {
            Cursor c = null;
            try {
                c = getContentResolver().query(MeetingProvider.URI_USER,
                        new String[]{DBHelper.USER_NAME, DBHelper.USER_ADMIN_LV,
                                DBHelper.USER_DEPT, DBHelper.USER_PHONE, DBHelper.USER_SUB_EXPIRY},
                        DBHelper.USER_ID + "=?", new String[]{String.valueOf(uid)}, null);
                if (c != null && c.moveToFirst()) {
                    tvUsername.setText(c.getString(0));
                    int adminLv = c.getInt(1);
                    tvRole.setText(DBHelper.adminName(adminLv) + " (Lv." + adminLv + ")");
                    tvDept.setText(c.getString(2) != null ? c.getString(2) : "-");
                    tvPhone.setText(c.getString(3) != null ? c.getString(3) : "-");

                    // 订阅状态
                    long subExpiry = c.getLong(4);
                    boolean active = subExpiry > System.currentTimeMillis();
                    if (tvSubStatus != null) {
                        tvSubStatus.setText(active ? "已订阅" : "未订阅");
                        tvSubStatus.setTextColor(active ? 0xFF00897B : 0xFFEF4444);
                    }
                    if (tvSubExpiry != null) {
                        if (active) {
                            tvSubExpiry.setText("到期日: " + new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(new Date(subExpiry)));
                            tvSubExpiry.setTextColor(0xFF00897B);
                        } else {
                            tvSubExpiry.setText("开通订阅仅需 ¥99/月");
                            tvSubExpiry.setTextColor(0xFF757575);
                        }
                    }
                }
            } finally { if (c != null) c.close(); }
        }

        btnLogout.setOnClickListener(v -> {
            prefs.edit().clear().apply();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
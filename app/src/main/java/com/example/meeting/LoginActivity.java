package com.example.meeting;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private EditText etUsername, etPassword;
    private Button btnLogin;
    private TextView tvRegister;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_login);
        } catch (Exception e) {
            Log.e(TAG, "setContentView failed", e);
            Toast.makeText(this, "页面加载失败，请重新安装", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        prefs = getSharedPreferences("meeting_prefs", MODE_PRIVATE);

        // Only auto-login if explicitly set (skip if backup-restored stale data)
        if (prefs.getBoolean("auto_login", false) && prefs.getBoolean("login_verified", false)) {
            int uid = prefs.getInt("uid", -1);
            String uname = prefs.getString("uname", "");
            int adminLv = prefs.getInt("admin_level", 0);
            if (uid > 0 && !uname.isEmpty()) {
                try {
                    startMain(uid, uname, adminLv);
                    return;
                } catch (Exception e) {
                    Log.e(TAG, "Auto-login failed", e);
                    prefs.edit().clear().apply();
                }
            }
        }

        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvRegister = findViewById(R.id.tv_register);

        if (btnLogin != null) btnLogin.setOnClickListener(v -> doLogin());
        if (tvRegister != null) tvRegister.setOnClickListener(v -> {
            try {
                startActivityForResult(new Intent(this, RegisterActivity.class), 100);
            } catch (Exception e) {
                Log.e(TAG, "Register launch failed", e);
            }
        });
    }

    private void doLogin() {
        if (etUsername == null || etPassword == null) return;
        String uname = etUsername.getText().toString().trim();
        String pwd = etPassword.getText().toString().trim();
        if (uname.isEmpty() || pwd.isEmpty()) {
            Toast.makeText(this, "请输入用户名和密码", Toast.LENGTH_SHORT).show();
            return;
        }

        Cursor c = null;
        try {
            c = getContentResolver().query(
                    MeetingProvider.URI_USER,
                    new String[]{DBHelper.USER_ID, DBHelper.USER_NAME, DBHelper.USER_ADMIN_LV, DBHelper.USER_DEPT},
                    DBHelper.USER_NAME + "=? AND " + DBHelper.USER_PWD + "=?",
                    new String[]{uname, pwd}, null);

            if (c != null && c.moveToFirst()) {
                int uid = c.getInt(0);
                String name = c.getString(1);
                int adminLv = c.getInt(2);
                String dept = c.getString(3);

                prefs.edit()
                        .putBoolean("auto_login", true)
                        .putBoolean("login_verified", true)
                        .putInt("uid", uid)
                        .putString("uname", name)
                        .putInt("admin_level", adminLv)
                        .putInt("role", adminLv)
                        .putString("dept", dept != null ? dept : "")
                        .apply();

                startMain(uid, name, adminLv);
            } else {
                Toast.makeText(this, "用户名或密码错误", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Login query failed", e);
            Toast.makeText(this, "登录异常，请清除应用数据后重试", Toast.LENGTH_LONG).show();
        } finally {
            if (c != null) {
                try { c.close(); } catch (Exception ignored) {}
            }
        }
    }

    private void startMain(int uid, String uname, int adminLv) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("uid", uid);
        intent.putExtra("uname", uname);
        intent.putExtra("role", adminLv);
        intent.putExtra("admin_level", adminLv);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == Activity.RESULT_OK && data != null) {
            String uname = data.getStringExtra("username");
            String pwd = data.getStringExtra("password");
            if (etUsername != null && uname != null) etUsername.setText(uname);
            if (etPassword != null && pwd != null) etPassword.setText(pwd);
        }
    }
}
package com.example.meeting;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {
    private EditText etUsername, etPassword, etConfirm, etPhone, etDept;
    private Button btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etUsername = findViewById(R.id.et_reg_username);
        etPassword = findViewById(R.id.et_reg_password);
        etConfirm = findViewById(R.id.et_reg_confirm);
        etPhone = findViewById(R.id.et_reg_phone);
        etDept = findViewById(R.id.et_reg_dept);
        btnRegister = findViewById(R.id.btn_register);

        btnRegister.setOnClickListener(v -> doRegister());
    }

    private void doRegister() {
        String uname = etUsername.getText().toString().trim();
        String pwd = etPassword.getText().toString().trim();
        String confirm = etConfirm.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String dept = etDept.getText().toString().trim();

        if (uname.isEmpty() || pwd.isEmpty()) {
            Toast.makeText(this, "用户名和密码不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!pwd.equals(confirm)) {
            Toast.makeText(this, "两次密码不一致", Toast.LENGTH_SHORT).show();
            return;
        }

        ContentValues cv = new ContentValues();
        cv.put(DBHelper.USER_NAME, uname);
        cv.put(DBHelper.USER_PWD, pwd);
        cv.put(DBHelper.USER_ROLE, 0);
        cv.put(DBHelper.USER_PHONE, phone);
        cv.put(DBHelper.USER_DEPT, dept);

        try {
            getContentResolver().insert(MeetingProvider.URI_USER_REG, cv);
            Toast.makeText(this, "注册成功！请登录", Toast.LENGTH_SHORT).show();
            Intent data = new Intent();
            data.putExtra("username", uname);
            data.putExtra("password", pwd);
            setResult(Activity.RESULT_OK, data);
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "注册失败，用户名可能已存在", Toast.LENGTH_SHORT).show();
        }
    }
}

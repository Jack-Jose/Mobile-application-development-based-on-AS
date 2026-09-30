package com.example.meeting;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ApprovalActivity extends AppCompatActivity {
    private RecyclerView rv;
    private ApprovalAdapter adapter;
    private TextView tvEmpty;
    private final List<ApprovalItem> items = new ArrayList<>();
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);
    private int currentAdminLv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_approval);

        SharedPreferences prefs = getSharedPreferences("meeting_prefs", MODE_PRIVATE);
        currentAdminLv = prefs.getInt("admin_level", 0);

        if (currentAdminLv < 1) {
            Toast.makeText(this, "无审批权限", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setTitle("审批中心 (" + DBHelper.adminName(currentAdminLv) + ")");
        rv = findViewById(R.id.rv_approval);
        tvEmpty = findViewById(R.id.tv_empty);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ApprovalAdapter();
        rv.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPending();
    }

    private void loadPending() {
        items.clear();
        Cursor c = null;
        try {
            c = getContentResolver().query(MeetingProvider.URI_RES,
                    null, DBHelper.RES_STATUS + "=0", null,
                    DBHelper.RES_START + " ASC");
            if (c != null) {
                while (c.moveToNext()) {
                    long resId = c.getLong(c.getColumnIndexOrThrow(DBHelper.RES_ID));
                    int roomId = c.getInt(c.getColumnIndexOrThrow(DBHelper.RES_ROOM_ID));
                    int userId = c.getInt(c.getColumnIndexOrThrow(DBHelper.RES_USER_ID));
                    long start = c.getLong(c.getColumnIndexOrThrow(DBHelper.RES_START));
                    long end = c.getLong(c.getColumnIndexOrThrow(DBHelper.RES_END));
                    String title = c.getString(c.getColumnIndexOrThrow(DBHelper.RES_TITLE));
                    int attendees = c.getInt(c.getColumnIndexOrThrow(DBHelper.RES_ATTENDEES));
                    String note = c.getString(c.getColumnIndexOrThrow(DBHelper.RES_NOTE));
                    double fee = c.getDouble(c.getColumnIndexOrThrow(DBHelper.RES_TOTAL_FEE));
                    double deposit = c.getDouble(c.getColumnIndexOrThrow(DBHelper.RES_DEPOSIT));
                    int isPaid = c.getInt(c.getColumnIndexOrThrow(DBHelper.RES_PAID));
                    if (note == null) note = "";

                    String roomName = "会议室" + roomId;
                    Cursor rc = null;
                    try {
                        rc = getContentResolver().query(MeetingProvider.URI_ROOM,
                                new String[]{DBHelper.ROOM_NAME}, DBHelper.ROOM_ID + "=?",
                                new String[]{String.valueOf(roomId)}, null);
                        if (rc != null && rc.moveToFirst()) roomName = rc.getString(0);
                    } finally { if (rc != null) rc.close(); }

                    String userName = "用户" + userId;
                    Cursor uc = null;
                    try {
                        uc = getContentResolver().query(MeetingProvider.URI_USER,
                                new String[]{DBHelper.USER_NAME, DBHelper.USER_DEPT},
                                DBHelper.USER_ID + "=?", new String[]{String.valueOf(userId)}, null);
                        if (uc != null && uc.moveToFirst()) {
                            userName = uc.getString(0);
                            String dept = uc.getString(1);
                            if (dept != null && !dept.isEmpty()) userName += " (" + dept + ")";
                        }
                    } finally { if (uc != null) uc.close(); }

                    items.add(new ApprovalItem(resId, roomName, userName, title,
                            attendees, sdf.format(new Date(start)), sdf.format(new Date(end)),
                            note, fee, deposit, isPaid));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        finally { if (c != null) c.close(); }

        if (tvEmpty != null) tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.notifyDataSetChanged();
    }

    private void approve(ApprovalItem item) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.RES_STATUS, 1); // approved
        getContentResolver().update(
                Uri.withAppendedPath(MeetingProvider.URI_RES, String.valueOf(item.resId)), cv, null, null);

        // Show payment notification
        new AlertDialog.Builder(this)
            .setTitle("审批通过")
            .setMessage("已批准: " + item.title + "\n"
                + "会议室: " + item.roomName + "\n"
                + "费用: ¥" + String.format("%.2f", item.fee) + "\n"
                + "押金: ¥" + String.format("%.2f", item.deposit) + "\n\n"
                + "请通知用户支付费用\n管理员可代为支付后向用户当面收取")
            .setPositiveButton("我已知晓", (d, w) -> loadPending())
            .setNeutralButton("立即代付", (d, w) -> markAsPaid(item))
            .show();
    }

    private void reject(ApprovalItem item) {
        ContentValues cv = new ContentValues();
        cv.put(DBHelper.RES_STATUS, 2); // rejected
        cv.put(DBHelper.RES_DEPOSIT_STATUS, 2); // refund deposit
        getContentResolver().update(
                Uri.withAppendedPath(MeetingProvider.URI_RES, String.valueOf(item.resId)), cv, null, null);
        Toast.makeText(this, "已拒绝，押金¥" + String.format("%.2f", item.deposit) + "已退回", Toast.LENGTH_SHORT).show();
        loadPending();
    }

    private void markAsPaid(ApprovalItem item) {
        new AlertDialog.Builder(this)
            .setTitle("确认代付")
            .setMessage("确认支付 ¥" + String.format("%.2f", item.fee) + "？\n\n支付后将向预约人当面收取此费用")
            .setPositiveButton("确认支付", (d, w) -> {
                ContentValues cv = new ContentValues();
                cv.put(DBHelper.RES_PAID, 1);
                getContentResolver().update(
                    Uri.withAppendedPath(MeetingProvider.URI_RES, String.valueOf(item.resId)), cv, null, null);
                Toast.makeText(this, "已支付¥" + String.format("%.2f", item.fee) + "，请向预约人当面收取", Toast.LENGTH_LONG).show();
                loadPending();
            })
            .setNegativeButton("取消", null)
            .show();
    }

    class ApprovalAdapter extends RecyclerView.Adapter<ApprovalAdapter.Holder> {
        @Override public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(ApprovalActivity.this).inflate(R.layout.item_approval, parent, false));
        }
        @Override public void onBindViewHolder(Holder h, int pos) {
            ApprovalItem a = items.get(pos);
            h.tvTitle.setText(a.title);
            h.tvRoom.setText(a.roomName);
            h.tvUser.setText("申请人: " + a.userName);
            h.tvTime.setText(a.startTime + " ~ " + a.endTime);
            h.tvDetail.setText(a.attendees + "人参会 | 费用¥" + String.format("%.2f", a.fee) + " | 押金¥" + String.format("%.2f", a.deposit) + (a.note.isEmpty() ? "" : " | " + a.note));
            h.btnApprove.setOnClickListener(v -> approve(a));
            h.btnReject.setOnClickListener(v -> reject(a));
            h.btnPaid.setOnClickListener(v -> markAsPaid(a));
        }
        @Override public int getItemCount() { return items.size(); }
        class Holder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvRoom, tvUser, tvTime, tvDetail;
            Button btnApprove, btnReject, btnPaid;
            Holder(View v) {
                super(v);
                tvTitle = v.findViewById(R.id.tv_approval_title);
                tvRoom = v.findViewById(R.id.tv_approval_room);
                tvUser = v.findViewById(R.id.tv_approval_user);
                tvTime = v.findViewById(R.id.tv_approval_time);
                tvDetail = v.findViewById(R.id.tv_approval_detail);
                btnApprove = v.findViewById(R.id.btn_approve);
                btnReject = v.findViewById(R.id.btn_reject);
                btnPaid = v.findViewById(R.id.btn_paid);
            }
        }
    }

    static class ApprovalItem {
        long resId; String roomName, userName, title, startTime, endTime, note; int attendees, isPaid; double fee, deposit;
        ApprovalItem(long id, String rn, String un, String t, int a, String st, String et, String n, double f, double d, int p) {
            resId=id; roomName=rn; userName=un; title=t; attendees=a; startTime=st; endTime=et; note=n; fee=f; deposit=d; isPaid=p;
        }
    }
}
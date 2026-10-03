package com.example.meeting;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MyReservationsActivity extends AppCompatActivity {
    private static final String TAG = "MyReservations";
    private RecyclerView rvReservations;
    private MyReservationAdapter adapter;
    private final List<MyReservation> list = new ArrayList<>();
    private final List<MyReservation> allList = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);
    private int currentUid, filterStatus = -1;
    private final String[] STATUS_NAMES = {"待审批", "已通过", "已拒绝", "已取消", "已完成"};
    private final int[] STATUS_COLORS = {0xFFFF9800, 0xFF4CAF50, 0xFFF44336, 0xFF9E9E9E, 0xFF2196F3};
    private TextView tvFilterAll, tvFilterPending, tvFilterApproved, tvFilterCancelled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_my_reservations);
        } catch (Exception e) {
            Log.e(TAG, "setContentView failed", e);
            Toast.makeText(this, "页面加载失败", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        SharedPreferences prefs = getSharedPreferences("meeting_prefs", MODE_PRIVATE);
        currentUid = getIntent().getIntExtra("uid", prefs.getInt("uid", 1));

        tvFilterAll = findViewById(R.id.tv_filter_all);
        tvFilterPending = findViewById(R.id.tv_filter_pending);
        tvFilterApproved = findViewById(R.id.tv_filter_approved);
        tvFilterCancelled = findViewById(R.id.tv_filter_cancelled);

        if (tvFilterAll != null) tvFilterAll.setOnClickListener(v -> setFilter(-1, tvFilterAll));
        if (tvFilterPending != null) tvFilterPending.setOnClickListener(v -> setFilter(0, tvFilterPending));
        if (tvFilterApproved != null) tvFilterApproved.setOnClickListener(v -> setFilter(1, tvFilterApproved));
        if (tvFilterCancelled != null) tvFilterCancelled.setOnClickListener(v -> setFilter(3, tvFilterCancelled));

        rvReservations = findViewById(R.id.rv_reservations);
        if (rvReservations != null) {
            rvReservations.setLayoutManager(new LinearLayoutManager(this));
            adapter = new MyReservationAdapter();
            rvReservations.setAdapter(adapter);
        }

        setFilter(-1, tvFilterAll);
        try {
            loadMyReservations();
        } catch (Exception e) {
            Log.e(TAG, "loadMyReservations failed", e);
            Toast.makeText(this, "加载失败，请重试", Toast.LENGTH_SHORT).show();
        }
    }

    private void setFilter(int status, TextView tv) {
        filterStatus = status;
        TextView[] tabs = {tvFilterAll, tvFilterPending, tvFilterApproved, tvFilterCancelled};
        for (TextView t : tabs) {
            if (t == null) continue;
            t.setBackgroundColor(t == tv ? 0xFF2563EB : 0xFFF1F5F9);
            t.setTextColor(t == tv ? 0xFFFFFFFF : 0xFF64748B);
        }
        applyFilter();
    }

    private void applyFilter() {
        list.clear();
        if (filterStatus == -1) {
            list.addAll(allList);
        } else {
            for (MyReservation r : allList) {
                if (r.status == filterStatus) list.add(r);
            }
        }
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void loadMyReservations() {
        allList.clear();
        Map<Integer, String> roomNames = new HashMap<>();
        try (Cursor rooms = getContentResolver().query(MeetingProvider.URI_ROOM,
                new String[]{DBHelper.ROOM_ID, DBHelper.ROOM_NAME}, null, null, null)) {
            if (rooms != null) {
                while (rooms.moveToNext()) roomNames.put(rooms.getInt(0), rooms.getString(1));
            }
        } catch (Exception e) {
            Log.e(TAG, "room query", e);
        }
        Cursor c = null;
        try {
            c = getContentResolver().query(MeetingProvider.URI_RES,
                    null, DBHelper.RES_USER_ID + "=?", new String[]{String.valueOf(currentUid)},
                    DBHelper.RES_START + " DESC");
            if (c != null) {
                while (c.moveToNext()) {
                    long resId = c.getLong(c.getColumnIndexOrThrow(DBHelper.RES_ID));
                    int roomId = c.getInt(c.getColumnIndexOrThrow(DBHelper.RES_ROOM_ID));
                    String title = c.getString(c.getColumnIndexOrThrow(DBHelper.RES_TITLE));
                    int attendees = c.getInt(c.getColumnIndexOrThrow(DBHelper.RES_ATTENDEES));
                    int status = c.getInt(c.getColumnIndexOrThrow(DBHelper.RES_STATUS));
                    long start = c.getLong(c.getColumnIndexOrThrow(DBHelper.RES_START));
                    long end = c.getLong(c.getColumnIndexOrThrow(DBHelper.RES_END));
                    String note = c.getString(c.getColumnIndexOrThrow(DBHelper.RES_NOTE));
                    if (note == null) note = "";

                    String roomName = roomNames.getOrDefault(roomId, "会议室" + roomId);

                    String sTime = dateFormat.format(new Date(start));
                    String eTime = dateFormat.format(new Date(end));

                    allList.add(new MyReservation(resId, roomId, roomName, title, attendees, status,
                            sTime, eTime, start, end, note));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "loadMyReservations query error", e);
        } finally {
            if (c != null) { try { c.close(); } catch (Exception ignored) {} }
        }
        applyFilter();
    }

    private void editReservation(MyReservation r) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_reservation, null);
        EditText etTitle = dialogView.findViewById(R.id.et_edit_title);
        EditText etDuration = dialogView.findViewById(R.id.et_duration);
        EditText etNote = dialogView.findViewById(R.id.et_edit_note);
        if (etTitle != null) etTitle.setText(r.title);
        if (etNote != null) etNote.setText(r.note);
        long durMs = r.endTimestamp - r.startTimestamp;
        if (etDuration != null) etDuration.setText(String.valueOf(durMs / 60000));

        builder.setView(dialogView)
                .setTitle("修改预约")
                .setPositiveButton("保存", (d, w) -> {
                    String ttl = etTitle != null ? etTitle.getText().toString().trim() : r.title;
                    String durStr = etDuration != null ? etDuration.getText().toString().trim() : "60";
                    String nt = etNote != null ? etNote.getText().toString().trim() : r.note;
                    try {
                        showDateTimePicker(r, Integer.parseInt(durStr), ttl, nt);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "请输入有效时长", Toast.LENGTH_SHORT).show();
                    }
                }).setNegativeButton("取消", null).show();
    }

    private void showDateTimePicker(MyReservation r, int duration, String title, String note) {
        if (duration <= 0 || duration > FeeCalculator.MAX_DURATION_MIN) {
            Toast.makeText(this, "预约时长必须在1到360分钟之间", Toast.LENGTH_SHORT).show();
            return;
        }
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, y, m, d) -> {
            new TimePickerDialog(this, (v, hh, mm) -> {
                Calendar cal = Calendar.getInstance();
                cal.set(y, m, d, hh, mm, 0);
                cal.set(Calendar.MILLISECOND, 0);
                long start = cal.getTimeInMillis();
                long end = start + duration * 60_000L;
                String hoursError = FeeCalculator.checkBusinessHours(start, end);
                if (start <= System.currentTimeMillis() || hoursError != null) {
                    Toast.makeText(this, hoursError != null ? hoursError : "不能选择过去的时间", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (hasReservationConflict(r.roomId, r.resId, start, end)) {
                    Toast.makeText(this, "该时段已被预约", Toast.LENGTH_SHORT).show();
                    return;
                }
                ContentValues cv = new ContentValues();
                cv.put(DBHelper.RES_START, start);
                cv.put(DBHelper.RES_END, end);
                cv.put(DBHelper.RES_TITLE, title);
                cv.put(DBHelper.RES_NOTE, note);
                int updated = getContentResolver().update(
                        Uri.withAppendedPath(MeetingProvider.URI_RES, String.valueOf(r.resId)),
                        cv, null, null);
                if (updated == 0) {
                    Toast.makeText(this, "修改失败，请重试", Toast.LENGTH_SHORT).show();
                    return;
                }
                AlarmUtil.cancelAlarm(this, r.resId);
                AlarmUtil.setAlarm(this, r.resId, start - 10 * 60_000L,
                        "会议《" + title + "》即将开始");
                Toast.makeText(this, "修改成功", Toast.LENGTH_SHORT).show();
                try { loadMyReservations(); } catch (Exception e) { Log.e(TAG, "reload", e); }
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private boolean hasReservationConflict(int roomId, long excludedId, long start, long end) {
        try (Cursor reservations = getContentResolver().query(MeetingProvider.URI_RES,
                new String[]{DBHelper.RES_ID, DBHelper.RES_START, DBHelper.RES_END},
                DBHelper.RES_ROOM_ID + "=? AND " + DBHelper.RES_STATUS + " IN (0,1)",
                new String[]{String.valueOf(roomId)}, null)) {
            if (reservations == null) return true;
            while (reservations.moveToNext()) {
                if (reservations.getLong(0) != excludedId
                        && start < reservations.getLong(2) && end > reservations.getLong(1)) return true;
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "conflict check failed", e);
            return true;
        }
    }

    private void cancelReservation(long resId) {
        new AlertDialog.Builder(this)
                .setTitle("取消预约")
                .setMessage("确定要取消此预约吗？")
                .setPositiveButton("确定", (d, w) -> {
                    ContentValues cv = new ContentValues();
                    cv.put(DBHelper.RES_STATUS, 3);
                    int updated = getContentResolver().update(Uri.withAppendedPath(MeetingProvider.URI_RES, String.valueOf(resId)),
                            cv, null, null);
                    if (updated == 0) {
                        Toast.makeText(this, "取消失败，请重试", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    AlarmUtil.cancelAlarm(this, resId);
                    Toast.makeText(this, "已取消预约", Toast.LENGTH_SHORT).show();
                    try { loadMyReservations(); } catch (Exception e) { Log.e(TAG, "reload after cancel", e); }
                })
                .setNegativeButton("返回", null).show();
    }

    class MyReservationAdapter extends RecyclerView.Adapter<MyReservationAdapter.Holder> {
        @Override public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(MyReservationsActivity.this)
                    .inflate(R.layout.item_my_reservation, parent, false));
        }
        @Override public void onBindViewHolder(Holder h, int pos) {
            if (pos < 0 || pos >= list.size()) return;
            MyReservation r = list.get(pos);
            if (h.tvTitle != null) h.tvTitle.setText(r.title != null ? r.title : "");
            if (h.tvRoom != null) h.tvRoom.setText(r.roomName != null ? r.roomName : "");
            if (h.tvTime != null) h.tvTime.setText((r.startTime != null ? r.startTime : "") + " ~ " + (r.endTime != null ? r.endTime : ""));
            if (h.tvAttendees != null) h.tvAttendees.setText(r.attendees + "人参会");
            int statusIdx = Math.min(r.status, STATUS_NAMES.length - 1);
            if (h.tvStatus != null) {
                h.tvStatus.setText(STATUS_NAMES[statusIdx]);
                h.tvStatus.setTextColor(STATUS_COLORS[statusIdx]);
            }
            if (h.tvNote != null) h.tvNote.setText(r.note != null && !r.note.isEmpty() ? "备注：" + r.note : "");
            if (h.btnEdit != null) h.btnEdit.setOnClickListener(v -> editReservation(r));
            if (h.btnCancel != null) h.btnCancel.setOnClickListener(v -> cancelReservation(r.resId));
            boolean canCancel = (r.status == 0 || r.status == 1);
            if (h.btnCancel != null) h.btnCancel.setVisibility(canCancel ? View.VISIBLE : View.GONE);
            if (h.btnEdit != null) h.btnEdit.setVisibility(r.status == 0 ? View.VISIBLE : View.GONE);
        }
        @Override public int getItemCount() { return list.size(); }

        class Holder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvRoom, tvTime, tvAttendees, tvStatus, tvNote;
            Button btnEdit, btnCancel;
            Holder(View v) {
                super(v);
                tvTitle = v.findViewById(R.id.tv_title);
                tvRoom = v.findViewById(R.id.tv_room);
                tvTime = v.findViewById(R.id.tv_time);
                tvAttendees = v.findViewById(R.id.tv_attendees);
                tvStatus = v.findViewById(R.id.tv_status);
                tvNote = v.findViewById(R.id.tv_note);
                btnEdit = v.findViewById(R.id.btn_edit);
                btnCancel = v.findViewById(R.id.btn_cancel);
            }
        }
    }

    static class MyReservation {
        long resId, startTimestamp, endTimestamp;
        int roomId, attendees, status;
        String roomName, title, startTime, endTime, note;
        MyReservation(long rid, int roomId, String rn, String t, int att, int st,
                      String sTime, String eTime, long sTs, long eTs, String n) {
            resId=rid; this.roomId=roomId; roomName=rn; title=t; attendees=att; status=st;
            startTime=sTime; endTime=eTime; startTimestamp=sTs; endTimestamp=eTs; note=n;
        }
    }
}

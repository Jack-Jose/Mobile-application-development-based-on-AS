package com.example.meeting;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BookingActivity extends AppCompatActivity {
    private ConflictCheckService conflictCheckService;
    private boolean isServiceBound = false;
    private Spinner spRoom, spRecur;
    private EditText etTitle, etAttendees, etDuration, etNote;
    private Button btnSelectTime, btnSubmit;
    private TextView tvSelectedTime, tvFeeDetail, tvSubStatus;
    private CheckBox cbSubscription;
    private long selectedStartTime = 0;
    private int currentUid, currentAdminLv;
    private long subExpiry = 0;
    private List<RoomItem> roomItems = new ArrayList<>();
    private List<Integer> roomIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        SharedPreferences prefs = getSharedPreferences("meeting_prefs", MODE_PRIVATE);
        currentUid = prefs.getInt("uid", 1);
        currentAdminLv = prefs.getInt("admin_level", 0);

        spRoom = findViewById(R.id.sp_room);
        spRecur = findViewById(R.id.sp_recur);
        etTitle = findViewById(R.id.et_title);
        etAttendees = findViewById(R.id.et_attendees);
        etDuration = findViewById(R.id.et_duration);
        etNote = findViewById(R.id.et_note);
        btnSelectTime = findViewById(R.id.btn_select_time);
        tvSelectedTime = findViewById(R.id.tv_selected_time);
        tvFeeDetail = findViewById(R.id.tv_fee_detail);
        cbSubscription = findViewById(R.id.cb_subscription);
        tvSubStatus = findViewById(R.id.tv_sub_status);
        btnSubmit = findViewById(R.id.btn_submit);

        loadRooms();
        loadSubStatus();

        String[] recurItems = {"不重复", "每天", "每周", "每月"};
        spRecur.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, recurItems));

        btnSelectTime.setOnClickListener(v -> showDateTimePicker());
        btnSubmit.setOnClickListener(v -> submitReservation());

        spRoom.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { refreshFee(); }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
        etDuration.setOnFocusChangeListener((v, hasFocus) -> { if (!hasFocus) refreshFee(); });
        // 勾选只预览折扣，不立即扣费订阅 —— 订阅在预约提交时一并处理
        cbSubscription.setOnCheckedChangeListener((b, checked) -> refreshFee());
    }

    private void loadSubStatus() {
        Cursor c = null;
        try {
            c = getContentResolver().query(MeetingProvider.URI_USER,
                    new String[]{DBHelper.USER_SUB_EXPIRY}, DBHelper.USER_ID + "=?",
                    new String[]{String.valueOf(currentUid)}, null);
            if (c != null && c.moveToFirst()) {
                subExpiry = c.getLong(0);
                boolean active = subExpiry > System.currentTimeMillis();
                if (tvSubStatus != null) {
                    if (active) {
                        tvSubStatus.setText("已订阅 (有效期至" +
                            new SimpleDateFormat("MM/dd", Locale.CHINA).format(new Date(subExpiry)) + ")");
                        tvSubStatus.setTextColor(0xFF00897B);
                    } else {
                        tvSubStatus.setText("未订阅 | 勾选下方复选框可在预约时一并开通 ¥99/月");
                        tvSubStatus.setTextColor(0xFF757575);
                        if (cbSubscription != null) cbSubscription.setChecked(false);
                    }
                }
            }
        } finally { if (c != null) c.close(); }
    }

    private void loadRooms() {
        Cursor c = getContentResolver().query(MeetingProvider.URI_ROOM,
                new String[]{DBHelper.ROOM_ID, DBHelper.ROOM_NAME, DBHelper.ROOM_CAP,
                        DBHelper.ROOM_BASE_PRICE, DBHelper.ROOM_DESKS, DBHelper.ROOM_CHAIRS, DBHelper.ROOM_STATUS},
                DBHelper.ROOM_STATUS + "=0", null, DBHelper.ROOM_NAME + " ASC");
        roomItems.clear(); roomIds.clear();
        List<String> names = new ArrayList<>();
        if (c != null) {
            while (c.moveToNext()) {
                int id = c.getInt(0);
                String name = c.getString(1);
                int cap = c.getInt(2);
                int basePrice = c.getInt(3);
                int desks = c.getInt(4);
                int chairs = c.getInt(5);
                roomIds.add(id);
                roomItems.add(new RoomItem(id, name, cap, basePrice, desks, chairs));
                names.add(name + " (" + cap + "人)");
            }
            c.close();
        }
        spRoom.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names));
    }

    private void refreshFee() {
        if (tvFeeDetail == null) return;
        int pos = spRoom.getSelectedItemPosition();
        if (pos < 0 || pos >= roomItems.size()) return;
        RoomItem room = roomItems.get(pos);
        String durStr = etDuration.getText().toString().trim();
        long dur = 0; try { dur = Long.parseLong(durStr); } catch (NumberFormatException ignored) {}
        if (dur <= 0) {
            tvFeeDetail.setText("请填写使用时长后自动计算费用\n容量:" + room.capacity + "人 | 椅子:" + room.chairs + "把 | 单价:" + room.basePrice);
            return;
        }
        int minDur = FeeCalculator.minDurationByCapacity(room.capacity);
        if (dur < minDur) {
            tvFeeDetail.setText("该会议室最低预约" + minDur + "分钟");
            return;
        }
        if (dur > FeeCalculator.MAX_DURATION_MIN) {
            tvFeeDetail.setText("最长预约6小时(360分钟)");
            return;
        }
        // 勾选即预览折扣价（不管当前是否已订阅，提交时才激活订阅）
        boolean wantSub = cbSubscription != null && cbSubscription.isChecked();
        boolean alreadySub = subExpiry > System.currentTimeMillis();
        boolean effectiveSub = wantSub || alreadySub;
        double fee = FeeCalculator.calculateFee(room.capacity, room.basePrice, dur, effectiveSub, room.name);
        double deposit = FeeCalculator.calculateDeposit(room.capacity);
        String label = FeeCalculator.getDiscountLabel(effectiveSub, room.name);

        StringBuilder sb = new StringBuilder();
        sb.append("费用预估\n");
        sb.append("容量×时段×单价: ").append(room.capacity).append("×")
          .append(FeeCalculator.getTierMultiplier(dur)).append("×").append(room.basePrice)
          .append(" = ¥").append(String.format("%.2f", fee))
          .append(" [").append(label).append("]\n");
        sb.append("押金: ¥").append(String.format("%.2f", deposit)).append(" (50×").append(room.capacity).append("人)\n");
        if (wantSub && !alreadySub) {
            sb.append("⚠ 预约提交时将一并开通订阅 (¥99/月)\n");
        }
        if (effectiveSub && FeeCalculator.isVipRoom(room.name)) {
            sb.append("[VIP: 额外服务全免]\n");
        }
        sb.append("营业时间: 6:00-23:30");
        tvFeeDetail.setText(sb.toString());
    }

    private void showDateTimePicker() {
        Calendar now = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            new TimePickerDialog(this, (v, hour, minute) -> {
                Calendar sel = Calendar.getInstance();
                sel.set(year, month, day, hour, minute, 0);
                sel.set(Calendar.MILLISECOND, 0);
                if (sel.getTimeInMillis() < System.currentTimeMillis()) {
                    Toast.makeText(this, "不能选择过去的时间", Toast.LENGTH_SHORT).show();
                    return;
                }
                selectedStartTime = sel.getTimeInMillis();
                tvSelectedTime.setText("已选：" + new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(sel.getTime()));
                refreshFee();
            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show();
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void submitReservation() {
        if (!isServiceBound) {
            Toast.makeText(this, "服务未就绪，请稍后再试", Toast.LENGTH_SHORT).show();
            return;
        }
        int pos = spRoom.getSelectedItemPosition();
        if (pos < 0 || pos >= roomItems.size()) {
            Toast.makeText(this, "请选择会议室", Toast.LENGTH_SHORT).show();
            return;
        }
        RoomItem room = roomItems.get(pos);
        int roomId = roomIds.get(pos);

        String title = etTitle.getText().toString().trim();
        if (title.isEmpty()) { Toast.makeText(this, "请输入会议主题", Toast.LENGTH_SHORT).show(); return; }

        String attStr = etAttendees.getText().toString().trim();
        int attendees; try { attendees = Integer.parseInt(attStr); } catch (NumberFormatException e) { Toast.makeText(this, "请输入正确的参会人数", Toast.LENGTH_SHORT).show(); return; }
        if (attendees < 1) { Toast.makeText(this, "参会人数至少为1", Toast.LENGTH_SHORT).show(); return; }

        // 容量/椅子硬性检查
        String chairCheck = FeeCalculator.checkChairRedundancy(attendees, room.capacity, room.chairs);
        if (chairCheck != null && chairCheck.startsWith("BLOCK:")) {
            String nextRoom = findNextRoomByCapacity(attendees);
            String msg = chairCheck.substring(6);
            if (!nextRoom.isEmpty()) msg += "\n" + nextRoom;
            new AlertDialog.Builder(this)
                .setTitle("无法预约")
                .setMessage(msg)
                .setPositiveButton("知道了", null)
                .show();
            return;
        }

        String durStr = etDuration.getText().toString().trim();
        long dur; try { dur = Long.parseLong(durStr); } catch (NumberFormatException e) { Toast.makeText(this, "请输入正确的时长", Toast.LENGTH_SHORT).show(); return; }
        int minDur = FeeCalculator.minDurationByCapacity(room.capacity);
        if (dur < minDur) {
            Toast.makeText(this, "该会议室最低预约" + minDur + "分钟", Toast.LENGTH_SHORT).show();
            return;
        }
        if (dur > FeeCalculator.MAX_DURATION_MIN) {
            Toast.makeText(this, "最长预约6小时", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedStartTime == 0) {
            Toast.makeText(this, "请选择预约时间", Toast.LENGTH_SHORT).show();
            return;
        }

        long endTime = selectedStartTime + dur * 60 * 1000L;
        String note = etNote.getText().toString().trim();

        String hourErr = FeeCalculator.checkBusinessHours(selectedStartTime, endTime);
        if (hourErr != null) {
            Toast.makeText(this, hourErr, Toast.LENGTH_SHORT).show();
            return;
        }

        // 确认订阅（如果有勾选且未订阅）
        final int finalAtt = attendees;
        final long finalDur = dur;
        final RoomItem finalRoom = room;
        final int finalRoomId = roomId;
        final String finalTitle = title;
        final String finalNote = note;
        final long finalStart = selectedStartTime;
        final long finalEnd = endTime;
        boolean wantSub = cbSubscription.isChecked();
        boolean alreadySub = subExpiry > System.currentTimeMillis();
        if (wantSub && !alreadySub) {
            // 弹确认框：订阅¥99 + 预约费用一并处理
            final double previewFee = FeeCalculator.calculateFee(room.capacity, room.basePrice, dur, true, room.name);
            double deposit = FeeCalculator.calculateDeposit(room.capacity);
            new AlertDialog.Builder(this)
                .setTitle("确认预约并订阅")
                .setMessage("本次预约将一并开通订阅服务：\n\n"
                    + "订阅费：¥99.00/月\n"
                    + "预约费：" + FeeCalculator.getDiscountLabel(true, room.name)
                    + " ¥" + String.format("%.2f", previewFee) + "\n"
                    + "押金：¥" + String.format("%.2f", deposit) + "\n\n"
                    + "合计需付：¥" + String.format("%.2f", previewFee + 99.0)
                    + " + 押金 ¥" + String.format("%.2f", deposit))
                .setPositiveButton("确认", (d, w) -> doSubmit(finalRoom, finalRoomId, finalTitle, finalAtt, finalNote, finalStart, finalEnd, finalDur))
                .setNegativeButton("取消", null)
                .show();
            return;
        }

        doSubmit(finalRoom, finalRoomId, finalTitle, finalAtt, finalNote, finalStart, finalEnd, finalDur);
    }

    private void doSubmit(RoomItem room, int roomId, String title, int attendees, String note,
                          long start, long end, long dur) {
        int conflict = conflictCheckService.binder.checkConflict(roomId, start, end, currentAdminLv);
        switch (conflict) {
            case 0:
                doInsert(room, roomId, title, attendees, note, start, end, dur);
                break;
            case 1:
                if (currentAdminLv >= 2) {
                    conflictCheckService.binder.deleteNormalUserConflicts(roomId, start, end);
                    doInsert(room, roomId, title, attendees, note, start, end, dur);
                    Toast.makeText(this, "已覆盖普通用户预约", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "该时段已被预约", Toast.LENGTH_SHORT).show();
                }
                break;
            case 2:
                Toast.makeText(this, "该时段已被高级管理员预约", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void doInsert(RoomItem room, int roomId, String title, int attendees, String note,
                          long start, long end, long dur) {
        boolean wantSub = cbSubscription.isChecked();
        boolean alreadySub = subExpiry > System.currentTimeMillis();
        // 只要勾选了就算订阅（已在submitReservation中确认过）
        boolean effectiveSub = wantSub || alreadySub;

        // 如果需要新开订阅，在预约提交时激活
        if (wantSub && !alreadySub) {
            long newExpiry = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000;
            ContentValues ucv = new ContentValues();
            ucv.put(DBHelper.USER_SUB_EXPIRY, newExpiry);
            getContentResolver().update(MeetingProvider.URI_USER, ucv,
                DBHelper.USER_ID + "=?", new String[]{String.valueOf(currentUid)});
            subExpiry = newExpiry;
        }

        double fee = FeeCalculator.calculateFee(room.capacity, room.basePrice, dur, effectiveSub, room.name);
        double deposit = FeeCalculator.calculateDeposit(room.capacity);
        int recur = spRecur.getSelectedItemPosition();

        int status = currentAdminLv >= 1 ? 1 : 0;

        int times = 1;
        long interval = 0;
        if (recur == 1) { times = 7; interval = 24 * 60 * 60 * 1000L; }
        else if (recur == 2) { times = 4; interval = 7 * 24 * 60 * 60 * 1000L; }
        else if (recur == 3) { times = 4; interval = 30L * 24 * 60 * 60 * 1000L; }

        for (int i = 0; i < times; i++) {
            ContentValues cv = new ContentValues();
            cv.put(DBHelper.RES_ROOM_ID, roomId);
            cv.put(DBHelper.RES_USER_ID, currentUid);
            cv.put(DBHelper.RES_START, start + i * interval);
            cv.put(DBHelper.RES_END, end + i * interval);
            cv.put(DBHelper.RES_TITLE, title);
            cv.put(DBHelper.RES_ATTENDEES, attendees);
            cv.put(DBHelper.RES_STATUS, status);
            cv.put(DBHelper.RES_RECUR, recur);
            cv.put(DBHelper.RES_NOTE, note);
            cv.put(DBHelper.RES_TOTAL_FEE, fee);
            cv.put(DBHelper.RES_DEPOSIT, deposit);
            cv.put(DBHelper.RES_DEPOSIT_STATUS, 1);
            cv.put(DBHelper.RES_IS_SUB, effectiveSub ? 1 : 0);
            cv.put(DBHelper.RES_DISCOUNT, effectiveSub ?
                (FeeCalculator.isVipRoom(room.name) ? FeeCalculator.VIP_SUBSCRIPTION_DISCOUNT_PCT : FeeCalculator.SUBSCRIPTION_DISCOUNT_PCT) : 0);
            cv.put(DBHelper.RES_PAID, currentAdminLv >= 1 ? 1 : 0);
            getContentResolver().insert(MeetingProvider.URI_RES, cv);
        }

        String discLabel = FeeCalculator.getDiscountLabel(effectiveSub, room.name);
        String payNote = currentAdminLv >= 1 ? " (已支付)" : " (待审批支付)";
        String subNote = (wantSub && !alreadySub) ? " +订阅¥99" : "";
        String summary = String.format("费用¥%.2f | 押金¥%.2f | %s%s%s", fee, deposit, discLabel, subNote, payNote);
        Toast.makeText(this, "预约成功！" + summary + (times > 1 ? " 共" + times + "条" : ""), Toast.LENGTH_LONG).show();
        AlarmUtil.setAlarm(this, start - 10 * 60 * 1000, "会议《" + title + "》即将开始");
        finish();
    }

    private String findNextRoomByCapacity(int attendees) {
        int currentPos = spRoom.getSelectedItemPosition();
        if (currentPos < 0 || currentPos >= roomItems.size()) return "";
        int currentCap = roomItems.get(currentPos).capacity;
        for (RoomItem r : roomItems) {
            if (r.capacity >= attendees && r.capacity > currentCap) {
                return "建议使用: " + r.name + " (容量" + r.capacity + "人)";
            }
        }
        return "没有更大会议室可用";
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder service) {
            conflictCheckService = ((ConflictCheckService.MyBinder) service).getService();
            isServiceBound = true;
        }
        @Override public void onServiceDisconnected(ComponentName name) {
            conflictCheckService = null; isServiceBound = false;
        }
    };

    @Override protected void onStart() { super.onStart(); bindService(new Intent(this, ConflictCheckService.class), serviceConnection, BIND_AUTO_CREATE); }
    @Override protected void onStop() { super.onStop(); if (isServiceBound) { unbindService(serviceConnection); isServiceBound = false; } }

    static class RoomItem {
        int id, capacity, basePrice, desks, chairs; String name;
        RoomItem(int i, String n, int c, int bp, int dk, int ch) {
            id=i; name=n; capacity=c; basePrice=bp; desks=dk; chairs=ch;
        }
    }
}

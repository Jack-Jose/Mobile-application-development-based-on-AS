package com.example.meeting;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class RoomManagementActivity extends AppCompatActivity {
    private RecyclerView rvRooms;
    private RoomAdapter adapter;
    private List<RoomInfo> roomList = new ArrayList<>();
    private Button btnAddRoom;
    private int currentAdminLv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_management);

        SharedPreferences prefs = getSharedPreferences("meeting_prefs", MODE_PRIVATE);
        currentAdminLv = prefs.getInt("admin_level", 0);

        rvRooms = findViewById(R.id.rv_rooms);
        btnAddRoom = findViewById(R.id.btn_add_room);
        rvRooms.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RoomAdapter();
        rvRooms.setAdapter(adapter);

        // 中级管理(2)及以上可增删会议室
        if (currentAdminLv >= 2) {
            btnAddRoom.setVisibility(View.VISIBLE);
            btnAddRoom.setOnClickListener(v -> showRoomDialog(null));
        } else {
            btnAddRoom.setVisibility(View.GONE);
        }

        loadRooms();
    }

    private void loadRooms() {
        roomList.clear();
        Cursor c = getContentResolver().query(MeetingProvider.URI_ROOM,
                null, null, null, DBHelper.ROOM_NAME + " ASC");
        if (c != null) {
            while (c.moveToNext()) {
                roomList.add(new RoomInfo(
                        c.getInt(c.getColumnIndexOrThrow(DBHelper.ROOM_ID)),
                        c.getString(c.getColumnIndexOrThrow(DBHelper.ROOM_NAME)),
                        c.getInt(c.getColumnIndexOrThrow(DBHelper.ROOM_CAP)),
                        c.getString(c.getColumnIndexOrThrow(DBHelper.ROOM_EQ)),
                        c.getString(c.getColumnIndexOrThrow(DBHelper.ROOM_LOCATION)),
                        c.getString(c.getColumnIndexOrThrow(DBHelper.ROOM_FLOOR)),
                        c.getInt(c.getColumnIndexOrThrow(DBHelper.ROOM_STATUS)),
                        c.getInt(c.getColumnIndexOrThrow(DBHelper.ROOM_DESKS)),
                        c.getInt(c.getColumnIndexOrThrow(DBHelper.ROOM_CHAIRS)),
                        c.getInt(c.getColumnIndexOrThrow(DBHelper.ROOM_BASE_PRICE))
                ));
            }
            c.close();
            adapter.notifyDataSetChanged();
        }
    }

    private void showRoomDialog(RoomInfo room) {
        View dv = LayoutInflater.from(this).inflate(R.layout.dialog_room_edit, null);
        EditText etName = dv.findViewById(R.id.et_room_name);
        EditText etCap = dv.findViewById(R.id.et_room_cap);
        EditText etEquip = dv.findViewById(R.id.et_room_equip);
        EditText etLocation = dv.findViewById(R.id.et_room_location);
        EditText etFloor = dv.findViewById(R.id.et_room_floor);
        EditText etDesks = dv.findViewById(R.id.et_room_desks);
        EditText etChairs = dv.findViewById(R.id.et_room_chairs);
        EditText etBasePrice = dv.findViewById(R.id.et_room_base_price);

        boolean isEdit = room != null;
        if (isEdit) {
            etName.setText(room.name);
            etCap.setText(String.valueOf(room.capacity));
            etEquip.setText(room.equip);
            etLocation.setText(room.location);
            etFloor.setText(room.floor);
            etDesks.setText(String.valueOf(room.desks));
            etChairs.setText(String.valueOf(room.chairs));
            etBasePrice.setText(String.valueOf(room.basePrice));
        }

        new AlertDialog.Builder(this)
                .setTitle(isEdit ? "编辑会议室" : "添加会议室")
                .setView(dv)
                .setPositiveButton("保存", (d, w) -> {
                    ContentValues cv = new ContentValues();
                    cv.put(DBHelper.ROOM_NAME, etName.getText().toString().trim());
                    cv.put(DBHelper.ROOM_CAP, Integer.parseInt(etCap.getText().toString().trim()));
                    cv.put(DBHelper.ROOM_EQ, etEquip.getText().toString().trim());
                    cv.put(DBHelper.ROOM_LOCATION, etLocation.getText().toString().trim());
                    cv.put(DBHelper.ROOM_FLOOR, etFloor.getText().toString().trim());
                    cv.put(DBHelper.ROOM_DESKS, Integer.parseInt(etDesks.getText().toString().trim()));
                    cv.put(DBHelper.ROOM_CHAIRS, Integer.parseInt(etChairs.getText().toString().trim()));
                    cv.put(DBHelper.ROOM_BASE_PRICE, Integer.parseInt(etBasePrice.getText().toString().trim()));
                    cv.put(DBHelper.ROOM_DEPOSIT_RATE, 50);

                    if (isEdit) {
                        getContentResolver().update(MeetingProvider.URI_ROOM, cv,
                                DBHelper.ROOM_ID + "=?", new String[]{String.valueOf(room.id)});
                    } else {
                        getContentResolver().insert(MeetingProvider.URI_ROOM, cv);
                    }
                    Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
                    loadRooms();
                })
                .setNegativeButton("取消", null).show();
    }

    private void deleteRoom(int id) {
        new AlertDialog.Builder(this)
                .setTitle("删除会议室")
                .setMessage("确定删除该会议室吗？相关预约也将被删除。")
                .setPositiveButton("删除", (d, w) -> {
                    getContentResolver().delete(MeetingProvider.URI_ROOM,
                            DBHelper.ROOM_ID + "=?", new String[]{String.valueOf(id)});
                    getContentResolver().delete(MeetingProvider.URI_RES,
                            DBHelper.RES_ROOM_ID + "=?", new String[]{String.valueOf(id)});
                    Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                    loadRooms();
                }).setNegativeButton("取消", null).show();
    }

    class RoomAdapter extends RecyclerView.Adapter<RoomAdapter.Holder> {
        @Override public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(RoomManagementActivity.this)
                    .inflate(R.layout.item_room, parent, false));
        }
        @Override public void onBindViewHolder(Holder h, int pos) {
            RoomInfo r = roomList.get(pos);
            h.tvName.setText(r.name);
            h.tvDetail.setText("容量: " + r.capacity + "人 | 桌" + r.desks + "椅" + r.chairs + " | ¥" + r.basePrice + "/人 | " + r.equip);
            h.tvLocation.setText(r.location + " " + r.floor);
            h.tvStatus.setText(r.status == 0 ? "✅可用" : "🔧维护中");
            h.tvStatus.setTextColor(r.status == 0 ? 0xFF4CAF50 : 0xFFFF9800);
            h.itemView.setOnClickListener(v -> {
                if (currentAdminLv >= 2) showRoomDialog(r);
            });
            h.itemView.setOnLongClickListener(v -> {
                if (currentAdminLv >= 3) { deleteRoom(r.id); return true; }
                return false;
            });
        }
        @Override public int getItemCount() { return roomList.size(); }
        class Holder extends RecyclerView.ViewHolder {
            TextView tvName, tvDetail, tvLocation, tvStatus;
            Holder(View v) {
                super(v);
                tvName = v.findViewById(R.id.tv_room_name);
                tvDetail = v.findViewById(R.id.tv_room_detail);
                tvLocation = v.findViewById(R.id.tv_room_location);
                tvStatus = v.findViewById(R.id.tv_room_status);
            }
        }
    }

    static class RoomInfo {
        int id, capacity, status, desks, chairs, basePrice;
        String name, equip, location, floor;
        RoomInfo(int id, String name, int cap, String eq, String loc, String fl, int st, int dk, int ch, int bp) {
            this.id=id; this.name=name; this.capacity=cap; this.equip=eq; this.location=loc;
            this.floor=fl; this.status=st; this.desks=dk; this.chairs=ch; this.basePrice=bp;
        }
    }
}
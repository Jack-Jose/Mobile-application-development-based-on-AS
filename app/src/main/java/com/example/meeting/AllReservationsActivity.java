package com.example.meeting;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AllReservationsActivity extends AppCompatActivity {
    private RecyclerView rvAllReservations;
    private AllReservationAdapter adapter;
    private List<AllReservation> list = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);
    private final String[] STATUS_NAMES = {"待审批","已通过","已拒绝","已取消","已完成"};
    private final int[] STATUS_COLORS = {0xFFFF9800,0xFF4CAF50,0xFFF44336,0xFF9E9E9E,0xFF2196F3};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_reservations);

        rvAllReservations = findViewById(R.id.recyclerAllReservations);
        rvAllReservations.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AllReservationAdapter();
        rvAllReservations.setAdapter(adapter);

        loadAllReservations();
    }

    private void loadAllReservations() {
        list.clear();
        Cursor c = getContentResolver().query(MeetingProvider.URI_RES,
                null, null, null, DBHelper.RES_START + " DESC");
        if (c != null) {
            while (c.moveToNext()) {
                int roomId = c.getInt(c.getColumnIndexOrThrow(DBHelper.RES_ROOM_ID));
                int userId = c.getInt(c.getColumnIndexOrThrow(DBHelper.RES_USER_ID));
                int status = c.getInt(c.getColumnIndexOrThrow(DBHelper.RES_STATUS));
                long start = c.getLong(c.getColumnIndexOrThrow(DBHelper.RES_START));
                long end = c.getLong(c.getColumnIndexOrThrow(DBHelper.RES_END));
                String title = c.getString(c.getColumnIndexOrThrow(DBHelper.RES_TITLE));

                String userName = "未知";
                Cursor uc = getContentResolver().query(MeetingProvider.URI_USER,
                        new String[]{DBHelper.USER_NAME}, DBHelper.USER_ID + "=?",
                        new String[]{String.valueOf(userId)}, null);
                if (uc != null && uc.moveToFirst()) userName = uc.getString(0);
                if (uc != null) uc.close();

                String roomName = "会议室" + roomId;
                Cursor rc = getContentResolver().query(MeetingProvider.URI_ROOM,
                        new String[]{DBHelper.ROOM_NAME}, DBHelper.ROOM_ID + "=?",
                        new String[]{String.valueOf(roomId)}, null);
                if (rc != null && rc.moveToFirst()) roomName = rc.getString(0);
                if (rc != null) rc.close();

                list.add(new AllReservation(roomName, userName, title, status,
                        dateFormat.format(new Date(start)),
                        dateFormat.format(new Date(end))));
            }
            c.close();
            adapter.notifyDataSetChanged();
        }
    }

    class AllReservationAdapter extends RecyclerView.Adapter<AllReservationAdapter.Holder> {
        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(AllReservationsActivity.this)
                    .inflate(R.layout.item_all_reservation, parent, false);
            return new Holder(v);
        }
        @Override
        public void onBindViewHolder(Holder holder, int position) {
            AllReservation r = list.get(position);
            holder.tvTitle.setText(r.title);
            holder.tvRoom.setText(r.roomName);
            holder.tvUser.setText("预约人：" + r.userName);
            holder.tvTime.setText(r.startTime + " ~ " + r.endTime);
            holder.tvStatus.setText(STATUS_NAMES[r.status]);
            holder.tvStatus.setTextColor(STATUS_COLORS[r.status]);
        }
        @Override
        public int getItemCount() { return list.size(); }

        class Holder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvRoom, tvUser, tvTime, tvStatus;
            Holder(View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tv_all_title);
                tvRoom = itemView.findViewById(R.id.tv_room);
                tvUser = itemView.findViewById(R.id.tv_user);
                tvTime = itemView.findViewById(R.id.tv_time);
                tvStatus = itemView.findViewById(R.id.tv_all_status);
            }
        }
    }

    static class AllReservation {
        String roomName, userName, title, startTime, endTime;
        int status;
        AllReservation(String rn, String un, String t, int s, String st, String et) {
            roomName=rn; userName=un; title=t; status=s; startTime=st; endTime=et;
        }
    }
}
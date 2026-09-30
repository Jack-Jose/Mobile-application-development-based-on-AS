package com.example.meeting;

import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import androidx.annotation.Nullable;

public class ConflictCheckService extends Service {
    public final MyBinder binder = new MyBinder();

    public class MyBinder extends Binder {
        public ConflictCheckService getService() {
            return ConflictCheckService.this;
        }

        // 检查冲突：0=无冲突，1=普通用户冲突(可覆盖)，2=管理员冲突(不可覆盖)
        public int checkConflict(int roomId, long start, long end, int currentRole) {
            if (roomId <= 0 || start < 0 || end < start) return 2;

            ContentResolver cr = getContentResolver();
            Cursor c = null;
            try {
                // 只查询有效预约(待审批0/已通过1)，排除已取消3/已拒绝2/已完成4
                c = cr.query(
                        MeetingProvider.URI_RES,
                        null,
                        DBHelper.RES_ROOM_ID + "=? AND " + DBHelper.RES_STATUS + " IN (0,1)",
                        new String[]{String.valueOf(roomId)},
                        null
                );

                if (c != null && c.moveToFirst()) {
                    do {
                        long s = c.getLong(c.getColumnIndexOrThrow(DBHelper.RES_START));
                        long e = c.getLong(c.getColumnIndexOrThrow(DBHelper.RES_END));
                        int userId = c.getInt(c.getColumnIndexOrThrow(DBHelper.RES_USER_ID));

                        if (!(end <= s || start >= e)) {
                            Cursor userCursor = cr.query(
                                    MeetingProvider.URI_USER,
                                    new String[]{DBHelper.USER_ROLE},
                                    DBHelper.USER_ID + "=?",
                                    new String[]{String.valueOf(userId)},
                                    null
                            );

                            int conflictRole = 0;
                            if (userCursor != null) {
                                if (userCursor.moveToFirst()) {
                                    conflictRole = userCursor.getInt(0);
                                }
                                userCursor.close();
                            }
                            // role>=1 为管理员/超管，不可覆盖
                            return conflictRole >= 1 ? 2 : 1;
                        }
                    } while (c.moveToNext());
                }
            } catch (Exception e) {
                e.printStackTrace();
                return 2;
            } finally {
                if (c != null) c.close();
            }
            return 0;
        }

        // 删除普通用户的冲突预约（管理员预约时覆盖）
        public void deleteNormalUserConflicts(int roomId, long start, long end) {
            ContentResolver cr = getContentResolver();
            Cursor c = null;
            try {
                c = cr.query(
                        MeetingProvider.URI_RES,
                        new String[]{DBHelper.RES_ID, DBHelper.RES_USER_ID, DBHelper.RES_START, DBHelper.RES_END},
                        DBHelper.RES_ROOM_ID + "=? AND " + DBHelper.RES_STATUS + " IN (0,1)",
                        new String[]{String.valueOf(roomId)},
                        null
                );

                if (c != null && c.moveToFirst()) {
                    do {
                        long s = c.getLong(2);
                        long e = c.getLong(3);
                        if (end <= s || start >= e) continue;

                        long resId = c.getLong(0);
                        int userId = c.getInt(1);

                        Cursor userCursor = cr.query(
                                MeetingProvider.URI_USER,
                                new String[]{DBHelper.USER_ROLE},
                                DBHelper.USER_ID + "=?",
                                new String[]{String.valueOf(userId)},
                                null
                        );

                        int role = 0;
                        if (userCursor != null) {
                            if (userCursor.moveToFirst()) {
                                role = userCursor.getInt(0);
                            }
                            userCursor.close();
                        }

                        if (role == 0) {
                            cr.delete(
                                    Uri.withAppendedPath(MeetingProvider.URI_RES, String.valueOf(resId)),
                                    null, null
                            );
                        }
                    } while (c.moveToNext());
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (c != null) c.close();
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}
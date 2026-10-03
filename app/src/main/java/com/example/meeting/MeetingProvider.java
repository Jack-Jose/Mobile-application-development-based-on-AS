package com.example.meeting;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MeetingProvider extends ContentProvider {
    private static final String TAG = "MeetingProvider";
    public static final String AUTHORITY = "com.example.meeting.provider";
    public static final Uri URI_RES = Uri.parse("content://" + AUTHORITY + "/reservations");
    public static final Uri URI_USER = Uri.parse("content://" + AUTHORITY + "/users");
    public static final Uri URI_USER_REG = Uri.parse("content://" + AUTHORITY + "/users/register");
    public static final Uri URI_ROOM = Uri.parse("content://" + AUTHORITY + "/rooms");
    public static final Uri URI_NOTICE = Uri.parse("content://" + AUTHORITY + "/notices");
    public static final Uri URI_SERVICE = Uri.parse("content://" + AUTHORITY + "/services");
    public static final Uri URI_RES_SVC = Uri.parse("content://" + AUTHORITY + "/res_services");

    private static final int RESERVATIONS = 1;
    private static final int RESERVATION_ITEM = 2;
    private static final int USERS = 3;
    private static final int USERS_REG = 4;
    private static final int ROOMS = 5;
    private static final int ROOM_ITEM = 6;
    private static final int NOTICES = 7;
    private static final int SERVICES = 8;
    private static final int SERVICE_ITEM = 9;
    private static final int RES_SERVICES = 10;
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        uriMatcher.addURI(AUTHORITY, "reservations", RESERVATIONS);
        uriMatcher.addURI(AUTHORITY, "reservations/#", RESERVATION_ITEM);
        uriMatcher.addURI(AUTHORITY, "users", USERS);
        uriMatcher.addURI(AUTHORITY, "users/register", USERS_REG);
        uriMatcher.addURI(AUTHORITY, "rooms", ROOMS);
        uriMatcher.addURI(AUTHORITY, "rooms/#", ROOM_ITEM);
        uriMatcher.addURI(AUTHORITY, "notices", NOTICES);
        uriMatcher.addURI(AUTHORITY, "services", SERVICES);
        uriMatcher.addURI(AUTHORITY, "services/#", SERVICE_ITEM);
        uriMatcher.addURI(AUTHORITY, "res_services", RES_SERVICES);
    }

    private DBHelper dbHelper;
    private volatile SQLiteDatabase db;
    private final CountDownLatch dbReady = new CountDownLatch(1);

    @Override
    public boolean onCreate() {
        if (getContext() == null) return false;
        // Initialize DB on background thread to avoid ANR
        new Thread(() -> {
            try {
                dbHelper = new DBHelper(getContext());
                db = dbHelper.getWritableDatabase();
                Log.d(TAG, "Database initialized successfully");
            } catch (Exception e) {
                Log.e(TAG, "Database init failed", e);
            } finally {
                dbReady.countDown();
            }
        }, "DB-Init").start();
        return true;
    }

    private SQLiteDatabase getDb() {
        if (db != null) return db;
        // Wait for DB init with timeout
        try {
            if (dbReady.await(3, TimeUnit.SECONDS)) {
                return db;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        Log.e(TAG, "Database not ready after timeout");
        return null;
    }

    @Nullable @Override
    public Cursor query(@NonNull Uri uri, String[] p, String sel, String[] sArgs, String sort) {
        SQLiteDatabase database = getDb();
        if (database == null) return null;
        try {
            switch (uriMatcher.match(uri)) {
                case RESERVATIONS: return database.query(DBHelper.TABLE_RES, p, sel, sArgs, null, null, sort);
                case RESERVATION_ITEM: return database.query(DBHelper.TABLE_RES, p, DBHelper.RES_ID + "=?", new String[]{uri.getLastPathSegment()}, null, null, sort);
                case USERS: return database.query(DBHelper.TABLE_USER, p, sel, sArgs, null, null, sort);
                case ROOMS: return database.query(DBHelper.TABLE_ROOM, p, sel, sArgs, null, null, sort);
                case ROOM_ITEM: return database.query(DBHelper.TABLE_ROOM, p, DBHelper.ROOM_ID + "=?", new String[]{uri.getLastPathSegment()}, null, null, sort);
                case NOTICES: return database.query(DBHelper.TABLE_NOTICE, p, sel, sArgs, null, null, sort);
                case SERVICES: return database.query(DBHelper.TABLE_SERVICE, p, sel, sArgs, null, null, sort);
                case SERVICE_ITEM: return database.query(DBHelper.TABLE_SERVICE, p, DBHelper.SVC_ID + "=?", new String[]{uri.getLastPathSegment()}, null, null, sort);
                case RES_SERVICES: return database.query(DBHelper.TABLE_RES_SVC, p, sel, sArgs, null, null, sort);
                default: throw new IllegalArgumentException("Unknown URI: " + uri);
            }
        } catch (Exception e) {
            Log.e(TAG, "Query failed: " + uri, e);
            return null;
        }
    }

    @Nullable @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        SQLiteDatabase database = getDb();
        if (database == null || values == null) return null;
        long id;
        Uri base;
        try {
            switch (uriMatcher.match(uri)) {
                case RESERVATIONS: id = database.insert(DBHelper.TABLE_RES, null, values); base = URI_RES; break;
                case USERS_REG:
                    Cursor c = database.query(DBHelper.TABLE_USER, null, DBHelper.USER_NAME + "=?",
                            new String[]{values.getAsString(DBHelper.USER_NAME)}, null, null, null);
                    if (c != null && c.getCount() > 0) { c.close(); throw new IllegalArgumentException("用户名已存在"); }
                    if (c != null) c.close();
                    id = database.insert(DBHelper.TABLE_USER, null, values); base = URI_USER; break;
                case ROOMS: id = database.insert(DBHelper.TABLE_ROOM, null, values); base = URI_ROOM; break;
                case NOTICES: id = database.insert(DBHelper.TABLE_NOTICE, null, values); base = URI_NOTICE; break;
                case SERVICES: id = database.insert(DBHelper.TABLE_SERVICE, null, values); base = URI_SERVICE; break;
                case RES_SERVICES: id = database.insert(DBHelper.TABLE_RES_SVC, null, values); base = URI_RES_SVC; break;
                default: throw new IllegalArgumentException("Unknown URI: " + uri);
            }
        } catch (Exception e) {
            Log.e(TAG, "Insert failed: " + uri, e);
            return null;
        }
        if (id < 0) {
            Log.e(TAG, "Insert returned no row: " + uri);
            return null;
        }
        if (getContext() != null) getContext().getContentResolver().notifyChange(uri, null);
        return Uri.withAppendedPath(base, String.valueOf(id));
    }

    @Override
    public int delete(@NonNull Uri uri, String sel, String[] sArgs) {
        SQLiteDatabase database = getDb();
        if (database == null) return 0;
        int count;
        try {
            switch (uriMatcher.match(uri)) {
                case RESERVATIONS: count = database.delete(DBHelper.TABLE_RES, sel, sArgs); break;
                case RESERVATION_ITEM: count = database.delete(DBHelper.TABLE_RES, DBHelper.RES_ID + "=?", new String[]{uri.getLastPathSegment()}); break;
                case ROOMS: count = database.delete(DBHelper.TABLE_ROOM, sel, sArgs); break;
                case ROOM_ITEM: count = database.delete(DBHelper.TABLE_ROOM, DBHelper.ROOM_ID + "=?", new String[]{uri.getLastPathSegment()}); break;
                case SERVICES: count = database.delete(DBHelper.TABLE_SERVICE, sel, sArgs); break;
                case SERVICE_ITEM: count = database.delete(DBHelper.TABLE_SERVICE, DBHelper.SVC_ID + "=?", new String[]{uri.getLastPathSegment()}); break;
                case RES_SERVICES: count = database.delete(DBHelper.TABLE_RES_SVC, sel, sArgs); break;
                default: throw new IllegalArgumentException("Unknown URI: " + uri);
            }
        } catch (Exception e) {
            Log.e(TAG, "Delete failed: " + uri, e);
            return 0;
        }
        if (getContext() != null) getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String sel, String[] sArgs) {
        SQLiteDatabase database = getDb();
        if (database == null || values == null) return 0;
        int count;
        try {
            switch (uriMatcher.match(uri)) {
                case RESERVATIONS: count = database.update(DBHelper.TABLE_RES, values, sel, sArgs); break;
                case RESERVATION_ITEM: count = database.update(DBHelper.TABLE_RES, values, DBHelper.RES_ID + "=?", new String[]{uri.getLastPathSegment()}); break;
                case ROOMS: count = database.update(DBHelper.TABLE_ROOM, values, sel, sArgs); break;
                case ROOM_ITEM: count = database.update(DBHelper.TABLE_ROOM, values, DBHelper.ROOM_ID + "=?", new String[]{uri.getLastPathSegment()}); break;
                case USERS: count = database.update(DBHelper.TABLE_USER, values, sel, sArgs); break;
                case SERVICES: count = database.update(DBHelper.TABLE_SERVICE, values, sel, sArgs); break;
                case SERVICE_ITEM: count = database.update(DBHelper.TABLE_SERVICE, values, DBHelper.SVC_ID + "=?", new String[]{uri.getLastPathSegment()}); break;
                default: throw new IllegalArgumentException("Unknown URI: " + uri);
            }
        } catch (Exception e) {
            Log.e(TAG, "Update failed: " + uri, e);
            return 0;
        }
        if (getContext() != null) getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Nullable @Override
    public String getType(@NonNull Uri uri) {
        switch (uriMatcher.match(uri)) {
            case RESERVATIONS: return "vnd.android.cursor.dir/vnd.com.example.meeting.reservations";
            case RESERVATION_ITEM: return "vnd.android.cursor.item/vnd.com.example.meeting.reservations";
            case USERS: case USERS_REG: return "vnd.android.cursor.dir/vnd.com.example.meeting.users";
            case ROOMS: return "vnd.android.cursor.dir/vnd.com.example.meeting.rooms";
            case ROOM_ITEM: return "vnd.android.cursor.item/vnd.com.example.meeting.rooms";
            case NOTICES: return "vnd.android.cursor.dir/vnd.com.example.meeting.notices";
            case SERVICES: return "vnd.android.cursor.dir/vnd.com.example.meeting.services";
            case SERVICE_ITEM: return "vnd.android.cursor.item/vnd.com.example.meeting.services";
            case RES_SERVICES: return "vnd.android.cursor.dir/vnd.com.example.meeting.res_services";
            default: return null;
        }
    }
}

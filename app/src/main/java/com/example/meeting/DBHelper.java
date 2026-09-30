package com.example.meeting;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    public static final String DB = "meeting.db";
    public static final int VERSION = 7;

    public static final String TABLE_ROOM = "meeting_rooms";
    public static final String ROOM_ID = "_id";
    public static final String ROOM_NAME = "name";
    public static final String ROOM_CAP = "capacity";
    public static final String ROOM_EQ = "equip";
    public static final String ROOM_LOCATION = "location";
    public static final String ROOM_FLOOR = "floor";
    public static final String ROOM_STATUS = "status";
    public static final String ROOM_DESKS = "desk_count";
    public static final String ROOM_CHAIRS = "chair_count";
    public static final String ROOM_BASE_PRICE = "base_price";
    public static final String ROOM_DEPOSIT_RATE = "deposit_rate";

    public static final String TABLE_USER = "users";
    public static final String USER_ID = "_id";
    public static final String USER_NAME = "uname";
    public static final String USER_PWD = "pwd";
    public static final String USER_ROLE = "role";
    public static final String USER_PHONE = "phone";
    public static final String USER_EMAIL = "email";
    public static final String USER_DEPT = "dept";
    public static final String USER_ADMIN_LV = "admin_level";
    public static final String USER_SUB_EXPIRY = "sub_expiry";

    public static final String TABLE_RES = "reservations";
    public static final String RES_ID = "_id";
    public static final String RES_ROOM_ID = "room_id";
    public static final String RES_USER_ID = "user_id";
    public static final String RES_START = "start_time";
    public static final String RES_END = "end_time";
    public static final String RES_TITLE = "title";
    public static final String RES_ATTENDEES = "attendees";
    public static final String RES_STATUS = "status";
    public static final String RES_RECUR = "recurrence";
    public static final String RES_NOTE = "note";
    public static final String RES_TOTAL_FEE = "total_fee";
    public static final String RES_DEPOSIT = "deposit_amount";
    public static final String RES_DEPOSIT_STATUS = "deposit_status";
    public static final String RES_IS_SUB = "is_subscription";
    public static final String RES_DISCOUNT = "discount_percent";
    public static final String RES_OVERTIME_MIN = "overtime_min";
    public static final String RES_OVERTIME_FEE = "overtime_fee";
    public static final String RES_SERVICE_FEE = "service_fee";
    public static final String RES_PAID = "is_paid";

    public static final String TABLE_NOTICE = "notices";
    public static final String NOTICE_ID = "_id";
    public static final String NOTICE_TITLE = "title";
    public static final String NOTICE_CONTENT = "content";
    public static final String NOTICE_TIME = "create_time";

    public static final String TABLE_SERVICE = "extra_services";
    public static final String SVC_ID = "_id";
    public static final String SVC_NAME = "name";
    public static final String SVC_MARKET_PRICE = "market_price";
    public static final String SVC_OUR_PRICE = "our_price";
    public static final String SVC_UNIT = "unit";
    public static final String SVC_ICON = "icon";

    public static final String TABLE_RES_SVC = "reservation_services";
    public static final String RSVC_ID = "_id";
    public static final String RSVC_RES_ID = "res_id";
    public static final String RSVC_SVC_ID = "svc_id";
    public static final String RSVC_QTY = "quantity";
    public static final String RSVC_PRICE = "price";

    public static final int ADMIN_USER = 0;
    public static final int ADMIN_JUNIOR = 1;
    public static final int ADMIN_MID = 2;
    public static final int ADMIN_SENIOR = 3;
    public static final int ADMIN_BOSS = 4;

    public static String adminName(int level) {
        switch (level) {
            case 4: return "Boss总管";
            case 3: return "高级管理";
            case 2: return "中级管理";
            case 1: return "初级管理";
            default: return "普通用户";
        }
    }

    public DBHelper(Context c) { super(c, DB, null, VERSION); }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_ROOM + " (" + ROOM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + ROOM_NAME + " TEXT NOT NULL," + ROOM_CAP + " INTEGER DEFAULT 10," + ROOM_EQ + " TEXT DEFAULT ''," + ROOM_LOCATION + " TEXT DEFAULT ''," + ROOM_FLOOR + " TEXT DEFAULT ''," + ROOM_STATUS + " INTEGER DEFAULT 0," + ROOM_DESKS + " INTEGER DEFAULT 6," + ROOM_CHAIRS + " INTEGER DEFAULT 8," + ROOM_BASE_PRICE + " INTEGER DEFAULT 18," + ROOM_DEPOSIT_RATE + " INTEGER DEFAULT 50)");
        db.execSQL("CREATE TABLE " + TABLE_USER + " (" + USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + USER_NAME + " TEXT UNIQUE," + USER_PWD + " TEXT NOT NULL," + USER_ROLE + " INTEGER DEFAULT 0," + USER_PHONE + " TEXT DEFAULT ''," + USER_EMAIL + " TEXT DEFAULT ''," + USER_DEPT + " TEXT DEFAULT ''," + USER_ADMIN_LV + " INTEGER DEFAULT 0," + USER_SUB_EXPIRY + " INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE " + TABLE_RES + " (" + RES_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + RES_ROOM_ID + " INTEGER," + RES_USER_ID + " INTEGER," + RES_START + " INTEGER," + RES_END + " INTEGER," + RES_TITLE + " TEXT DEFAULT ''," + RES_ATTENDEES + " INTEGER DEFAULT 1," + RES_STATUS + " INTEGER DEFAULT 0," + RES_RECUR + " INTEGER DEFAULT 0," + RES_NOTE + " TEXT DEFAULT ''," + RES_TOTAL_FEE + " REAL DEFAULT 0," + RES_DEPOSIT + " REAL DEFAULT 0," + RES_DEPOSIT_STATUS + " INTEGER DEFAULT 0," + RES_IS_SUB + " INTEGER DEFAULT 0," + RES_DISCOUNT + " INTEGER DEFAULT 0," + RES_OVERTIME_MIN + " INTEGER DEFAULT 0," + RES_OVERTIME_FEE + " REAL DEFAULT 0," + RES_SERVICE_FEE + " REAL DEFAULT 0," + RES_PAID + " INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE " + TABLE_NOTICE + " (" + NOTICE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + NOTICE_TITLE + " TEXT," + NOTICE_CONTENT + " TEXT," + NOTICE_TIME + " INTEGER)");
        db.execSQL("CREATE TABLE " + TABLE_SERVICE + " (" + SVC_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + SVC_NAME + " TEXT," + SVC_MARKET_PRICE + " REAL DEFAULT 0," + SVC_OUR_PRICE + " REAL DEFAULT 0," + SVC_UNIT + " TEXT DEFAULT ''," + SVC_ICON + " TEXT DEFAULT '')");
        db.execSQL("CREATE TABLE " + TABLE_RES_SVC + " (" + RSVC_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + RSVC_RES_ID + " INTEGER," + RSVC_SVC_ID + " INTEGER," + RSVC_QTY + " INTEGER DEFAULT 1," + RSVC_PRICE + " REAL DEFAULT 0)");
        insertUsers(db);
        insertDefaultRooms(db);
        insertDefaultServices(db);
        ContentValues n = new ContentValues();
        n.put(NOTICE_TITLE, "v7.0 营业时间+订阅+支付");
        n.put(NOTICE_CONTENT, "营业6:00-23:30 | 订阅99/月 | 审批后支付 | 桌椅可租");
        n.put(NOTICE_TIME, System.currentTimeMillis());
        db.insert(TABLE_NOTICE, null, n);
    }

    private void insertUsers(SQLiteDatabase db) {
        Object[][] uu = {{"boss","123456",4,4,"管理层","13800000000"},{"admin","123456",3,3,"技术部","13800000001"},{"admin2","123456",2,2,"行政部","13800000003"},{"admin3","123456",1,1,"前台","13800000004"},{"user001","123456",0,0,"市场部","13800000002"}};
        for (Object[] u : uu) {
            ContentValues cv = new ContentValues();
            cv.put(USER_NAME,(String)u[0]); cv.put(USER_PWD,(String)u[1]);
            cv.put(USER_ROLE,(Integer)u[2]); cv.put(USER_ADMIN_LV,(Integer)u[3]);
            cv.put(USER_DEPT,(String)u[4]); cv.put(USER_PHONE,(String)u[5]);
            db.insert(TABLE_USER,null,cv);
        }
    }

    private void insertDefaultRooms(SQLiteDatabase db) {
        Object[][] rr = {{"A101 小型会议室",6,"白板、投影仪",6,8,18,50},{"A201 中型会议室",15,"投影仪、视频会议设备、白板",15,18,20,50},{"A301 大型会议室",30,"投影仪、视频会议设备、音响系统、白板",30,35,21,50},{"B101 洽谈室",4,"白板、饮水机",4,6,15,50},{"B201 培训室",50,"投影仪、音响系统、讲台、白板",50,55,20,50},{"C101 VIP接待室",8,"投影仪、视频会议设备、茶水服务",8,10,25,50}};
        for (Object[] r : rr) {
            ContentValues cv = new ContentValues();
            cv.put(ROOM_NAME,(String)r[0]); cv.put(ROOM_CAP,(Integer)r[1]);
            cv.put(ROOM_EQ,(String)r[2]); cv.put(ROOM_DESKS,(Integer)r[3]);
            cv.put(ROOM_CHAIRS,(Integer)r[4]); cv.put(ROOM_BASE_PRICE,(Integer)r[5]);
            cv.put(ROOM_DEPOSIT_RATE,(Integer)r[6]); cv.put(ROOM_STATUS,0);
            db.insert(TABLE_ROOM,null,cv);
        }
    }

    private void insertDefaultServices(SQLiteDatabase db) {
        Object[][] ss = {{"饮水机（桶装水）",30.0,"桶","dh"},{"茶水服务（含茶具）",50.0,"套","ch"},{"投影仪额外使用",80.0,"次","ty"},{"白板笔套装",15.0,"套","bb"},{"会议记录服务",100.0,"次","jl"},{"同声传译设备",200.0,"套","cy"},{"鲜花布置",120.0,"次","xh"},{"名牌/席卡打印",10.0,"张","mp"},{"额外桌子租赁",5.0,"张","zz"},{"额外椅子租赁",3.0,"把","yz"}};
        for (Object[] s : ss) {
            ContentValues cv = new ContentValues();
            cv.put(SVC_NAME,(String)s[0]); cv.put(SVC_MARKET_PRICE,(Double)s[1]);
            cv.put(SVC_OUR_PRICE,(Double)s[1]*0.9); cv.put(SVC_UNIT,(String)s[2]);
            cv.put(SVC_ICON,(String)s[3]); db.insert(TABLE_SERVICE,null,cv);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RES_SVC);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SERVICE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTICE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ROOM);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER);
        onCreate(db);
    }
}
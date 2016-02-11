package com.teskalabs.seacat.android.companion.Model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by mpavelka on 09/02/16.
 */
public class ModelProfile extends SQLiteOpenHelper {
    public static final String TAG = "ModelProfile";
    private Integer id;
    private String profileName;
    private String gatewayName;
    private String ip;
    private String port;

    public static final String TABLE_NAME = "profiles";
    public static final String COL_ID = "_id";
    public static final String COL_PROFILE_NAME = "profileName";
    public static final String COL_GATEWAY_NAME = "gatewayName";
    public static final String COL_IP = "ip";
    public static final String COL_PORT = "port";

    public static final int DATABASE_VERSION = 4;
    public static final String DATABASE_NAME = "teskalabs.companion.db";

    public static final String SQL_CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COL_ID + " INTEGER PRIMARY KEY," +
                    COL_PROFILE_NAME + " TEXT," +
                    COL_GATEWAY_NAME + " TEXT," +
                    COL_IP + " TEXT," +
                    COL_PORT + " TEXT" +
                    ")";

    public static final String SQL_DROP_TABLE =
            "DROP TABLE IF EXISTS " + TABLE_NAME + ";";


    public void setGatewayName(String gatewayName) { this.gatewayName = gatewayName; }
    public void setProfileName(String profileName) { this.profileName = profileName; }
    public void setIp(String ip) { this.ip = ip; }
    public void setPort(String port) { this.port = port; }

    public Integer getId() { return this.id; }
    public String getGatewayName() { return this.gatewayName; }
    public String getProfileName() { return this.profileName; }
    public String getIp() { return this.ip; }
    public String getPort() { return this.port; }

    public ModelProfile(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    public void onCreate(SQLiteDatabase db) {
        Log.i(TAG, "Creating database.");
        db.execSQL(SQL_DROP_TABLE);
        db.execSQL(ModelProfile.SQL_CREATE_TABLE);
        db.execSQL("INSERT INTO " + TABLE_NAME + " (" + COL_PROFILE_NAME + "," + COL_GATEWAY_NAME + ", " + COL_IP + ", " + COL_PORT + ")" +
                " VALUES ('default', 'localhost.s.seacat.mobi', '10.0.2.2', '433');");
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(TAG, "Upgrading database.");
        db.execSQL(SQL_DROP_TABLE);
        db.execSQL(ModelProfile.SQL_CREATE_TABLE);
        db.execSQL("INSERT INTO " + TABLE_NAME + " (" + COL_PROFILE_NAME + ", " + COL_GATEWAY_NAME + ", " + COL_IP + ", " + COL_PORT + ")" +
                " VALUES ('default', 'localhost.s.seacat.mobi', '10.0.2.2', '433');");
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    private void setFromCursor(Cursor cursor)
    {
        this.id = cursor.getInt(cursor.getColumnIndex(COL_ID));
        this.setProfileName(cursor.getString(cursor.getColumnIndex(COL_PROFILE_NAME)));
        this.setGatewayName(cursor.getString(cursor.getColumnIndex(COL_GATEWAY_NAME)));
        this.setIp(cursor.getString(cursor.getColumnIndex(COL_IP)));
        this.setPort(cursor.getString(cursor.getColumnIndex(COL_PORT)));;
    }

    public Cursor findAll()
    {
        SQLiteDatabase db = getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM " + TABLE_NAME + " ORDER BY " + COL_PROFILE_NAME + " ASC;"
                , null);
    }

    public ModelProfile findOneById(int id)
    {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_NAME + " WHERE " + COL_ID + "=" + id + " ORDER BY " + COL_PROFILE_NAME + " ASC;"
                , null);
        if (!cursor.moveToFirst())
            throw new RuntimeException("Profile not found.");
        setFromCursor(cursor);
        return this;
    }

    public ModelProfile findOneByName(String name)
    {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_NAME + " WHERE " + COL_PROFILE_NAME + "=" + name + " ORDER BY " + COL_PROFILE_NAME + " ASC;"
                , null);
        if (!cursor.moveToFirst())
            throw new RuntimeException("Profile not found.");
        setFromCursor(cursor);
        return this;
    }

    public long insert()
    {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        if (id != null)
            values.put(COL_ID, id);
        values.put(COL_PROFILE_NAME, profileName);
        values.put(COL_GATEWAY_NAME, gatewayName);
        values.put(COL_IP, ip);
        values.put(COL_PORT, port);

        return db.insert(
                TABLE_NAME,
                null,
                values);
    }

    public long update()
    {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_PROFILE_NAME, profileName);
        values.put(COL_GATEWAY_NAME, gatewayName);
        values.put(COL_IP, ip);
        values.put(COL_PORT, port);

        return db.update(
                TABLE_NAME,
                values,
                COL_ID + "=?",
                new String[]{id.toString()});
    }

    public int removeById(Integer id)
    {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(
                TABLE_NAME,
                COL_ID + "=?",
                new String[]{id.toString()});
    }

}

/*
 * Made by:
 * Uroš Podkrižnik
 * uros.podkriznik(at)gmail.com
 * Tel.: 041829380
 */

package enklikanketa.com.a1kapanel.System;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import enklikanketa.com.a1kapanel.Libraries.GeneralLib;

public class Database extends SQLiteOpenHelper {

    private String TAG = "Database";

    /**
     * Ko se naredi upgrade baze, je treba popravit v funkciji createDatabase(), dodati case v
     * onUpgrade() in popravit konstanto DATABASE_VERSION
     *
     * Important:
     *      19.3.2018 - do not downgrade to version 6, downgrade it to 5 instead
     *          also, when upgrading, better to upgrade to 6 instead of 5
     */
    private static final int DATABASE_VERSION = 20;

    private SQLiteDatabase db;
    private static SQLiteOpenHelper mInstance;

    private Database(Context cont) {
        super(cont, "1kapanel_baza", null, DATABASE_VERSION);
        //if DB is not created yet, this will return null, but in onCrate it will initialize
        db = getDB();
    }

    /**
     * Get default instance of the class to keep it a singleton
     *
     * @param context
     *            the application context
     */
    public static synchronized SQLiteOpenHelper getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new Database(context.getApplicationContext());
        }
        return mInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        this.db = db;
        createDatabase();
        //insert device data
        ContentValues[] valuesArr = getDeviceInfoValues();
        for(ContentValues values : valuesArr)
            insertData("device_info", values);
    }

    @Override
    public void close() {
        super.close();
        if (db != null) {
            db.close();
            db = null;
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        while (oldVersion < newVersion)
        {
            switch (oldVersion)
            {
                case 0:
                case 1:
                    Log.d(TAG, "Database upgraded to 2.");
                    //tabela, kjer so shranjene info o anketah
                    db.execSQL("DROP TABLE IF EXISTS surveys;");
                    db.execSQL("CREATE TABLE IF NOT EXISTS surveys "
                            + "(id INTEGER PRIMARY KEY, link VARCHAR);");
                    //tabela, kjer so shranjene info o alarmih
                    db.execSQL("DROP TABLE IF EXISTS alarms;");
                    db.execSQL("CREATE TABLE IF NOT EXISTS alarms "
                            + "(alarm_id INTEGER PRIMARY KEY, srv_id INTEGER, alarm_notif_title VARCHAR, " +
                            "alarm_notif_message VARCHAR, alarm_notif_repeat INTEGER, alarm_notif_sound TINYINT, " +
                            "FOREIGN KEY(srv_id) REFERENCES surveys(id) ON DELETE CASCADE);");
                    break;
                case 2:
                    Log.d(TAG, "Database upgraded to 3.");
                    db.execSQL("DROP TABLE IF EXISTS things_to_send;");
                    db.execSQL("CREATE TABLE IF NOT EXISTS things_to_send "
                            + "(name VARCHAR, value VARCHAR, event VARCHAR, tsSec INTEGER);");
                    break;
                case 3:
                case 4:
                    Log.d(TAG, "Database upgraded to 5.");
                    db.execSQL("DROP TABLE IF EXISTS alarms;");
                    db.execSQL("CREATE TABLE IF NOT EXISTS alarms "
                            + "(alarm_id INTEGER PRIMARY KEY, srv_id INTEGER, alarm_notif_title VARCHAR, " +
                            "alarm_notif_message VARCHAR, repeat_by VARCHAR, time_in_day VARCHAR, " +
                            "day_in_week VARCHAR, every_which_day VARCHAR, alarm_notif_sound TINYINT, " +
                            "FOREIGN KEY(srv_id) REFERENCES surveys(id) ON DELETE CASCADE);");
                    break;
                case 5:
                    Log.d(TAG, "Database upgraded to 6.");
                    db.execSQL("CREATE TABLE IF NOT EXISTS repeaters "
                            + "(id INTEGER PRIMARY KEY AUTOINCREMENT, srv_id INTEGER, " +
                            "repeat_by VARCHAR, time_in_day VARCHAR, day_in_week VARCHAR, " +
                            "every_which_day VARCHAR, datetime_start VARCHAR, datetime_end VARCHAR, " +
                            "datetime_last_check VARCHAR, FOREIGN KEY(srv_id) REFERENCES surveys(id) ON DELETE CASCADE);");
                    break;
                case 6:
                    Log.d(TAG, "Database upgraded to 7.");
                    try {
                        db.execSQL("ALTER TABLE repeaters ADD COLUMN datetime_user_started VARCHAR;");
                    } catch (SQLiteException e) {
                        Log.d(TAG, "Column datetime_user_started (VARCHAR) already exists in table repeaters at DB upgrade to 7.");
                    }
                    break;
                case 7:
                    Log.d(TAG, "Database upgraded to 8.");
                    db.execSQL("CREATE TABLE IF NOT EXISTS geofences "
                            + "(id INTEGER PRIMARY KEY, srv_id INTEGER, lat REAL, lng REAL, radius REAL, " +
                            "address VARCHAR, notif_title VARCHAR, notif_message VARCHAR, " +
                            "notif_sound INTEGER, on_transition VARCHAR, after_seconds INTEGER, " +
                            "was_dwelled INTEGER DEFAULT 0, " +
                            "FOREIGN KEY(srv_id) REFERENCES surveys(id) ON DELETE CASCADE);");
                case 8:
                    Log.d(TAG, "Database upgraded to 9.");
                    db.execSQL("CREATE TABLE IF NOT EXISTS activities "
                            + "(id INTEGER PRIMARY KEY, srv_id INTEGER, " +
                            "notif_title VARCHAR, notif_message VARCHAR, " +
                            "notif_sound INTEGER, activity_type VARCHAR, after_seconds INTEGER, " +
                            "permission INTEGER DEFAULT 0, " +
                            "FOREIGN KEY(srv_id) REFERENCES surveys(id) ON DELETE CASCADE);");
                case 9:
                    Log.d(TAG, "Database upgraded to 10.");
                    try {
                        db.execSQL("ALTER TABLE surveys ADD COLUMN title VARCHAR;");
                    } catch (SQLiteException e) {
                        Log.d(TAG, "Column title (VARCHAR) already exists in table surveys at DB upgrade to 10.");
                    }
                case 10:
                    Log.d(TAG, "Database upgraded to 11.");
                    try {
                        db.execSQL("ALTER TABLE uporabnik ADD COLUMN nextpin_password VARCHAR;");
                    } catch (SQLiteException e) {
                        Log.d(TAG, "Column nextpin_password (VARCHAR) already exists in table uporabnik at DB upgrade to 11.");
                    }
                case 11:
                    Log.d(TAG, "Database upgraded to 12.");
                    db.execSQL("CREATE TABLE IF NOT EXISTS tracking "
                            + "(id INTEGER PRIMARY KEY, srv_id INTEGER, " +
                            "tracking_accuracy VARCHAR, interval_wanted INTEGER, " +
                            "interval_fastes INTEGER, displacement_min INTEGER, " +
                            "activity_recognition INTEGER DEFAULT 0, permission INTEGER DEFAULT 0, " +
                            "FOREIGN KEY(srv_id) REFERENCES surveys(id) ON DELETE CASCADE);");
                case 12:
                    Log.d(TAG, "Database upgraded to 13.");
                    db.execSQL("CREATE TABLE IF NOT EXISTS locations "
                            + "(id INTEGER PRIMARY KEY, lat TEXT, lng TEXT, provider VARCHAR, " +
                            "timestamp INTEGER, accuracy TEXT, altitude TEXT, bearing TEXT, " +
                            "speed TEXT, bearing_acc TEXT, speed_acc TEXT, vertical_acc TEXT, " +
                            "extras TEXT, is_mock NUMERIC, sending NUMERIC DEFAULT 0);");
                case 13:
                    Log.d(TAG, "Database upgraded to 14.");
                    try {
                        db.execSQL("ALTER TABLE tracking ADD COLUMN ar_interval_wanted INTEGER DEFAULT 30;");
                    } catch (SQLiteException e) {
                        Log.d(TAG, "Column ar_interval_wanted (INTEGER) already exists in table tracking at DB upgrade to 14.");
                    }
                    db.execSQL("CREATE TABLE IF NOT EXISTS activity_recognition "
                            + "(id INTEGER PRIMARY KEY, timestamp INTEGER, in_vehicle INTEGER DEFAULT 0, " +
                            "on_bicycle INTEGER DEFAULT 0, on_foot INTEGER DEFAULT 0, still INTEGER DEFAULT 0, " +
                            "unknown INTEGER DEFAULT 0, tilting INTEGER DEFAULT 0, " +
                            "running INTEGER DEFAULT 0, walking INTEGER DEFAULT 0, sending NUMERIC DEFAULT 0);");
                case 14:
                    Log.d(TAG, "Database upgraded to 15.");
                    try {
                        db.execSQL("ALTER TABLE locations ADD COLUMN edited NUMERIC DEFAULT 0;");
                    } catch (SQLiteException e) {
                        Log.d(TAG, "Column edited (NUMERIC) already exists in table locations at DB upgrade to 15.");
                    }
                case 15:
                    Log.d(TAG, "Database upgraded to 16.");
                    try {
                        db.execSQL("ALTER TABLE geofences ADD COLUMN location_triggered NUMERIC DEFAULT 0;");
                    } catch (SQLiteException e) {
                        Log.d(TAG, "Column location_triggered (NUMERIC) already exists in table geofences at DB upgrade to 16.");
                    }
                    try {
                        db.execSQL("ALTER TABLE geofences ADD COLUMN name VARCHAR;");
                    } catch (SQLiteException e) {
                        Log.d(TAG, "Column name (VARCHAR) already exists in table geofences at DB upgrade to 16.");
                    }
                case 16:
                    Log.d(TAG, "Database upgraded to 17.");
                    db.execSQL("CREATE TABLE IF NOT EXISTS entry "
                            + "(id INTEGER PRIMARY KEY, srv_id INTEGER, location_check NUMERIC DEFAULT 0);");
                    try {
                        db.execSQL("ALTER TABLE locations ADD COLUMN server_input_id INTEGER DEFAULT -1;");
                    } catch (SQLiteException e) {
                        Log.d(TAG, "Column server_input_id (INTEGER) already exists in table locations at DB upgrade to 16.");
                    }
                case 17:
                    Log.d(TAG, "Database upgraded to 18.");
                    try {
                        db.execSQL("ALTER TABLE geofences ADD COLUMN trigger_survey NUMERIC DEFAULT 0;");
                    } catch (SQLiteException e) {
                        Log.d(TAG, "Column trigger_survey (NUMERIC) already exists in table geofences at DB upgrade to 18.");
                    }
                case 18:
                    Log.d(TAG, "Database upgraded to 19.");
                    try {
                        db.execSQL("ALTER TABLE geofences ADD COLUMN enter_timestamp INTEGER DEFAULT 0;");
                        db.execSQL("ALTER TABLE geofences ADD COLUMN dwell_timestamp INTEGER DEFAULT 0;");
                    } catch (SQLiteException e) {
                        Log.d(TAG, "Columns enter_timestamp and dwell_timestamp (INTEGER) already exists in table geofences at DB upgrade to 19.");
                    }
                    try {
                        db.execSQL("ALTER TABLE things_to_send ADD COLUMN param1 TEXT;");
                        db.execSQL("ALTER TABLE things_to_send ADD COLUMN param2 TEXT;");
                        db.execSQL("ALTER TABLE things_to_send ADD COLUMN param3 TEXt;");
                    } catch (SQLiteException e) {
                        Log.d(TAG, "Columns param1, param2, param3 (TEXT) already exists in table things_to_send at DB upgrade to 19.");
                    }
                case 19:
                    Log.d(TAG, "Database upgraded to 20.");
                    try {
                        db.execSQL("ALTER TABLE surveys ADD COLUMN description VARCHAR;");
                    } catch (SQLiteException e) {
                        Log.d(TAG, "Column description (VARCHAR) already exists in table surveys at DB upgrade to 20.");
                    }
            }
            oldVersion++;
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        while (oldVersion > newVersion)
        {
            switch (oldVersion)
            {
                case 0:
                case 1:
                    Log.d(TAG, "Database upgraded to 0.");
                    break;
                case 2:
                    Log.d(TAG, "Database onDowngrade to 1.");
                    //tabela, kjer so shranjene info o anketah
                    db.execSQL("DROP TABLE IF EXISTS surveys;");
                    //tabela, kjer so shranjene info o alarmih
                    db.execSQL("DROP TABLE IF EXISTS alarms;");
                    break;
                case 3:
                    Log.d(TAG, "Database onDowngrade to 2.");
                    db.execSQL("DROP TABLE IF EXISTS things_to_send;");
                    break;
                case 4:
                    Log.d(TAG, "Database onDowngrade to 3.");
                    db.execSQL("DROP TABLE IF EXISTS alarms;");
                    db.execSQL("CREATE TABLE IF NOT EXISTS alarms "
                            + "(alarm_id INTEGER PRIMARY KEY, srv_id INTEGER, alarm_notif_title VARCHAR, " +
                            "alarm_notif_message VARCHAR, alarm_notif_repeat INTEGER, alarm_notif_sound TINYINT, " +
                            "FOREIGN KEY(srv_id) REFERENCES surveys(id) ON DELETE CASCADE);");
                    break;
                case 6:
                case 7:
                    Log.d(TAG, "Database onDowngrade to 5.");
                    db.execSQL("DROP TABLE IF EXISTS repeaters;");
                    break;
                case 8:
                    Log.d(TAG, "Database onDowngrade to 7.");
                    db.execSQL("DROP TABLE IF EXISTS geofences;");
                    break;
                case 9:
                    Log.d(TAG, "Database onDowngrade to 8.");
                    db.execSQL("DROP TABLE IF EXISTS activities;");
                    break;
                case 10:
                    Log.d(TAG, "Database onDowngrade to 9.");
                    //no need to do anything - column title in surveys table can hang
                    break;
                case 11:
                    Log.d(TAG, "Database onDowngrade to 10.");
                    //no need to do anything - column nextpin_password in uporabnik table can hang
                    break;
                case 12:
                    Log.d(TAG, "Database onDowngrade to 11.");
                    db.execSQL("DROP TABLE IF EXISTS tracking;");
                    break;
                case 13:
                    Log.d(TAG, "Database onDowngrade to 12.");
                    db.execSQL("DROP TABLE IF EXISTS locations;");
                    break;
                case 14:
                    Log.d(TAG, "Database onDowngrade to 13.");
                    db.execSQL("DROP TABLE IF EXISTS activity_recognition;");
                    //column ar_interval_wanted in tracking table can hang
                    break;
                case 15:
                    Log.d(TAG, "Database onDowngrade to 14.");
                    //no need to do anything - column edited in locations table can hang
                    break;
                case 16:
                    Log.d(TAG, "Database onDowngrade to 15.");
                    //no need to do anything - columns location_triggered and name in geofences table can hang
                    break;
                case 17:
                    Log.d(TAG, "Database onDowngrade to 16.");
                    db.execSQL("DROP TABLE IF EXISTS entry;");
                    //column server_input_id in locations table can hang
                    break;
                case 18:
                    Log.d(TAG, "Database onDowngrade to 17.");
                    //no need to do anything - column trigger_survey in geofences table can hang
                    break;
                case 19:
                    Log.d(TAG, "Database onDowngrade to 18.");
                    //columns enter_timestamp and dwell_timestamp in geofences table can hang
                    //columns param1, param2, param3 in things_to_send table can hang
                    //column was_dwelled in geofences was deleted in newer versions, so remake it
                    try {
                        db.execSQL("ALTER TABLE geofences ADD COLUMN was_dwelled INTEGER DEFAULT 0;");
                    } catch (SQLiteException e) {
                        Log.d(TAG, "Column was_dwelled (INTEGER) already exists in table geofences at DB upgrade to 19.");
                    }
                    break;
                case 20:
                    Log.d(TAG, "Database onDowngrade to 19.");
                    //no need to do anything - column description in surveys table can hang
                    break;
            }
            oldVersion--;
        }
    }

    @Override
    public void onConfigure(SQLiteDatabase db){
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    /**
     * Returns a writable database instance in order not to open and close many
     * SQLiteDatabase objects simultaneously
     *
     * @return a writable instance to SQLiteDatabase
     */
    public SQLiteDatabase getDB() {
        if ((db == null) || (!db.isOpen())) {
            db = this.getWritableDatabase();
        }
        return db;
    }

    /**
     * Update DB - remove all tables and create new ones <br>
     * <b>This function will remove all stored data</b>
     */
    private void createDatabase() {
        try {
            //tabela, kjer je shranjen uporabnik
            db.execSQL("DROP TABLE IF EXISTS uporabnik;");
            db.execSQL("CREATE TABLE IF NOT EXISTS uporabnik "
                    + "(id INTEGER PRIMARY KEY, id_server INTEGER, identifier VARCHAR, "
                    + "nextpin_password VARCHAR);");
            //tabela, kjer so shranjene info o napravi
            db.execSQL("DROP TABLE IF EXISTS device_info;");
            db.execSQL("CREATE TABLE IF NOT EXISTS device_info "
                    + "(name VARCHAR, value VARCHAR);");
            //tabela, kjer so shranjene info o anketah
            db.execSQL("DROP TABLE IF EXISTS surveys;");
            db.execSQL("CREATE TABLE IF NOT EXISTS surveys "
                    + "(id INTEGER PRIMARY KEY, link VARCHAR, title VARCHAR, description VARCHAR);");
            //tabela, kjer so shranjene info o alarmih
            db.execSQL("DROP TABLE IF EXISTS alarms;");
            db.execSQL("CREATE TABLE IF NOT EXISTS alarms "
                    + "(alarm_id INTEGER PRIMARY KEY, srv_id INTEGER, alarm_notif_title VARCHAR, " +
                    "alarm_notif_message VARCHAR, repeat_by VARCHAR, time_in_day VARCHAR, " +
                    "day_in_week VARCHAR, every_which_day VARCHAR, alarm_notif_sound TINYINT, " +
                    "FOREIGN KEY(srv_id) REFERENCES surveys(id) ON DELETE CASCADE);");
            //table of things to send when connection available
            db.execSQL("DROP TABLE IF EXISTS things_to_send;");
            db.execSQL("CREATE TABLE IF NOT EXISTS things_to_send "
                    + "(name VARCHAR, value VARCHAR, event VARCHAR, tsSec INTEGER, " +
                    "param1 TEXT, param2 TEXT, param3 TEXT);");
            //table to store repeaters of survey
            db.execSQL("DROP TABLE IF EXISTS repeaters;");
            db.execSQL("CREATE TABLE IF NOT EXISTS repeaters "
                    + "(id INTEGER PRIMARY KEY AUTOINCREMENT, srv_id INTEGER, " +
                    "repeat_by VARCHAR, time_in_day VARCHAR, day_in_week VARCHAR, " +
                    "every_which_day VARCHAR, datetime_start VARCHAR, datetime_end VARCHAR, " +
                    "datetime_last_check VARCHAR, datetime_user_started VARCHAR, " +
                    "FOREIGN KEY(srv_id) REFERENCES surveys(id) ON DELETE CASCADE);");
            //table to store geofences of survey
            db.execSQL("DROP TABLE IF EXISTS geofences;");
            db.execSQL("CREATE TABLE IF NOT EXISTS geofences "
                    + "(id INTEGER PRIMARY KEY, srv_id INTEGER, lat REAL, lng REAL, radius REAL, " +
                    "address VARCHAR, name VARCHAR, notif_title VARCHAR, notif_message VARCHAR, " +
                    "notif_sound INTEGER, on_transition VARCHAR, after_seconds INTEGER, " +
                    "location_triggered NUMERIC DEFAULT 0, trigger_survey NUMERIC DEFAULT 0, " +
                    "enter_timestamp INTEGER DEFAULT 0, dwell_timestamp INTEGER DEFAULT 0, " +
                    "FOREIGN KEY(srv_id) REFERENCES surveys(id) ON DELETE CASCADE);");
            //table to store activities of survey
            db.execSQL("DROP TABLE IF EXISTS activities;");
            db.execSQL("CREATE TABLE IF NOT EXISTS activities "
                    + "(id INTEGER PRIMARY KEY, srv_id INTEGER, " +
                    "notif_title VARCHAR, notif_message VARCHAR, " +
                    "notif_sound INTEGER, activity_type VARCHAR, after_seconds INTEGER, " +
                    "permission INTEGER DEFAULT 0, " +
                    "FOREIGN KEY(srv_id) REFERENCES surveys(id) ON DELETE CASCADE);");
            //table to store tracking of survey
            db.execSQL("DROP TABLE IF EXISTS tracking;");
            db.execSQL("CREATE TABLE IF NOT EXISTS tracking "
                    + "(id INTEGER PRIMARY KEY, srv_id INTEGER, " +
                    "tracking_accuracy VARCHAR, interval_wanted INTEGER, " +
                    "interval_fastes INTEGER, displacement_min INTEGER, " +
                    "activity_recognition INTEGER DEFAULT 0, ar_interval_wanted INTEGER DEFAULT 30, " +
                    "permission INTEGER DEFAULT 0, " +
                    "FOREIGN KEY(srv_id) REFERENCES surveys(id) ON DELETE CASCADE);");
            //table to store locations of tracking
            //sending column - 0=not sended yet, 1=sending now, 2=sended
            //server_input_id column - id on server of unit/entry/respondent id
            //edited column - 0=not edited, 1=edited, 2=deleted
            //server_input_id column - id of location in case of entry (regular location is -1 by default)
            db.execSQL("DROP TABLE IF EXISTS locations;");
            db.execSQL("CREATE TABLE IF NOT EXISTS locations "
                    + "(id INTEGER PRIMARY KEY, lat TEXT, lng TEXT, provider VARCHAR, " +
                    "timestamp INTEGER, accuracy TEXT, altitude TEXT, bearing TEXT, " +
                    "speed TEXT, bearing_acc TEXT, speed_acc TEXT, vertical_acc TEXT, " +
                    "extras TEXT, is_mock NUMERIC, sending NUMERIC DEFAULT 0, " +
                    "server_input_id INTEGER DEFAULT -1, edited NUMERIC DEFAULT 0);");
            //table to store activity recognition of tracking
            //sending column - 0=not sended yet, 1=sending now, 2=sended
            db.execSQL("DROP TABLE IF EXISTS activity_recognition;");
            db.execSQL("CREATE TABLE IF NOT EXISTS activity_recognition "
                    + "(id INTEGER PRIMARY KEY, timestamp INTEGER, in_vehicle INTEGER DEFAULT 0, " +
                    "on_bicycle INTEGER DEFAULT 0, on_foot INTEGER DEFAULT 0, still INTEGER DEFAULT 0, " +
                    "unknown INTEGER DEFAULT 0, tilting INTEGER DEFAULT 0, " +
                    "running INTEGER DEFAULT 0, walking INTEGER DEFAULT 0, sending NUMERIC DEFAULT 0);");
            //table to store data entries
            db.execSQL("DROP TABLE IF EXISTS entry;");
            db.execSQL("CREATE TABLE IF NOT EXISTS entry "
                    + "(id INTEGER PRIMARY KEY, srv_id INTEGER, location_check NUMERIC DEFAULT 0);");
        } catch (Exception e) {
            GeneralLib.reportCrash(e, null);
        }
    }//upgradeDatabase

    /**
     * Delete data from table
     * db.execSQL("DELETE FROM " + table + ";");
     *
     * @param table name of table
     */
    public void deleteAllRows(String table) {
        try {
            db.execSQL("DELETE FROM " + table + ";");
        } catch (Exception e) {
            GeneralLib.reportCrash(e, null);
        }
    }//deleteAllRows

    /**
     * Delete data from table
     *
     * @param table name of table
     * @param where where statement
     */
    public void deleteRows(String table, String where) {
        try {
            db.execSQL("DELETE FROM " + table + " WHERE " + where + ";");
        } catch (Exception e) {
            GeneralLib.reportCrash(e, null);
        }
    }//deleteAllRows

    public void clearAllFromDatabase(){
        // query to obtain the names of all tables in your database
        Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
        List<String> tables = new ArrayList<>();

        // iterate over the result set, adding every table name to a list
        while (c.moveToNext()) {
            tables.add(c.getString(0));
        }

        // delete all records from all tables
        for (String table : tables) {
            deleteAllRows(table);
        }
        c.close();
    }

    /**
     * Get data from app's DB for one array (one survey, one question,...)
     * unique ID is usualy in where statement
     *
     * @param table   name of table
     * @param columns wanted columns to get
     * @param where   where statement, null if not needed (eg for user email)
     * @return String array of data
     */
    public String[] getRowData(String table, String[] columns, String where) {
        String[] temp_array = null;

        try {
            Cursor c = db.query(table, columns, where, null, null, null, null);
            if (c.getCount() != 0) {
                temp_array = new String[c.getColumnCount()];
                c.moveToFirst();
                for (int i = 0; i < c.getColumnCount(); i++) {
                    temp_array[i] = c.getString(i);
                }
            } else {
                Log.d(TAG, "Database.getRowData() - No data");
            }
            c.close();
            return temp_array;
        } catch (Exception e) {
            GeneralLib.reportCrash(e, null);
            return null;
        }
    }//getRowData

    /**
     * Get data from app's DB for one array (one survey, one question,...)
     * unique ID is usualy in where statement
     *
     * @param table   name of table
     * @param columns wanted columns to get
     * @param where   where statement, null if not needed (eg for user email)
     * @return HashMap of data
     */
    public HashMap<String, String> getRowHashMapData(String table, String[] columns, String where) {
        HashMap<String, String> temp_map = null;

        try {
            Cursor c = db.query(table, columns, where, null, null, null, null);
            if (c.getCount() != 0) {
                temp_map = new HashMap<>();
                c.moveToFirst();
                for (int i = 0; i < c.getColumnCount(); i++) {
                    temp_map.put(c.getColumnName(i), c.getString(i));
                }
            } else {
                Log.d(TAG, "Database.getRowHashMapData() - No data");
            }
            c.close();
            return temp_map;
        } catch (Exception e) {
            GeneralLib.reportCrash(e, null);
            return null;
        }
    }//getRowHashMapData

    /**
     * Get data from app's DB for all rows
     * @param table   name of table
     * @param columns wanted columns to get
     * @param where   where statement, null if not needed
     * @param orderby   order by statement, null if not needed
     * @return ArrayList<HashMap> of data
     */
    public ArrayList<HashMap<String, String>> getListHashMapData(String table, String[] columns, String where, String orderby) {
        ArrayList<HashMap<String, String>> list = null;

        try {
            Cursor c = db.query(table, columns, where, null, null, null, orderby);
            int ccount = c.getCount();
            if (ccount > 0) {
                list = new ArrayList<>();
                c.moveToFirst();
                for(int i = 0; i < ccount; i++) {
                    HashMap<String, String> temp_map = new HashMap<>();
                    for (int j = 0; j < c.getColumnCount(); j++) {
                        temp_map.put(c.getColumnName(j), c.getString(j));
                    }
                    c.moveToNext();
                    list.add(temp_map);
                }
            } else {
                Log.d(TAG, "Database.getRowHashMapData() - No data");
            }
            c.close();
            return list;
        } catch (Exception e) {
            GeneralLib.reportCrash(e, null);
            return null;
        }
    }//getListHashMapData

    /**
     * Get one column of all rows
     * @param table   name of table
     * @param column  wanted column to get
     * @param where   where statement, null if not needed (eg for user email)
     * @return ArrayList<String> of one column
     */
    public ArrayList<String> getListOfOneColumn(String table, String column, String where) {
        ArrayList<String> list = new ArrayList<>();

        try {
            Cursor c = db.query(table, new String[]{column}, where, null, null, null, null);
            int ccount = c.getCount();
            if (ccount > 0) {
                c.moveToFirst();
                for(int i = 0; i < ccount; i++) {
                    list.add(c.getString(0));
                    c.moveToNext();
                }
            } else {
                Log.d(TAG, "Database.getListOfOneColumn() - No data");
            }
            c.close();
            return list;
        } catch (Exception e) {
            GeneralLib.reportCrash(e, null);
            return null;
        }
    }//getListOfOneColumn

    /**
     * Get data from app's DB
     * unique ID is usualy in where statement
     *
     * @param table   name of table
     * @param columns wanted columns to get
     * @param where   where statement, null if not needed (eg for user email)
     * @return String array of data
     */
    public String[][] getData(String table, String[] columns, String where) {
        String[][] temp_array = null;

        try {
            Cursor c = db.query(table, columns, where, null, null, null, null);
            int ccount = c.getCount();
            if (ccount != 0) {
                temp_array = new String[ccount][c.getColumnCount()];
                c.moveToFirst();
                for(int i = 0; i < ccount; i++) {
                    for (int j = 0; j < c.getColumnCount(); j++) {
                        temp_array[i][j] = c.getString(j);
                    }
                    c.moveToNext();
                }
            } else {
                Log.d(TAG, "Database.getData() - No data");
            }
            c.close();
            return temp_array;
        } catch (Exception e) {
            GeneralLib.reportCrash(e, null);
            return null;
        }
    }//getData

    /**
     * Get data from app's DB as JSONArray
     * unique ID is usualy in where statement
     *
     * @param table   name of table
     * @param columns wanted columns to get
     * @param where   where statement, null if not needed (eg for user email)
     * @return String array of data
     */
    public JSONArray getJSONData(String table, String[] columns, String where) {
        JSONArray temp_array = new JSONArray();

        try {
            Cursor c = db.query(table, columns, where, null, null, null, null);
            int ccount = c.getCount();
            if (ccount != 0) {
                c.moveToFirst();
                for(int i = 0; i < ccount; i++) {
                    JSONObject jsonObj = new JSONObject();
                    for (int j = 0; j < c.getColumnCount(); j++) {
                        jsonObj.put(c.getColumnName(j), c.getString(j));
                    }
                    temp_array.put(jsonObj);
                    c.moveToNext();
                }
            } else {
                Log.d(TAG, "Database.getJSONData() - No data");
            }
            c.close();
            if(temp_array.length() == 0)
                return null;
            else
                return temp_array;
        } catch (Exception e) {
            GeneralLib.reportCrash(e, null);
            return null;
        }
    }//getJSONData


    /**
     * Get data from app's DB as JSONObject
     * unique ID is usualy in where statement
     *
     * @param table   name of table
     * @param columns wanted columns to get
     * @param where   where statement, null if not needed (eg for user email)
     * @return String array of data
     */
    public JSONObject getJSONDataObject(String table, String[] columns, String where) {
        JSONObject jsonObj = new JSONObject();

        try {
            Cursor c = db.query(table, columns, where, null, null, null, null);
            int ccount = c.getCount();
            if (ccount != 0) {
                c.moveToFirst();
                for(int i = 0; i < ccount; i++) {
                    for (int j = 0; j < c.getColumnCount(); j++) {
                        jsonObj.put(c.getColumnName(j), c.getString(j));
                    }
                    c.moveToNext();
                }
            } else {
                Log.d(TAG, "Database.getJSONData() - No data");
            }
            c.close();
            if(jsonObj.length() == 0)
                return null;
            else
                return jsonObj;
        } catch (Exception e) {
            GeneralLib.reportCrash(e, null);
            return null;
        }
    }//getJSONData

    /**
     * Get data from app's DB as JSONObject from name-value like table (deviceInfo)
     * unique ID is usualy in where statement
     *
     * @param table   name of table
     * @param columns wanted pair of columns to get ({"name", "value"})
     * @param where   where statement, null if not needed (eg for user email)
     * @return String array of data
     */
    public JSONObject getJSONDataFromNameValueTable(String table, String[] columns, String where) {
        JSONObject jsonObj = new JSONObject();

        try {
            Cursor c = db.query(table, columns, where, null, null, null, null);
            int ccount = c.getCount();
            if (ccount != 0) {
                c.moveToFirst();
                for(int i = 0; i < ccount; i++) {
                    jsonObj.put(c.getString(0), c.getString(1));
                    c.moveToNext();
                }
            } else {
                Log.d(TAG, "Database.getJSONData() - No data");
            }
            c.close();
            if(jsonObj.length() == 0)
                return null;
            else
                return jsonObj;
        } catch (Exception e) {
            GeneralLib.reportCrash(e, null);
            return null;
        }
    }//getJSONData

    /**
     * Insert new row in DB
     *
     * @param table   name of table
     * @param podatki data to insert - eg {{ "name_of_column", "value" },...}
     * @return id of new row - int id = getLastID(db);
     */
    public int insertData(String table, String[][] podatki) {
        try {
            String values = "", columns = "";

            for (int i = 0; i < podatki.length; i++) {
                if (podatki[i][1] != null) {
                    if (i == 0) {
                        columns = podatki[i][0];
                        values = "'" + podatki[i][1].replaceAll("'", "''") + "'";
                    } else {
                        columns += ", " + podatki[i][0];
                        values += ", '" + podatki[i][1].replaceAll("'", "''") + "'";
                    }
                }
            }

            db.execSQL("INSERT INTO " + table + " (" + columns + ") "
                    + "VALUES (" + values + ");");

            //get id of last inserted row
            int id = getLastID(table);
            return id;
        } catch (Exception e) {
            GeneralLib.reportCrash(e, null);
            return -1;
        }
    }//insertData

    /**
     * Get data from app's DB for multiple arrays
     *
     * @param table   name of table
     * @param columns wanted columns to get
     * @param where   where statement, null if not needed
     * @return String[ROW_INDEX][VALUES] of data
     */
    public String[][] getPairData(String table, String[] columns, String where) {
        String[][] temp_array = null;

        try {
            Cursor c = db.query(table, columns, where, null, null, null, null);
            if (c.getCount() != 0) {
                temp_array = new String[c.getCount()][c.getColumnCount()];
                c.moveToFirst();
                do{
                    for (int col = 0; col < c.getColumnCount(); col++) {
                        temp_array[c.getPosition()][col] = c.getString(col);
                    }
                }
                while (c.moveToNext());
            } else {
                Log.d(TAG, "Database.getPairData() - No data");
            }
            c.close();
            return temp_array;
        } catch (Exception e) {
            GeneralLib.reportCrash(e, null);
            return null;
        }
    }//getPairData

    /**
     * Update row in DB
     *
     * @param table   name of table
     * @param podatki ContentValues values
     * @return id of new row - int id = getLastID(db);
     */
    public int updateData(String table, ContentValues podatki, String where) {
        try {
            int numRows = 0;

            if(podatki.size() > 0)
                numRows = db.update(table, podatki, where, null);
            else
                Log.w(TAG, "Database.updateData() - no data to insert");

            return numRows;
        } catch (Exception e) {
            GeneralLib.reportCrash(e, null);
            return 0;
        }
    }//updateData

    /**
     * Insert new row in DB
     *
     * @param table   name of table
     * @param podatki ContentValues values
     * @return id of new row or -1 if nothing inserted;
     */
    public long insertData(String table, ContentValues podatki) {
        try {
            long id = -1;

            if(podatki.size() > 0)
                id = db.insert(table, null, podatki);
            else
                Log.w(TAG, "Database.insertData() - no data to insert");

            return id;
        } catch (Exception e) {
            GeneralLib.reportCrash(e, null);
            return -1;
        }
    }//insertData

    /**
     * Convert HashMap<String, String> to ContentValues
     * @param podatki data to convert
     * @return converted ContentValues
     */
    public ContentValues convertToValues(HashMap<String, String> podatki){
        ContentValues values = new ContentValues();
        for (Map.Entry<String, String> entry : podatki.entrySet()){
            if (entry.getKey() != null || entry.getValue() != null)
                values.put(entry.getKey(), entry.getValue());
            else
                Log.w(TAG, "Database.convertToValues() - null value in pair: " + entry.getKey() + " = " + entry.getValue());
        }
        return values;
    }

    /**
     * Convert String[][] to ContentValues
     * @param podatki data to convert - eg {{ "name_of_column", "value" },...}
     * @return converted ContentValues
     */
    public ContentValues convertToValues(String[][] podatki){
        ContentValues values = new ContentValues();
        for (String[] pair : podatki){
            if (pair[0] != null || pair[1] != null)
                values.put(pair[0], pair[1]);
            else
                Log.w(TAG, "Database.convertToValues() - null value in pair: " + pair[0] + " = " + pair[1]);
        }
        return values;
    }

    /**
     * Count rows in table
     *
     * @param table name of table to count rows
     * @param where where statement, null if not needed
     * @return number (int) of rows counted
     */
    public int countData(String table, String where) {
        try {
            Cursor c = db.query(table, null, where, null, null, null, null);

            //if there is no rows in table
            if (c.getCount() == 0) {
                c.close();
                return 0;
            }

            c.close();
            return c.getCount();
        } catch (Exception e) {
            GeneralLib.reportCrash(e, null);
            return 0;
        }
    }//countData

    /**
     * Get ID of last added row in DB
     *
     * @param table name of table
     * @return ID of last added row (int)
     */
    private int getLastID(String table) {
        Cursor cur;
        if (table.equals(""))
            cur = db.rawQuery("SELECT last_insert_rowid()", null);
        else
            cur = db.rawQuery("SELECT ROWID from " + table + " order by ROWID DESC limit 1", null);

        if (cur.getCount() != 0) {
            cur.moveToFirst();
            int ID = cur.getInt(0);
            cur.close();
            return ID;
        } else
            return 0;
    }//getLastID

    /**
     * Create ContentValues of device info, ready to insert them in DB
     * @return ContentValues of device info
     */
    private ContentValues[] getDeviceInfoValues() {
        String [][] pairsArr = {{"os_version", System.getProperty("os.version")+""},
                {"incremental", android.os.Build.VERSION.INCREMENTAL+""},
                {"sdk_int", android.os.Build.VERSION.SDK_INT+""},
                {"release", android.os.Build.VERSION.RELEASE+""},
                {"device", android.os.Build.DEVICE+""},
                {"model", android.os.Build.MODEL+""},
                {"product", android.os.Build.PRODUCT+""},
                {"brand", android.os.Build.BRAND+""},
                {"manufacturer", android.os.Build.MANUFACTURER+""},
                {"serial", android.os.Build.SERIAL+""},
                {"display", android.os.Build.DISPLAY+""},
                {"unknown", android.os.Build.UNKNOWN+""},
                {"hardware", android.os.Build.HARDWARE+""},
                {"id", android.os.Build.ID+""},
                {"user", android.os.Build.USER+""},
                {"host", android.os.Build.HOST+""},
                {"rooted", findBinary("su")+""},};


        ContentValues[] valuesArr = new ContentValues[pairsArr.length];
        for(int i = 0; i < pairsArr.length; i++){
            ContentValues values = new ContentValues();
            values.put("name", pairsArr[i][0]);
            values.put("value", pairsArr[i][1]);
            valuesArr[i] = values;
        }

        return valuesArr;
    }//end getDeviceSuperInfo
    
    /**
     * Find out if device is rooted
     * @param binaryName - string to find (usualy "su")
     * @return boolean - true for rooted, false for otherwise
     */
    private boolean findBinary(String binaryName) {
        boolean found = false;
        String[] places = { "/sbin/", "/system/bin/", "/system/xbin/",
                "/data/local/xbin/", "/data/local/bin/",
                "/system/sd/xbin/", "/system/bin/failsafe/", "/data/local/" };
        for (String where : places) {
            if (new File(where + binaryName).exists()) {
                found = true;

                break;
            }
        }
        return found;
    }
}

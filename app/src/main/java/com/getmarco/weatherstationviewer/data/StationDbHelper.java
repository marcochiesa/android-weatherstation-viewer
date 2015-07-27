package com.getmarco.weatherstationviewer.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.getmarco.weatherstationviewer.data.StationContract.StationEntry;
import com.getmarco.weatherstationviewer.data.StationContract.ConditionEntry;

/**
 * Created by marco on 7/26/15.
 */
public class StationDbHelper extends SQLiteOpenHelper {

    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 1;

    static final String DATABASE_NAME = "station.db";

    public StationDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create a table to hold information about the weather stations
        final String SQL_CREATE_STATION_TABLE = "CREATE TABLE " + StationEntry.TABLE_NAME + " ("
                + StationEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + StationEntry.COLUMN_TAG + " TEXT UNIQUE NOT NULL, "
                + StationEntry.COLUMN_NAME + " TEXT NOT NULL, "
                + StationEntry.COLUMN_TEMP_HIGH + " REAL NOT NULL, "
                + StationEntry.COLUMN_TEMP_LOW + " REAL NOT NULL, "
                + StationEntry.COLUMN_HUMIDITY_HIGH + " REAL NOT NULL, "
                + StationEntry.COLUMN_HUMIDITY_LOW + " REAL NOT NULL"
                + ");";

        // Create a table to hold the local conditions reported by the weather stations
        final String SQL_CREATE_CONDITION_TABLE = "CREATE TABLE " + ConditionEntry.TABLE_NAME + " ("
                + ConditionEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + ConditionEntry.COLUMN_STATION_KEY + " INTEGER NOT NULL, "
                + ConditionEntry.COLUMN_DATE + " INTEGER NOT NULL, "
                + ConditionEntry.COLUMN_TEMP + " REAL NOT NULL, "
                + ConditionEntry.COLUMN_HUMIDITY + " REAL NOT NULL, "
                + ConditionEntry.COLUMN_LATITUDE + " REAL NOT NULL, "
                + ConditionEntry.COLUMN_LONGITUDE + " REAL NOT NULL, "
                + "FOREIGN KEY (" + ConditionEntry.COLUMN_STATION_KEY + ") "
                + "REFERENCES " + StationEntry.TABLE_NAME + " (" + StationEntry._ID + ")"
                + ");";

        db.execSQL(SQL_CREATE_STATION_TABLE);
        db.execSQL(SQL_CREATE_CONDITION_TABLE);

        // temporary dummy list of weather stations for testing
        insertDummyData(db);
    }

    private void insertDummyData(SQLiteDatabase db) {
        final String INSERT_STATIONS = "INSERT INTO " + StationEntry.TABLE_NAME + " ("
                + StationEntry.COLUMN_TAG + ", "
                + StationEntry.COLUMN_NAME + ", "
                + StationEntry.COLUMN_TEMP_HIGH + ", "
                + StationEntry.COLUMN_TEMP_LOW + ", "
                + StationEntry.COLUMN_HUMIDITY_HIGH + ", "
                + StationEntry.COLUMN_HUMIDITY_LOW + ", "
                + ") VALUES "
                + "('test_station_1', 'Test Station 1', 75.0, 68.0, 70.0, 50.0), "
                + "('test_station_2', 'Test Station 2', 75.0, 68.0, 70.0, 50.0), "
                + "('test_station_3', 'Test Station 3', 75.0, 68.0, 70.0, 50.0), "
                + "('test_station_4', 'Test Station 4', 75.0, 68.0, 70.0, 50.0), "
                + ";";
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // For initial simplicity, discard the data and start over. This only
        // fires if you change the version number for your database. It does
        // NOT depend on the version number for your application.
        db.execSQL("DROP TABLE IF EXISTS " + StationEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + ConditionEntry.TABLE_NAME);
        onCreate(db);
    }
}

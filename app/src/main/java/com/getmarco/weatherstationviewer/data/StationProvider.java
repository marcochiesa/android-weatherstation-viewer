package com.getmarco.weatherstationviewer.data;

import android.annotation.TargetApi;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

/**
 * Created by marco on 7/27/15.
 */
public class StationProvider extends ContentProvider {

    private static final UriMatcher uriMatcher = buildUriMatcher();
    private StationDbHelper dbHelper;

    // list all stations, insert/update station, delete one/all station(s), get one station?
    private static final int STATION = 100;
    // insert/update condition, delete one/all condition(s)
    private static final int CONDITION = 300;
    private static final int CONDITION_AT_STATION = 301;
    private static final int LATEST_CONDITION_AT_STATION = 302;

    private static final SQLiteQueryBuilder conditionAtStationQueryBuilder;
    static{
        conditionAtStationQueryBuilder = new SQLiteQueryBuilder();

        //condition INNER JOIN station ON condition.station_id = station._id
        conditionAtStationQueryBuilder.setTables(
                StationContract.ConditionEntry.TABLE_NAME + " INNER JOIN " +
                        StationContract.StationEntry.TABLE_NAME +
                        " ON " + StationContract.ConditionEntry.TABLE_NAME +
                        "." + StationContract.ConditionEntry.COLUMN_STATION_KEY +
                        " = " + StationContract.StationEntry.TABLE_NAME +
                        "." + StationContract.StationEntry._ID);
    }

    //station.tag = ?
    private static final String stationTagSelection =
            StationContract.StationEntry.TABLE_NAME+
                    "." + StationContract.StationEntry.COLUMN_TAG + " = ? ";

    //station.tag = ? AND date >= ?
    private static final String stationTagWithStartDateSelection =
            StationContract.StationEntry.TABLE_NAME+
                    "." + StationContract.StationEntry.COLUMN_TAG + " = ? AND " +
                    StationContract.ConditionEntry.COLUMN_DATE + " >= ? ";

    private Cursor getConditionAtStation(Uri uri, String[] projection, String sortOrder) {
        String station = StationContract.ConditionEntry.getStationFromUri(uri);
        long startDate = StationContract.ConditionEntry.getStartDateFromUri(uri);

        String[] selectionArgs;
        String selection;

        if (startDate == 0) {
            selection = stationTagSelection;
            selectionArgs = new String[]{station};
        } else {
            selection = stationTagWithStartDateSelection;
            selectionArgs = new String[]{station, Long.toString(startDate)};
        }

        return conditionAtStationQueryBuilder.query(dbHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }

    private Cursor getLatestConditionAtStation(Uri uri, String[] projection) {
        String station = StationContract.ConditionEntry.getStationFromUri(uri);
        String sortOrder = StationContract.ConditionEntry._ID + " DESC";
        String limit = "1";

        return conditionAtStationQueryBuilder.query(dbHelper.getReadableDatabase(),
                projection,
                stationTagSelection,
                new String[]{station},
                null,
                null,
                sortOrder,
                limit
        );
    }

    @Override
    public boolean onCreate() {
        dbHelper = new StationDbHelper(getContext());
        return true;
    }

    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH); //code to return for root URI

        final String authority = StationContract.CONTENT_AUTHORITY;
        // Paths added to UriMatcher have corresponding code to return when a match is found
        matcher.addURI(authority, StationContract.PATH_STATION, STATION);
        matcher.addURI(authority, StationContract.PATH_CONDITION, CONDITION);
        matcher.addURI(authority, StationContract.PATH_CONDITION + "/*", CONDITION_AT_STATION);
        matcher.addURI(authority, StationContract.PATH_CONDITION + "/*/" + StationContract.ConditionEntry.PATH_LATEST,
                LATEST_CONDITION_AT_STATION);

        return matcher;
    }

    @Override
    public String getType(Uri uri) {

        // Use the Uri Matcher to determine what kind of URI this is.
        final int match = uriMatcher.match(uri);

        switch (match) {
            // Student: Uncomment and fill out these two cases
            case LATEST_CONDITION_AT_STATION:
                return StationContract.ConditionEntry.CONTENT_ITEM_TYPE;
            case CONDITION_AT_STATION:
                return StationContract.ConditionEntry.CONTENT_TYPE;
            case CONDITION:
                return StationContract.ConditionEntry.CONTENT_TYPE;
            case STATION:
                return StationContract.StationEntry.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        // Query the database based on the type of request the URI corresponds to
        Cursor retCursor;
        switch (uriMatcher.match(uri)) {
            // "condition/*/latest"
            case LATEST_CONDITION_AT_STATION:
                retCursor = getLatestConditionAtStation(uri, projection);
                break;
            // "condition/*"
            case CONDITION_AT_STATION:
                retCursor = getConditionAtStation(uri, projection, sortOrder);
                break;
            // "condition"
            case CONDITION:
                retCursor = dbHelper.getReadableDatabase().query(
                        StationContract.ConditionEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            // "station"
            case STATION:
                retCursor = dbHelper.getReadableDatabase().query(
                        StationContract.StationEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        final int match = uriMatcher.match(uri);
        Uri returnUri;
        long _id;

        switch (match) {
            case STATION:
                _id = db.insert(StationContract.StationEntry.TABLE_NAME, null, values);
                if ( _id > 0 )
                    returnUri = StationContract.StationEntry.buildStationUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            case CONDITION:
                _id = db.insert(StationContract.ConditionEntry.TABLE_NAME, null, values);
                if ( _id > 0 )
                    returnUri = StationContract.ConditionEntry.buildConditionUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        final int match = uriMatcher.match(uri);
        int rowsUpdated;

        switch (match) {
            case STATION:
                rowsUpdated = db.update(StationContract.StationEntry.TABLE_NAME, values, selection,
                        selectionArgs);
                break;
            case CONDITION:
                rowsUpdated = db.update(StationContract.ConditionEntry.TABLE_NAME, values, selection,
                        selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        final int match = uriMatcher.match(uri);
        int rowsDeleted;
        // this makes delete all rows return the number of rows deleted
        if (selection == null) selection = "1";
        switch (match) {
            case CONDITION:
                rowsDeleted = db.delete(
                        StationContract.ConditionEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case STATION:
                rowsDeleted = db.delete(
                        StationContract.StationEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        // Because a null deletes all rows
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    // Used for unit test clean-up
    @Override
    @TargetApi(11)
    public void shutdown() {
        dbHelper.close();
        super.shutdown();
    }
}

package com.getmarco.weatherstationviewer.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by marco on 7/26/15.
 */
public class StationContract {

    // The "Content authority" is a name for the entire content provider
    public static final String CONTENT_AUTHORITY = "com.getmarco.weatherstationviewer";

    // Base of all URI's which apps will use to contact the content provider.
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    // Paths (appended to base content URI for URI's)
    public static final String PATH_STATION = "station";
    public static final String PATH_CONDITION = "condition";

    /**
     * Table contents of station table
     */
    public static final class StationEntry implements BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_STATION).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_STATION;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_STATION;

        public static final String TABLE_NAME = "station";

        // Tag configured for the individual weather station
        public static final String COLUMN_TAG = "tag";
        // Friendly name for the station
        public static final String COLUMN_NAME = "name";
        // The high temperature threshold for notifications
        public static final String COLUMN_TEMP_HIGH = "temp_high";
        // The low temperature threshold for notifications
        public static final String COLUMN_TEMP_LOW = "temp_low";
        // The high humidity threshold for notifications
        public static final String COLUMN_HUMIDITY_HIGH = "humidity_high";
        // The low humidity threshold for notifications
        public static final String COLUMN_HUMIDITY_LOW = "humidity_low";

        public static Uri buildStationUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }

    /**
     * Table contents of condition table (records of local conditions reported from weather stations).
     */
    public static final class ConditionEntry implements BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_CONDITION).build();

        public static final String PATH_LATEST = "latest";

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_CONDITION;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_CONDITION;

        public static final String TABLE_NAME = "condition";

        // Column with the foreign key into the station table.
        public static final String COLUMN_STATION_KEY = "station_id";
        // Date, stored as long in milliseconds since the epoch
        public static final String COLUMN_DATE = "date";
        public static final String COLUMN_TEMP = "temp";
        public static final String COLUMN_HUMIDITY = "humidity";
        // The reported latitude
        public static final String COLUMN_LATITUDE = "latitude";
        // The reported longitude
        public static final String COLUMN_LONGITUDE = "longitude";

        public static Uri buildConditionUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static Uri buildConditionAtStation(String stationTag) {
            return CONTENT_URI.buildUpon().appendPath(stationTag).build();
        }

        public static Uri buildConditionAtStationWithStartDate(
                String stationTag, long startDate) {
            return CONTENT_URI.buildUpon().appendPath(stationTag)
                    .appendQueryParameter(COLUMN_DATE, Long.toString(startDate)).build();
        }

        public static Uri buildLatestConditionAtStation(String stationTag) {
            return CONTENT_URI.buildUpon().appendPath(stationTag).appendPath(PATH_LATEST).build();
        }

        public static String getStationFromUri(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        public static long getStartDateFromUri(Uri uri) {
            String dateString = uri.getQueryParameter(COLUMN_DATE);
            if (dateString != null && dateString.length() > 0)
                return Long.parseLong(dateString);
            else
                return 0;
        }
    }
}

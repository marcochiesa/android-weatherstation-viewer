/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.getmarco.weatherstationviewer.gcm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.getmarco.weatherstationviewer.R;
import com.getmarco.weatherstationviewer.StationListActivity;
import com.getmarco.weatherstationviewer.Utility;
import com.getmarco.weatherstationviewer.data.StationContract;
import com.google.android.gms.gcm.GcmListenerService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class StationGcmListenerService extends GcmListenerService {

    public static final String STATION_TAG = "tag";
    public static final String CONDITION_TEMP = "temperature";
    public static final String CONDITION_HUMIDITY = "humidity";
    public static final String CONDITION_DATE = "date";
    public static final String CONDITION_LAT = "latitude";
    public static final String CONDITION_LONG = "longitude";
    private static final String LOG_TAG = StationGcmListenerService.class.getSimpleName();

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(String from, Bundle data) {
        String message = data.getString("message");
        Log.d(LOG_TAG, "From: " + from);
        Log.d(LOG_TAG, "Message: " + message);

        /**
         * Production applications would usually process the message here.
         * Eg: - Syncing with server.
         *     - Store message in local database.
         *     - Update UI.
         */

        /**
         * In some cases it may be useful to show a notification indicating to the user
         * that a message was received.
         */
        sendNotification(message);

        try {
            JSONObject json = new JSONObject(message);
            String tag = json.getString(STATION_TAG);
            long stationId = addStation(tag);
            double temp = json.getDouble(CONDITION_TEMP);
            double humidity = json.getDouble(CONDITION_HUMIDITY);
            double latitude = json.getDouble(CONDITION_LAT);
            double longitude = json.getDouble(CONDITION_LONG);
            String dateString = json.getString(CONDITION_DATE);
            Date date = Utility.parseDateDb(dateString);

            ContentValues conditionValues = new ContentValues();
            conditionValues.put(StationContract.ConditionEntry.COLUMN_STATION_KEY, stationId);
            conditionValues.put(StationContract.ConditionEntry.COLUMN_TEMP, temp);
            conditionValues.put(StationContract.ConditionEntry.COLUMN_HUMIDITY, humidity);
            conditionValues.put(StationContract.ConditionEntry.COLUMN_LATITUDE, latitude);
            conditionValues.put(StationContract.ConditionEntry.COLUMN_LONGITUDE, longitude);
            conditionValues.put(StationContract.ConditionEntry.COLUMN_DATE, date.getTime());

            getContentResolver().insert(StationContract.ConditionEntry.CONTENT_URI, conditionValues);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "error parsing GCM message JSON", e);
        }
    }
    // [END receive_message]

    /**
     * Create and show a simple notification containing the received GCM message.
     *
     * @param message GCM message received.
     */
    private void sendNotification(String message) {
        Intent intent = new Intent(this, StationListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("GCM Message")
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }

    /**
     * Helper method to handle insertion of a station in the database if it doesn't already exist.
     *
     * @param stationTag the station
     * @return the row ID of the station (new or existing)
     */
    long addStation(String stationTag) {
        long stationId;

        // First, check if a station with this tag exists in the db
        Cursor stationCursor = getContentResolver().query(
                StationContract.StationEntry.CONTENT_URI,
                new String[]{StationContract.StationEntry._ID},
                StationContract.StationEntry.COLUMN_TAG + " = ?",
                new String[]{stationTag},
                null);

        if (stationCursor.moveToFirst()) {
            int stationIdIndex = stationCursor.getColumnIndex(StationContract.StationEntry._ID);
            stationId = stationCursor.getLong(stationIdIndex);
        } else {
            ContentValues locationValues = new ContentValues();
            locationValues.put(StationContract.StationEntry.COLUMN_TAG, stationTag);
            locationValues.put(StationContract.StationEntry.COLUMN_NAME, stationTag);
            locationValues.put(StationContract.StationEntry.COLUMN_TEMP_HIGH, 0.0);
            locationValues.put(StationContract.StationEntry.COLUMN_TEMP_LOW, 0.0);
            locationValues.put(StationContract.StationEntry.COLUMN_HUMIDITY_HIGH, 0.0);
            locationValues.put(StationContract.StationEntry.COLUMN_HUMIDITY_LOW, 0.0);

            // Finally, insert location data into the database.
            Uri insertedUri = getContentResolver().insert(StationContract.StationEntry.CONTENT_URI,
                    locationValues);

            // The resulting URI contains the ID for the row.  Extract the locationId from the Uri.
            stationId = ContentUris.parseId(insertedUri);
        }

        stationCursor.close();
        // Wait, that worked?  Yes!
        return stationId;
    }
}

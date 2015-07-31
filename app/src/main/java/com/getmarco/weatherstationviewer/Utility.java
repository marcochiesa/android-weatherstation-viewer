package com.getmarco.weatherstationviewer;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.text.format.Time;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by marco on 7/27/15 from example code for Udacity class ud853 - Developing Android Apps
 */
public class Utility {

    public static final int MILLIS_IN_SEC = 1000;
    public static final int MILLIS_IN_MIN = 1000 * 60;
    public static final int MILLIS_IN_HOUR = 1000 * 60 * 60;
    public static final String DATEFORMAT_DB_STYLE = "yyyy-MM-dd HH:mm:ss";
    public static final DateFormat DATEFORMAT_DB = new SimpleDateFormat(DATEFORMAT_DB_STYLE);

    public static boolean isMetric(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_units_key), context.getString(R.string.pref_units_metric)).equals(context.getString(R.string.pref_units_metric));
    }

    public static String formatTemperature(Context context, double temperature) {
        // Data stored in Celsius by default.  If user prefers to see in Fahrenheit, convert
        // the values here.
        if (!isMetric(context)) {
            temperature = (temperature * 1.8) + 32;
        }

        // For presentation, assume the user doesn't care about hundredths of a degree.
        return String.format(context.getString(R.string.format_temperature), temperature);
    }

    public static String formatDateDb(Date date) {
        return DATEFORMAT_DB.format(date);
    }

    public static Date parseDateDb(String dateString) {
        Date date = null;

        try {
            return DATEFORMAT_DB.parse(dateString);
        } catch (java.text.ParseException e) {
            e.printStackTrace();
        }

        return date;
    }

    /**
     * Helper method to convert the database representation of the date into something friendly to
     * display to users.
     *
     * @param context Context to use for resource localization
     * @param dateInMillis The date in milliseconds
     * @return a user-friendly representation of the date.
     */
    public static String getFriendlyDayString(Context context, long dateInMillis) {
        // The day string for forecast uses the following logic:
        // For today: "Today, June 8"
        // For tomorrow:  "Tomorrow"
        // For the next 5 days: "Wednesday" (just the day name)
        // For all days after that: "Mon Jun 8"

        Time time = new Time();
        time.setToNow();
        long currentTime = System.currentTimeMillis();
        int julianDay = Time.getJulianDay(dateInMillis, time.gmtoff);
        int currentJulianDay = Time.getJulianDay(currentTime, time.gmtoff);

        if (julianDay == currentJulianDay) {
            long diff = currentTime - dateInMillis;
            if (diff <= MILLIS_IN_SEC) {
                return context.getString(R.string.seconds_ago, "1");
            } else if (diff < MILLIS_IN_MIN) {
                return context.getString(R.string.seconds_ago, diff / MILLIS_IN_SEC);
            } else if (diff < MILLIS_IN_HOUR) {
                return context.getString(R.string.minutes_ago, diff / MILLIS_IN_MIN);
            } else {
                return context.getString(R.string.hours_ago, diff / MILLIS_IN_HOUR);
            }
        } else if (julianDay > currentJulianDay - 7 && julianDay < currentJulianDay + 7) {
            // If the input date is less than a week in the past or future, just return the day name.
            return getDayName(context, dateInMillis);
        } else {
            // Otherwise, use the form "Mon Jun 3"
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortenedDateFormat.format(dateInMillis);
        }
    }

    /**
     * Given a date, returns just the name to use for that day.
     *
     * @param context Context to use for resource localization
     * @param dateInMillis The date in milliseconds
     * @return "Yesterday", "Monday", etc.
     */
    public static String getDayName(Context context, long dateInMillis) {
        // If the date is today, yesterday, or tomorrow then return the localized version of
        // "Today", "Yesterday", or "Tomorrow" instead of the actual day name.

        Time t = new Time();
        t.setToNow();
        int julianDay = Time.getJulianDay(dateInMillis, t.gmtoff);
        int currentJulianDay = Time.getJulianDay(System.currentTimeMillis(), t.gmtoff);
        if (julianDay == currentJulianDay) {
            return context.getString(R.string.today);
        } else if (julianDay == currentJulianDay - 1) {
            return context.getString(R.string.yesterday);
        } else if (julianDay == currentJulianDay + 1) {
            return context.getString(R.string.tomorrow);
        } else {
            // just the day of the week (ex: "Wednesday")
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE");
            return dayFormat.format(dateInMillis);
        }
    }

    /**
     * Converts date to the format "Month day", e.g "June 24".
     * @param context Context to use for resource localization
     * @param dateInMillis The date in millis
     * @return The day in the form of a string formatted "December 6"
     */
    public static String getFormattedMonthDay(Context context, long dateInMillis ) {
        SimpleDateFormat monthDayFormat = new SimpleDateFormat("MMMM dd");
        return monthDayFormat.format(dateInMillis);
    }

    /**
     * Returns true if the network is available or about to become available.
     *
     * @param c Context used to get the ConnectivityManager
     * @return true if the network is available
     */
    static public boolean isNetworkAvailable(Context c) {
        ConnectivityManager cm =
                (ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }
}

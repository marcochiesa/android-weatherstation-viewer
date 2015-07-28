package com.getmarco.weatherstationviewer;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.getmarco.weatherstationviewer.data.StationContract;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

/**
 * A fragment representing a single Station detail screen.
 * This fragment is either contained in a {@link StationListActivity}
 * in two-pane mode (on tablets) or a {@link StationDetailActivity}
 * on handsets.
 */
public class StationDetailFragment extends Fragment {
    public static final String LOG_TAG = StationDetailFragment.class.getSimpleName();
    /**
     * Fragment argument representing the (latest) condition URI that this fragment represents.
     */
    static final String DETAIL_URI = "URI";

    private static final String CONDITION_SHARE_HASHTAG = " #WeatherStaionApp";

    private ShareActionProvider shareActionProvider;
    private String conditionAtStation;
    private Uri latestUri;
    private Cursor latestCursor;

    private LoaderManager.LoaderCallbacks<Cursor> latestLoaderCallbacks = new LatestLoaderCallbacks();
    private LoaderManager.LoaderCallbacks<Cursor> conditionsLoaderCallbacks = new ConditionsLoaderCallbacks();

    private static final int LATEST_LOADER = 0;
    private static final int CONDITIONS_LOADER = 1;

    private ImageView mIconView;
    private TextView tempView;
    private TextView humidityView;
    private TextView locationView;
    private TextView dateView;
    private GraphView graphView;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public StationDetailFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Bundle arguments = getArguments();
        if (arguments != null) {
            latestUri = arguments.getParcelable(StationDetailFragment.DETAIL_URI);
        }

        View rootView = inflater.inflate(R.layout.fragment_station_detail, container, false);
        tempView = (TextView)rootView.findViewById(R.id.detail_temp_textview);
        humidityView = (TextView)rootView.findViewById(R.id.detail_humidity_textview);
        locationView = (TextView)rootView.findViewById(R.id.detail_location_textview);
        dateView = (TextView)rootView.findViewById(R.id.detail_date_textview);
        graphView = (GraphView)rootView.findViewById(R.id.graph);

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.detailfragment, menu);

        // Retrieve the share menu item
        MenuItem menuItem = menu.findItem(R.id.action_share);

        // Get the provider and hold onto it to set/change the share intent.
        shareActionProvider = (ShareActionProvider)MenuItemCompat.getActionProvider(menuItem);

        // If onLoadFinished happens before this, we can go ahead and set the share intent now.
        if (conditionAtStation != null) {
            shareActionProvider.setShareIntent(createShareConditionIntent());
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_map) {
            openLocationInMap();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void openLocationInMap() {
        // Using the URI scheme for showing a location found on a map.  This super-handy
        // intent can is detailed in the "Common Intents" page of Android's developer site:
        // http://developer.android.com/guide/components/intents-common.html#Maps
        if (latestCursor == null)
            return;
        latestCursor.moveToPosition(0);
        String posLat = latestCursor.getString(latestCursor.getColumnIndex(StationContract.ConditionEntry.COLUMN_LATITUDE));
        String posLong = latestCursor.getString(latestCursor.getColumnIndex(StationContract.ConditionEntry.COLUMN_LONGITUDE));
        Uri geoLocation = Uri.parse("geo:" + posLat + "," + posLong);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(geoLocation);

        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Log.d(LOG_TAG, "Couldn't call " + geoLocation.toString() + ", no receiving apps installed!");
        }
    }

    private Intent createShareConditionIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, conditionAtStation + CONDITION_SHARE_HASHTAG);
        return shareIntent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(LATEST_LOADER, null, latestLoaderCallbacks);
        getLoaderManager().initLoader(CONDITIONS_LOADER, null, conditionsLoaderCallbacks);
        super.onActivityCreated(savedInstanceState);
    }

    void onStationChanged(String newStationTag) {
        latestUri = StationContract.ConditionEntry.buildLatestConditionAtStation(newStationTag);
        getLoaderManager().restartLoader(LATEST_LOADER, null, latestLoaderCallbacks);
        getLoaderManager().restartLoader(CONDITIONS_LOADER, null, conditionsLoaderCallbacks);
    }

    private class ConditionsLoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            String stationTag = StationContract.ConditionEntry.getStationFromUri(latestUri);
            Uri uri = StationContract.ConditionEntry.buildConditionAtStation(stationTag);

            String sortOrder = StationContract.ConditionEntry._ID + " ASC";
            // Now create and return a CursorLoader that will take care of
            // creating a Cursor for the data being displayed.
            return new CursorLoader(
                    getActivity(),
                    uri,
                    null,
                    null,
                    null,
                    sortOrder
            );
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            LineGraphSeries<DataPoint> tempSeries = new LineGraphSeries<>(new DataPoint[] {});
            LineGraphSeries<DataPoint> humiditySeries = new LineGraphSeries<>(new DataPoint[] {});
            int n = 0;
            while (data.moveToNext()) {
                double temp = data.getDouble(data.getColumnIndex(StationContract.ConditionEntry.COLUMN_TEMP));
                double humidity = data.getDouble(data.getColumnIndex(StationContract.ConditionEntry.COLUMN_HUMIDITY));
                tempSeries.appendData(new DataPoint(n, temp), true, 100);
                humiditySeries.appendData(new DataPoint(n, humidity), true, 100);
                n++;
            }

            tempSeries.setTitle("Temp");
            tempSeries.setColor(Color.RED);
            tempSeries.setDrawDataPoints(true);
            tempSeries.setDataPointsRadius(10);
            tempSeries.setThickness(8);

            humiditySeries.setTitle("Humidity");
            humiditySeries.setColor(Color.BLUE);
            humiditySeries.setDrawDataPoints(true);
            humiditySeries.setDataPointsRadius(10);
            humiditySeries.setThickness(8);

            graphView.addSeries(tempSeries);
            graphView.addSeries(humiditySeries);
            graphView.setTitle("Conditions");
            graphView.getLegendRenderer().setVisible(true);
            graphView.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
        }
    }

    private class LatestLoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            //Log.v(LOG_TAG, "In onCreateLoader");
            if (latestUri == null)
                return null;

            // Now create and return a CursorLoader that will take care of
            // creating a Cursor for the data being displayed.
            return new CursorLoader(
                    getActivity(),
                    latestUri,
                    null,
                    null,
                    null,
                    null
            );
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

            latestCursor = data;
            if (latestCursor == null || !latestCursor.moveToFirst())
                return;

            double temp = latestCursor.getDouble(latestCursor.getColumnIndex(StationContract.ConditionEntry.COLUMN_TEMP));
            String tempString = Utility.formatTemperature(getActivity(), temp);
            tempView.setText(tempString);
            tempView.setContentDescription(getString(R.string.a11y_temp, tempString));

            float humidity = latestCursor.getFloat(latestCursor.getColumnIndex(StationContract.ConditionEntry.COLUMN_HUMIDITY));
            humidityView.setText(getString(R.string.format_humidity, humidity));
            humidityView.setContentDescription(getString(R.string.a11y_humidity, humidityView.getText()));

            String posLat = latestCursor.getString(latestCursor.getColumnIndex(StationContract.ConditionEntry.COLUMN_LATITUDE));
            String posLong = latestCursor.getString(latestCursor.getColumnIndex(StationContract.ConditionEntry.COLUMN_LONGITUDE));
            locationView.setText(posLat + ", " + posLong);

            long date = latestCursor.getLong(latestCursor.getColumnIndex(StationContract.ConditionEntry.COLUMN_DATE));
            String friendlyDate = Utility.getFriendlyDayString(getActivity(), date);
            dateView.setText(friendlyDate);

            String name = latestCursor.getString(latestCursor.getColumnIndex(StationContract.StationEntry.COLUMN_NAME));
            String tag = latestCursor.getString(latestCursor.getColumnIndex(StationContract.StationEntry.COLUMN_TAG));
            String station = name != null && name.length() > 0 ? name : tag;
            boolean isMetric = Utility.isMetric(getActivity());
            String tempUnit = isMetric ? "(C)" : "(F)";

            // for the share intent
            conditionAtStation = String.format("Station: %s - temp: %s %s, humidity: %s, time: %s",
                    station, tempString, tempUnit, humidityView.getText(), friendlyDate);

            // If onCreateOptionsMenu has already happened, we need to update the share intent now.
            if (shareActionProvider != null) {
                shareActionProvider.setShareIntent(createShareConditionIntent());
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            latestCursor = null;
        }
    }
}

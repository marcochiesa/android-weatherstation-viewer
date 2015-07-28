package com.getmarco.weatherstationviewer;

import android.content.Intent;
import android.database.Cursor;
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
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

/**
 * A fragment representing a single Station detail screen.
 * This fragment is either contained in a {@link StationListActivity}
 * in two-pane mode (on tablets) or a {@link StationDetailActivity}
 * on handsets.
 */
public class StationDetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String LOG_TAG = StationDetailFragment.class.getSimpleName();
    /**
     * Fragment argument representing the (latest) condition URI that this fragment represents.
     */
    static final String DETAIL_URI = "URI";

    private static final String CONDITION_SHARE_HASHTAG = " #WeatherStaionApp";

    private ShareActionProvider shareActionProvider;
    private String conditionAtStation;
    private Uri uri;
    private Cursor latestCursor;

    private static final int DETAIL_LOADER = 0;

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
            uri = arguments.getParcelable(StationDetailFragment.DETAIL_URI);
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
        getLoaderManager().initLoader(DETAIL_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    void onStationChanged(String newStationTag) {
        uri = StationContract.ConditionEntry.buildLatestConditionAtStation(newStationTag);
        getLoaderManager().restartLoader(DETAIL_LOADER, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        //Log.v(LOG_TAG, "In onCreateLoader");
        if (uri == null)
            return null;

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(
                getActivity(),
                uri,
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

        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(new DataPoint[] {
                new DataPoint(0, 1),
                new DataPoint(1, 5),
                new DataPoint(2, 3),
                new DataPoint(3, 2),
                new DataPoint(4, 6)
        });
        graphView.addSeries(series);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        latestCursor = null;
    }
}

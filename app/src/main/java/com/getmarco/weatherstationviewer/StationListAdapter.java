package com.getmarco.weatherstationviewer;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.getmarco.weatherstationviewer.data.StationContract;

/**
 * Created by marco on 7/27/15.
 */
public class StationListAdapter extends CursorAdapter {

    public static class ViewHolder {
        public final TextView name;

        public ViewHolder(View view) {
            name = (TextView) view.findViewById(android.R.id.text1);
        }
    }

    public StationListAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_activated_1, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();
        String tag = cursor.getString(cursor.getColumnIndex(StationContract.StationEntry.COLUMN_TAG));
        String name = cursor.getString(cursor.getColumnIndex(StationContract.StationEntry.COLUMN_NAME));
        viewHolder.name.setText(name != null && name.length() > 0 ? name : tag);
    }
}

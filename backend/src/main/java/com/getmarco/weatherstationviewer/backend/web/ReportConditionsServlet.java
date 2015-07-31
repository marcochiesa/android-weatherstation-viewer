package com.getmarco.weatherstationviewer.backend.web;

import com.getmarco.weatherstationviewer.backend.MessagingEndpoint;
import com.google.appengine.labs.repackaged.org.json.JSONException;
import com.google.appengine.labs.repackaged.org.json.JSONStringer;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by marco on 7/29/15.
 */
public class ReportConditionsServlet extends HttpServlet {

    public static final String PARAM_STATION_TAG = "tag";
    public static final String PARAM_CONDITION_TEMP = "temperature";
    public static final String PARAM_CONDITION_HUMIDITY = "humidity";
    public static final String PARAM_CONDITION_DATE = "date";
    public static final String PARAM_CONDITION_LAT = "latitude";
    public static final String PARAM_CONDITION_LONG = "longitude";

    public static final String DATEFORMAT_DB_STYLE = "yyyy-MM-dd HH:mm:ss";
    public static final DateFormat DATEFORMAT_DB = new SimpleDateFormat(DATEFORMAT_DB_STYLE);

    public static final String CONDITIONS_TOPIC = "conditions";

    private boolean hasText(String s) {
        return s != null && s.trim().length() > 0;
    }
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String stationTag = req.getParameter(PARAM_STATION_TAG);
        String temp = req.getParameter(PARAM_CONDITION_TEMP);
        String humidity = req.getParameter(PARAM_CONDITION_HUMIDITY);
        String latitude = req.getParameter(PARAM_CONDITION_LAT);
        String longitude = req.getParameter(PARAM_CONDITION_LONG);

        if (!hasText(stationTag)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "missing params");
            return;
        }

        String dateString = DATEFORMAT_DB.format(new Date());

        String jsonString = null;
        try {
            JSONStringer json = new JSONStringer();
            json.object();
            json.key(PARAM_STATION_TAG).value(stationTag);
            json.key(PARAM_CONDITION_TEMP).value(temp);
            json.key(PARAM_CONDITION_HUMIDITY).value(humidity);
            json.key(PARAM_CONDITION_DATE).value(dateString);
            json.key(PARAM_CONDITION_LAT).value(latitude);
            json.key(PARAM_CONDITION_LONG).value(longitude);
            json.endObject();
            jsonString = json.toString();
        } catch (JSONException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid json creation");
            return;
        }

        MessagingEndpoint endpoint = new MessagingEndpoint();
        endpoint.sendTopicMessage(CONDITIONS_TOPIC, jsonString);

        resp.setContentType("text/plain");
        resp.getWriter().println("OK");
    }
}

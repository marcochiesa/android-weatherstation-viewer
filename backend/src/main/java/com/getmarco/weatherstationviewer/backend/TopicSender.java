package com.getmarco.weatherstationviewer.backend;

import com.google.android.gcm.server.InvalidRequestException;
import com.google.android.gcm.server.Message;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import static com.google.android.gcm.server.Constants.GCM_SEND_ENDPOINT;
import static com.google.android.gcm.server.Constants.JSON_PAYLOAD;
import static com.google.android.gcm.server.Constants.PARAM_COLLAPSE_KEY;
import static com.google.android.gcm.server.Constants.PARAM_DELAY_WHILE_IDLE;
import static com.google.android.gcm.server.Constants.PARAM_TIME_TO_LIVE;

/**
 * Created by marco on 7/30/15.
 */
public class TopicSender extends com.google.android.gcm.server.Sender {

    /**
     * HTTP parameter for 'to'.
     */
    public static final String PARAM_TO = "to";

    public static final String TOKEN_MESSAGE_ID = "message_id";
    public static final String TOKEN_ERROR = "error";

    /**
     * Default constructor.
     *
     * @param key API key obtained through the Google API Console.
     */
    public TopicSender(String key) {
        super(key);
    }

    /**
     * Sends a topic message, retrying in case of unavailability.
     *
     * <p>
     * <strong>Note: </strong> this method uses exponential back-off to retry in
     * case of service unavailability and hence could block the calling thread
     * for many seconds.
     *
     * @param message message to be sent, including the device's registration id.
     * @param topic topic where the message will be sent.
     * @param retries number of retries in case of service unavailability errors.
     *
     * @return result of the request (see its javadoc for more details)
     *
     * @throws IllegalArgumentException if registrationId is {@literal null}.
     * @throws InvalidRequestException if GCM didn't returned a 200 or 503 status.
     * @throws IOException if message could not be sent.
     */
    public TopicResult sendTopic(Message message, String topic, int retries)
            throws IOException {
        int attempt = 0;
        TopicResult topicResult = null;
        int backoff = BACKOFF_INITIAL_DELAY;
        boolean tryAgain;
        do {
            attempt++;
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Attempt #" + attempt + " to send message " +
                        message + " to topic " + topic);
            }
            topicResult = sendTopicNoRetry(message, topic);
            tryAgain = topicResult == null && attempt <= retries;
            if (tryAgain) {
                int sleepTime = backoff / 2 + random.nextInt(backoff);
                sleep(sleepTime);
                if (2 * backoff < MAX_BACKOFF_DELAY) {
                    backoff *= 2;
                }
            }
        } while (tryAgain);
        if (topicResult == null) {
            throw new IOException("Could not send message after " + attempt +
                    " attempts");
        }
        return topicResult;
    }

    /**
     * Sends a topic message without retrying in case of service unavailability. See
     * {@link #sendTopic(Message, String, int)} for more info.
     *
     * @return result of the post, or {@literal null} if the GCM service was
     *         unavailable.
     *
     * @throws InvalidRequestException if GCM didn't returned a 200 or 503 status.
     * @throws IllegalArgumentException if registrationId is {@literal null}.
     */
    public TopicResult sendTopicNoRetry(Message message, String topic)
            throws IOException {
        if (topic == null) {
            throw new IllegalArgumentException("null message");
        }
        if (topic == null || "".equals(topic)) {
            throw new IllegalArgumentException("topic cannot be empty");
        }

        Map<Object, Object> jsonRequest = new HashMap<Object, Object>();
        setJsonField(jsonRequest, PARAM_TIME_TO_LIVE, message.getTimeToLive());
        setJsonField(jsonRequest, PARAM_COLLAPSE_KEY, message.getCollapseKey());
        setJsonField(jsonRequest, PARAM_DELAY_WHILE_IDLE, message.isDelayWhileIdle());
        jsonRequest.put(PARAM_TO, topic);
        Map<String, String> payload = message.getData();
        if (!payload.isEmpty()) {
            jsonRequest.put(JSON_PAYLOAD, payload);
        }
        String requestBody = JSONValue.toJSONString(jsonRequest);
        logger.finest("JSON request: " + requestBody);

        HttpURLConnection conn = null;
        String responseBody;
        try {
            conn = post(GCM_SEND_ENDPOINT, "application/json", requestBody);
            int status = conn.getResponseCode();
            if (status == 503) {
                logger.fine("GCM service is unavailable");
                return null;
            }
            if (status != 200) {
                responseBody = getString(conn.getErrorStream());
                logger.finest("JSON error response: " + responseBody);
                throw new InvalidRequestException(status, responseBody);
            }
            responseBody = getString(conn.getInputStream());
            logger.finest("JSON response: " + responseBody);
        } finally {
            if (conn != null)
                conn.disconnect();
        }

        JSONParser parser = new JSONParser();
        JSONObject jsonResponse;
        try {
            jsonResponse = (JSONObject)parser.parse(responseBody);
        } catch (Exception e) {
            throw newIoException(responseBody, e);
        }

        TopicResult topicResult = new TopicResult();
        Object ob = jsonResponse.get(TOKEN_MESSAGE_ID);
        if (ob != null)
            topicResult.setMessageId(ob.toString());
        ob = jsonResponse.get(TOKEN_ERROR);
        if (ob != null)
            topicResult.setError(ob.toString());
        return topicResult;
    }

    /**
     * Sets a JSON field, but only if the value is not {@literal null}.
     */
    protected void setJsonField(Map<Object, Object> json, String field, Object value) {
        if (value != null) {
            json.put(field, value);
        }
    }

    private IOException newIoException(String responseBody, Exception e) {
        // log exception, as IOException constructor that takes a message and cause
        // is only available on Java 6
        String msg = "Error parsing JSON response (" + responseBody + ")";
        logger.log(Level.WARNING, msg, e);
        return new IOException(msg + ":" + e);
    }

    void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static class TopicResult {

        private String messageId;
        private String error;

        public String getError() {
            return error;
        }
        public void setError(String error) {
            this.error = error;
        }

        public String getMessageId() {
            return messageId;
        }
        public void setMessageId(String messageId) {
            this.messageId = messageId;
        }
    }
}

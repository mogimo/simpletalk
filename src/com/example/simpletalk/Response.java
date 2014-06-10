package com.example.simpletalk;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

public class Response {
    private final static boolean DEBUG = BuildConfig.DEBUG;
    private final static String TAG = "SimpleTalk";
    private static final String dbFile = "response.txt";
    private static final int FORMAT_VERSION = 1;
    private static JSONArray mResponses = null;

    private static int SUPPORTIVE_RESPONSE_ID = 0;

    private static JSONArray getResponseData(Context context) {
        String data = Utils.loadAssetDB(context, dbFile);
        JSONArray responses = null;
        try {
            JSONObject root = new JSONObject(data);
            int format = root.getInt("format");
            if (format == FORMAT_VERSION) {
                responses = root.getJSONArray("response");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return responses;
    }

    private static JSONObject getResponse(int category, String variation) {
        if (mResponses != null) {
            try {
                // [{response data}, ..., {response data}]
                for (int i=0; i<mResponses.length(); i++) {
                    JSONObject response = mResponses.getJSONObject(i);
                    if (response.getInt("category") == category) {
                        String var = response.getString("variation");
                        if (var.equals(variation)) {
                            return response;
                        } else if (var.equals("")) {
                            return response;
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private static int getInt(Context context, int category, String variation, String tag) {
        if (mResponses == null) {
            mResponses = getResponseData(context);
        }

        int value = 0;
        try {
            JSONObject response = getResponse(category, variation);
            if (response != null) {
                value = response.getInt(tag);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return value;
    }

    private static String getString(Context context, int category, String variation, String tag) {
        if (mResponses == null) {
            mResponses = getResponseData(context);
        }

        String ret = null;
        try {
            JSONObject response = getResponse(category, variation);
            if (response != null) {
                JSONArray replies = response.getJSONArray(tag);
                int n = replies.length();
                int index = 0;
                if (n > 1) {
                    long time = SystemClock.elapsedRealtime();
                    index = (int) (time % n);   // random choice
                    if (DEBUG) Log.d(TAG,"magic="+time+" n="+n+" choose index="+index);
                }
                ret = replies.getString(index);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static String getSupportiveResponse(Context context) {
        return getReplyWord(context, SUPPORTIVE_RESPONSE_ID, "");
    }

    public static String getReplyWord(Context context, int category, String variation) {
        return getString(context, category, variation, "answer");
    }

    public static String getReplyWord(Context context, int category) {
        long time = SystemClock.elapsedRealtime();
        return getString(context, category, "", "answer");
    }

    public static int getIntonation(Context context, int category) {
        return getInt(context, category, "", "intonation");
    }

    public static int getAction(Context context, int category) {
        return getInt(context, category, "", "action");
    }
}

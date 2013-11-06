package com.example.simpletalk;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

public class Response {
    private static final String dbFile = "response.txt";
    private static final int FORMAT_VERSION = 1;
    private static JSONArray mResponses = null;

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

    private static JSONObject getResponse(int id) {
        if (mResponses != null) {
            try {
                // [{response data}, ..., {response data}]
                for (int i=0; i<mResponses.length(); i++) {
                    JSONObject response = mResponses.getJSONObject(i);
                    if (response.getInt("id") == id) {
                        return response;
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private static int getInt(Context context, int id, String tag) {
        if (mResponses == null) {
            mResponses = getResponseData(context);
        }

        int value = 0;
        try {
            JSONObject response = getResponse(id);
            if (response != null) {
                value = response.getInt(tag);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return value;
    }

    private static String getString(Context context, int id, String tag) {
        if (mResponses == null) {
            mResponses = getResponseData(context);
        }

        String ret = null;
        try {
            JSONObject response = getResponse(id);
            if (response != null) {
                ret = response.getString(tag);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static String getReplyWord(Context context, int id) {
        return getString(context, id, "reply");
    }

    public static int getIntonation(Context context, int id) {
        return getInt(context, id, "intonation");
    }

    public static int getAction(Context context, int id) {
        return getInt(context, id, "action");
    }
}

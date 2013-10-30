package com.example.simpletalk;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

public class Response {
    private static final String dbFile = "response.txt";
    private static String mData = null;

    // format version 1
    private static final int IDX_ID = 0;
    private static final int IDX_WORD = 1;
    private static final int IDX_INTONATION = 2;
    private static final int IDX_ACTION = 3;

    private static JSONArray getResponse(int id) {
        try {
            JSONObject root = new JSONObject(mData);
            int format = root.getInt("format");
            if (format == 1) {
                JSONArray responses = root.getJSONArray("response");
                // [[response data], ..., [response data]]
                return responses.getJSONArray(id);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static int getInt(int id, int index) {
        try {
            JSONArray response = getResponse(id);
            if (response != null) {
                // [id, word, intonation, action]
                return response.getInt(index);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static String getString(int id, int index) {
        try {
            JSONArray response = getResponse(id);
            if (response != null) {
                // [id, word, intonation, action]
                return response.getString(index);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getReplyWord(Context context, int id) {
        if (mData == null) {
            mData = Utils.loadAssetDB(context, dbFile);
        }
        return getString(id, IDX_WORD);
    }

    public static int getIntonation(Context context, int id) {
        if (mData == null) {
            mData = Utils.loadAssetDB(context, dbFile);
        }
        return getInt(id, IDX_INTONATION);
    }

    public static int getAction(Context context, int id) {
        if (mData == null) {
            mData = Utils.loadAssetDB(context, dbFile);
        }
        return getInt(id, IDX_ACTION);
    }
}

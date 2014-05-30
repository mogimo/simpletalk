package com.example.simpletalk;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

/**
 * Exact match phrase parser
 * @author S121206
 *
 */
public final class Greeting {
    private final static boolean DEBUG = BuildConfig.DEBUG;
    private final static String TAG = "SimpleTalk:Greeting";

    private static final String DB_FILE = "greeting.txt";
    /* greeting JSON data */
    private static String mData = null;

    private static final int FORMAT_VERSION = 2;

    @Deprecated
    /* not work any more */
     public static String getResponse(Context context, String word) {
         return null;
     }

    /*
     * return greeting message
     */
    public String greeting(Context context, List<SimpleToken> tokens) {
        int id = 0;
        for (SimpleToken token : tokens) {
            if ((id = getResponseId(context, token.getSurface())) != 0) {
                String str = Response.getReplyWord(context, id);
                int emotion = 0;
                if ((emotion = Response.getIntonation(context, id)) != 0) {
                    str = Utils.addEmotionTag(str, emotion);
                }
                return str;
            }
        }
        return null;
    }

    public String greeting(Context context, String word) {
        int category = 0;
        if ((category = getResponseId(context, word)) != 0) {
            String str = Response.getReplyWord(context, category);
            int emotion = 0;
            if ((emotion = Response.getIntonation(context, category)) != 0) {
                str = Utils.addEmotionTag(str, emotion);
            }
            return str;
        }
        return null;
    }

    /**
     * Return the response word for given greeting
     * @param context context
     * @param text the greeting word
     * @return the response id for the parameter
     */
     private int getResponseId(Context context, String sentence) {
         if (mData == null) {
             mData = Utils.loadAssetDB(context, DB_FILE);
         }

         int response = 0;
         boolean found = false;
         try {
             JSONObject root = new JSONObject(mData);
             int format = root.getInt("format");
             if (format == FORMAT_VERSION) {
                 JSONArray phrases = root.getJSONArray("phrases");
                 // [[phrase data], ..., [phrase data]]
                 for (int i=0; i<phrases.length(); i++) {
                     JSONObject phrase = phrases.getJSONObject(i);
                     // [[words], function, response]
                     JSONArray words = phrase.getJSONArray("words");
                     // [word, word, ..., word]
                     for (int k=0; k<words.length(); k++) {
                         if (sentence.contains(words.getString(k))) {
                             found = true;
                             break;
                         }
                     }
                     if (found) {
                         response = phrase.getInt("category");
                         break;
                     }
                 }
             }
         } catch (JSONException e) {
             e.printStackTrace();
         }
         return response;
     }
}

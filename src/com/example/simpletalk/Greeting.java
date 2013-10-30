package com.example.simpletalk;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

public final class Greeting {
    private static final String dbFile = "greeting.txt";
    private static String mData = null;

    // format version 1
    private static final int IDX_WORDS = 0;
    private static final int IDX_FUNCTION = 1;
    private static final int IDX_RESPONSE = 2;

    @Deprecated
     public static String getResponse(Context context, String word) {
         return null;
     }

    /*
     * Greeting
     */
    public static String greeting(Context context, List<SimpleToken> tokens) {
        int id = 0;
        for (SimpleToken token : tokens) {
            if ((id = Greeting.getResponseId(context, token.getSurface())) != 0) {
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

    /**
     * Return the response word for given greeting
     * @param context context
     * @param text the greeting word
     * @return the response id for the parameter
     */
     public static int getResponseId(Context context, String word) {
         if (mData == null) {
             mData = Utils.loadAssetDB(context, dbFile);
         }

         int response = 0;
         boolean found = false;
         try {
             JSONObject root = new JSONObject(mData);
             int format = root.getInt("format");
             if (format == 2) {
                 JSONArray phrases = root.getJSONArray("phrase");
                 // [[phrase data], ..., [phrase data]]
                 for (int i=0; i<phrases.length(); i++) {
                     JSONArray phrase = phrases.getJSONArray(i);
                     // [[words], function, response]
                     JSONArray words = phrase.getJSONArray(IDX_WORDS);
                     // [word, word, ..., word]
                     for (int k=0; k<words.length(); k++) {
                         if (words.getString(k).equals(word)) {
                             found = true;
                             break;
                         }
                     }
                     if (found) {
                         response = phrase.getInt(IDX_RESPONSE);
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

package com.example.simpletalk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

public class Utils {
    private final static boolean DEBUG = BuildConfig.DEBUG;
    private final static String TAG = "SimpleTalk";
    private final static String DEFAULT_LOCALE = "ja";

    private static String getCurrentLocale(Context context) {
        String locale = context.getResources().getConfiguration().locale.getLanguage();
        if (DEBUG) Log.d(TAG, "locale="+locale);
        return locale != null ? locale : DEFAULT_LOCALE;
    }

    public static String loadAssetDB(Context context, String filename) {
        AssetManager as = context.getResources().getAssets();
        try {
           InputStream input = as.open(getCurrentLocale(context) + "/" + filename);
           BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
           StringBuilder builder = new StringBuilder();
           String line;
           while (null != (line = reader.readLine())) {
               builder.append(line);
           }
           return builder.toString();
       } catch (IOException e) {
           e.printStackTrace();
       }
       return null;
    }

    public static final int EMOTION_NORMAL = 0;
    public static final int EMOTION_HAPPINESS = 1;
    public static final int EMOTION_ANGER = 2;
    public static final int EMOTION_SADNESS = 3;
    public static String addEmotionTag(String message, int emotion) {
        final String HAPPINESS = "happiness";
        final String ANGER = "anger";
        final String SADNESS = "sadness";
        final String[] emotions = {
                null, HAPPINESS, ANGER, SADNESS,
        };
        
        String str = emotions[emotion];
        if (str != null) {
            if (DEBUG) Log.d(TAG, "add emotion tag");
            return "<vtml_emotion category=\"" + str + "\" level=\"1\">" +
                    message + "</vtml_emotion>";
        }
        return message;
    }

    public static int levenshteinDistance(String px, String py) {
        int len1 = px.length(), len2 = py.length();
        int[][] row = new int[len1+1][len2+1];
        int i,j;
        int result;

        for (i=0; i<len1+1; i++) {
            row[i][0] = i;
        }
        for (i=0; i<len2+1; i++) {
            row[0][i] = i;
        }
        for (i=1; i<=len1; ++i) {
            for(j=1;j<=len2;++j) {
                row[i][j] = 
                    Math.min(Math.min(
                        (Integer)(row[i-1][j-1]) + ((px.substring(i-1,i).equals(py.substring(j-1,j)))?0:1) , // replace
                        (Integer)(row[i][j-1]) + 1), // delete
                        (Integer)(row[i-1][j]) + 1); // insert
            }
        }
        result = row[len1][len2];
        return result;

    }
}

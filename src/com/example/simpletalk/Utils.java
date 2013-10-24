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

    public static String loadAssetDB(Context context, String filename) {
        AssetManager as = context.getResources().getAssets();
        try {
           InputStream input = as.open(filename);
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
}

package com.example.simpletalk;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

public class Functions {
    private static final String DB_FILE = "functions.txt";
    private static final int FORMAT_VERSION = 3;

    private static final int COMMAND_DATE = 1;

    private String mData = null;
    private List<String> mTargets = new ArrayList<String>();

    private boolean searchSynonym(JSONObject phrase, String word) {
        boolean found = false;
        if (phrase == null || word == null) {
            return found;
        }
        try {
            JSONArray synonym = phrase.getJSONArray("synonym");
            for (int i=0; i<synonym.length(); i++) {
                if (word.equals(synonym.getString(i))) {
                    found = true;
                    break;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return found;
    }

    private boolean matchTargets(JSONObject function) {
        boolean match = false;
        if (function == null || mTargets.size() == 0) {
            return match;
        }
        try {
            JSONArray targets = function.getJSONArray("targets");
            if (targets.length() == 0) {
                return match;
            }
            match = true;
            for (int i=0; i<targets.length(); i++) {
                if (!mTargets.contains(targets.getString(i))) {
                    match = false;
                    break;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return match;
    }

    public int parse(Context context, List<SimpleToken> tokens) {
        int command = 0;
        if (mData == null) {
            mData = Utils.loadAssetDB(context, DB_FILE);
        }

        mTargets.clear();
        try {
            JSONObject root = new JSONObject(mData);
            int format = root.getInt("format");
            if (format == FORMAT_VERSION) {
                // search word in phrase array
                JSONArray phrases = root.getJSONArray("phrases");
                for (int i=0; i<phrases.length(); i++) {
                    JSONObject phrase = phrases.getJSONObject(i);
                    for (SimpleToken token : tokens) {
                        if (searchSynonym(phrase, token.getSurface())) {
                            mTargets.add(phrase.getString("target"));
                        }
                    }
                }
                // test to match function
                JSONArray functions = root.getJSONArray("function");
                for (int i=0; i<functions.length(); i++) {
                    JSONObject function = functions.getJSONObject(i);
                    if (matchTargets(function)) {
                        command = function.getInt("command");
                        break;
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return command;
    }
    
    public String answer(Context context, List<SimpleToken> tokens) {
        String answer = null;

        int command = parse(context, tokens);
        switch (command) {
            case COMMAND_DATE:
                Date date = new Date();
                Locale locale = context.getResources().getConfiguration().locale;
                SimpleDateFormat format = 
                    new SimpleDateFormat("yyyy'年'MM'月'dd'日'E'曜日'", locale);
                answer = format.format(date);
                break;
            default:
                answer = context.getString(R.string.please_again);
                break;
        }
        return answer;
    }
}

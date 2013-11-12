package com.example.simpletalk;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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

    private static final int COMMAND_SELF_INTRO = 1;
    private static final int COMMAND_DATE = 2;

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

    public JSONObject parse(Context context, List<SimpleToken> tokens) {
        JSONObject ret = null;
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
                            mTargets.add(phrase.getString("tag"));
                        }
                    }
                }
                // test to match function
                JSONArray functions = root.getJSONArray("function");
                for (int i=0; i<functions.length(); i++) {
                    JSONObject function = functions.getJSONObject(i);
                    if (matchTargets(function)) {
                        ret = function;
                        break;
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return ret;
    }

    private String getOption(JSONObject function) {
        String ret = null;
        if (function == null || mTargets.size() == 0) {
            return ret;
        }
        try {
            JSONArray option = function.getJSONArray("option");
            for (int i=0; i<option.length(); i++) {
                String str = option.getString(i);
                if (mTargets.contains(str)) {
                    ret = str;
                    break;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return ret;
    }

    private int getCommandId(JSONObject function) {
        int ret = 0;
        try {
            ret = function.getInt("command");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return ret;
    }

    private int getResponseId(JSONObject function) {
        int ret = 0;
        try {
            ret = function.getInt("reply");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return ret;
    }

    private Calendar getDate(Context context, String option) {
        Locale locale = context.getResources().getConfiguration().locale;
        Calendar cal = Calendar.getInstance(locale);
        if (option.equals("tomorrow")) {
            cal.add(Calendar.DAY_OF_MONTH, 1);
        } else if (option.equals("dayaftertomorrow")) {
            cal.add(Calendar.DAY_OF_MONTH, 2);
        } else if (option.equals("yesterday")) {
            cal.add(Calendar.DAY_OF_MONTH, -1);
        }
        return cal;
    }
    private String getDateString(Context context, String template, String option) {
        Locale locale = context.getResources().getConfiguration().locale;
        SimpleDateFormat format = new SimpleDateFormat(template, locale);
        return format.format(getDate(context, option).getTime());
    }

    public String answer(Context context, List<SimpleToken> tokens) {
        String answer = null;

        JSONObject function = parse(context, tokens);
        if (function != null) {
            switch (getCommandId(function)) {
                case COMMAND_SELF_INTRO:
                    answer = Response.getReplyWord(context, getResponseId(function));
                    break;
                case COMMAND_DATE:
                    String template = Response.getReplyWord(context, getResponseId(function));
                    String option = getOption(function);
                    answer = getDateString(context, template, option);
                    break;
                default:
                    //answer = context.getString(R.string.please_again);
                    break;
            }
        }
        return answer;
    }
}

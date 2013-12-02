package com.example.simpletalk;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

public class Functions {
    private final static boolean DEBUG = BuildConfig.DEBUG;
    private final static String TAG = "SimpleTalk";

    private static Functions sFunction;

    private static final String SETTING_FILE = "setting.txt";
    private static final String DB_FILE = "functions.txt";
    private static final int FORMAT_VERSION = 3;

    /* command ids */
    private static final int COMMAND_SELF_INTRO = 1;
    private static final int COMMAND_DATE = 2;
    private static final int COMMAND_LOCATION = 3;
    private static final int COMMAND_CLEAR_SETTING = 4;
    private static final int COMMAND_YES = 5;
    private static final int COMMAND_NO = 6;

    private Context mContext;
    /* setting JSON data */
    private String mSettings = null;
    private static final String OWNER_TAG = "owner";

    /* functions JSON data */
    private String mData = null;
    /* tag words in the speech */
    private List<String> mTargets = new ArrayList<String>();

    public enum Conversation {NONE, OWNERINFO, CLEARSETTING};
    private Conversation mConvState = Conversation.NONE;

    private String mOwnerName = null;

    public static Functions createInstance(Context context) {
        if (sFunction == null) {
            sFunction = new Functions(context);
        }
        return sFunction;
    }

    private Functions(Context context) {
        this.mContext = context;
        loadSettings();
    }

    private void loadSettings() {
        try {
            String line;
            InputStream in = mContext.openFileInput(SETTING_FILE);
            BufferedReader reader= new BufferedReader(new InputStreamReader(in, "UTF-8"));
            StringBuilder builder = new StringBuilder();
            while(null != (line = reader.readLine())){
                builder.append(line);
            }
            mSettings = builder.toString();
            if (DEBUG) Log.d(TAG, "loaded setting=" + mSettings);

            JSONObject json = new JSONObject(mSettings);
            if (json.has(OWNER_TAG)) {
                mOwnerName = json.getString(OWNER_TAG);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            if (DEBUG) Log.d(TAG, String.format("%s is not exist", SETTING_FILE));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveSettings() {
        if (DEBUG) Log.d(TAG, "save setting");
        if (mSettings != null && !mSettings.isEmpty()) {
            OutputStream out;
            try {
                out = mContext.openFileOutput(SETTING_FILE, Context.MODE_PRIVATE);
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));
                writer.append(mSettings);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void createSettings(String name, String value) {
        JSONObject data = new JSONObject();
        try {
            data.put(name, value);
            mSettings = data.toString();
            if (DEBUG) Log.d(TAG, "create setting=" + mSettings);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        saveSettings();
    }

    private void updateSetting(String name, String value) {
        if (mSettings == null) {
            createSettings(name, value);
            return;
        }
        JSONObject data = null;
        try {
            data = new JSONObject(mSettings);
        } catch (JSONException e) {
            if (DEBUG) Log.d(TAG, "mSettings is empty");
            createSettings(name, value);
            return;
        }
        try {
            data.put(name, value);
            mSettings = data.toString();
            //mOwnerName = data.getString(OWNER_TAG);
            if (DEBUG) Log.d(TAG, "update setting=" + mSettings);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        saveSettings();
    }

    public boolean isUnknownOwner() {
        boolean unknown = (mOwnerName == null) || mOwnerName.isEmpty();
        if (DEBUG) Log.d(TAG, "isUnknownOwner=" + unknown);
        return unknown ? true : false;
    }

    public String getOwnerName() {
        return mOwnerName;
    }

    public void setOwnerName(final String name) {
        if (!name.isEmpty()) {
            mOwnerName = name;
            updateSetting(OWNER_TAG, name);
        }
    }

    public void clearSettings() {
        // currently only owner name setting
        mOwnerName = null;
        updateSetting(OWNER_TAG, null);
    }

    public void setConversationState(Conversation mode) {
        if (DEBUG) Log.d(TAG, "ConvState changed: mode=" + mode);
        mConvState = mode;
    }

    public Conversation getConversationState() {
        return mConvState;
    }

    /*
     * search word in a phrase
     */
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

    /*
     * test whether all words in mTarges are contained in targets
     */
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

    /*
     * for Calendar/Date command
     */
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

    /*
     * for Location command
     */
    public String getCurrentLocation(Context context) {
        String message = null;
        Locale locale = context.getResources().getConfiguration().locale;
        Geocoder geocoder = new Geocoder(context, locale);
        LocationManager locMan =
            (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
        Location loc = locMan.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (loc == null) {
            loc = locMan.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }
        if (loc != null) {
            double latitude = loc.getLatitude();
            double longtitude = loc.getLongitude();
            StringBuilder builder = new StringBuilder();
            try {
                List<Address> addrs = geocoder.getFromLocation(latitude, longtitude, 1);
                for (Address addr : addrs){
                    int max = addr.getMaxAddressLineIndex();
                    for (int i = 0; i<=max; i++){
                        builder.append(addr.getAddressLine(i));
                        builder.append(" ");
                    }
                }
            } catch(IOException e){
                e.printStackTrace();
            }
            message = builder.toString();
        }
        return message;
    }

    private String getProperNoun(Context context, List<SimpleToken> tokens) {
        StringBuilder noun = new StringBuilder();
        // 名詞
        String NOUN = context.getResources().getString(R.string.noun);
        // 固有名詞
        String PROPER_NOUN = context.getResources().getString(R.string.proper_noun);
        // 人名
        String PERSON_NAME = context.getResources().getString(R.string.person_name);
        for (SimpleToken token : tokens) {
            String pos = token.getPartOfSpeech();
            if (pos.contains(NOUN) &&
                    pos.contains(PERSON_NAME) &&
                    pos.contains(PROPER_NOUN)) {
                noun.append(token.getSurface());
            }
        }
        return noun.toString();
    }

    public String answer(Context context, List<SimpleToken> tokens) {
        String answer = null;

        JSONObject function = parse(context, tokens);
        if (function != null) {
            switch (getCommandId(function)) {
                case COMMAND_SELF_INTRO:
                    answer = Response.getReplyWord(context, getResponseId(function));
                    if (isUnknownOwner() &&
                            (getConversationState() == Conversation.NONE)) {
                        answer = answer + context.getString(R.string.who_are_you);
                        setConversationState(Conversation.OWNERINFO);
                    }
                    break;
                case COMMAND_DATE:
                    String template = Response.getReplyWord(context,
                            getResponseId(function));
                    String option = getOption(function);
                    answer = getDateString(context, template, option);
                    setConversationState(Conversation.NONE);
                    break;
                case COMMAND_LOCATION:
                    String location = getCurrentLocation(context);
                    if (location != null) {
                        String format = Response.getReplyWord(context,
                                getResponseId(function));
                        answer = String.format(format, location);
                    } else {
                        answer = context.getString(R.string.error_location);
                    }
                    setConversationState(Conversation.NONE);
                    break;
                case COMMAND_CLEAR_SETTING:
                    if (getConversationState() == Conversation.NONE) {
                        setConversationState(Conversation.CLEARSETTING);
                        answer = Response.getReplyWord(context, getResponseId(function));
                    }
                    break;
                case COMMAND_YES:
                    if (getConversationState() == Conversation.CLEARSETTING) {
                        clearSettings();
                        answer = Response.getReplyWord(context, getResponseId(function));
                        setConversationState(Conversation.NONE);
                    }
                    break;
                case COMMAND_NO:
                    if (getConversationState() == Conversation.CLEARSETTING) {
                        answer = Response.getReplyWord(context, getResponseId(function));
                        setConversationState(Conversation.NONE);
                    }
                    break;
                default:
                    //answer = context.getString(R.string.please_again);
                    break;
            }
        } else {
            if (getConversationState() == Conversation.OWNERINFO) {
                setOwnerName(getProperNoun(context, tokens));
                if (!isUnknownOwner()) {
                    setConversationState(Conversation.NONE);
                    answer = String.format(
                            context.getString(R.string.isnt_it),
                            getOwnerName());
                } else {
                    answer = context.getString(R.string.who_are_you_again);
                }
            }
        }
        return answer;
    }
}

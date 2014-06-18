package com.example.simpletalk;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

/**
 * Exact match phrase parser
 */
public final class Greeting {
    private final static boolean DEBUG = BuildConfig.DEBUG;
    private final static String TAG = "SimpleTalk:Greeting";
    private static final int FORMAT_VERSION = 2;
    private static final String DB_FILE = "greeting.txt";
    /* greeting JSON data */
    private static String mData = null;

    private Context mContext;
    //private YahooMorphoParser mMa = new YahooMorphoParser();
    private LuceneGosenParser mMa = new LuceneGosenParser();
    private WordSearch mWs;
    private String mVariation, mClassifier, mContent;

    public Greeting(Context context) {
        mContext = context;
        mWs = new WordSearch(mContext);
    }

    public String greeting(Context context, List<SimpleToken> tokens) {
        int id = 0;
        for (SimpleToken token : tokens) {
            if ((id = getCategoryId(context, token.getSurface())) != 0) {
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
        mVariation = null;
        mClassifier = null;
        mContent = null;
        if ((category = getCategoryId(context, word)) != 0) {
            String str;
            if (mVariation != null) {
                str = Response.getReplyWord(context, category, mVariation);
            } else {
                str = Response.getReplyWord(context, category);
                if (mContent != null) {
                    if (mContent.equals(WordSearch.MISSING)) {
                        str = Response.getReplyWord(context, category, mContent);
                    } else {
                        str = String.format(str, mContent);
                    }
                }
            }
            int emotion = 0;
            if ((emotion = Response.getIntonation(context, category)) != 0) {
                str = Utils.addEmotionTag(str, emotion);
            }
            return str;
        }
        return null;
    }

    private boolean hasClassifier(String word) {
        return word.indexOf('%') != -1 ? true : false;
    }

    private String truncateMatchWordFromSentence(String sentence, String word) {
        int end = sentence.indexOf(word);
        return sentence.substring(0, end);
    }

    private String truncateClassifier(String word) {
        int start = word.indexOf('%');
        int end = word.indexOf('%', start+1);
        if (start == -1 || end == -1) {
            return "";
        } else {
            mClassifier = word.substring(start+1, end);
            String truncated = word.substring(end+1);
            if (DEBUG) Log.d(TAG, "classifier="+mClassifier+" truncated="+truncated);
            return truncated;
        }
    }

    private String searchContent(String sentence) {
        List<SimpleToken> tokens = mMa.parse(sentence);
        if (tokens.isEmpty()) {
            return null;
        }

        String NOUN = mContext.getResources().getString(R.string.noun);
        String UNKNOWN = mContext.getResources().getString(R.string.unknown);
        for (int i=tokens.size()-1; i>=0; i--) {
            SimpleToken token = tokens.get(i);
            String word = token.getSurface();
            if(DEBUG) Log.d(TAG, "word="+word+" ("+token.getPartOfSpeech()+")");
            if (token.getPartOfSpeech().contains(NOUN) ||
                    token.getPartOfSpeech().contains(UNKNOWN)) {
               return mWs.getContent(word);
            }
        }
        return null;
    }

    private String searchTopCategory(String sentence) {
        List<SimpleToken> tokens = mMa.parse(sentence);
        if (tokens.isEmpty()) {
            return null;
        }

        String NOUN = mContext.getResources().getString(R.string.noun);
        for (int i=tokens.size()-1; i>=0; i--) {
            SimpleToken token = tokens.get(i);
            String word = token.getSurface();
            if(DEBUG) Log.d(TAG, "word="+word+" ("+token.getPartOfSpeech()+")");
            if (token.getPartOfSpeech().contains(NOUN)) {
                List<String> categories = mWs.getCategories(word);
                if (!categories.isEmpty()) {
                    return categories.get(0);
                }
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
     private int getCategoryId(Context context, String sentence) {
         if (mData == null) {
             mData = Utils.loadAssetDB(context, DB_FILE);
         }

         int id = 0;
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
                         String word = words.getString(k);
                         boolean hasClassifier = hasClassifier(word);
                         if (hasClassifier) {
                             //NOTE: classifier is truncated from word
                             word = truncateClassifier(word);   // be set mClassifier
                         }
                         if (sentence.contains(word)) {
                             if (hasClassifier) {
                                 if (mClassifier.equals("category")) {
                                     mVariation = searchTopCategory(sentence);
                                     if (DEBUG) Log.d(TAG, "variation="+mVariation);
                                 } else if (mClassifier.equals("content")) {
                                     mContent = searchContent(
                                             truncateMatchWordFromSentence(sentence, word));
                                 }
                             }
                             found = true;
                             break;
                         }
                     }
                     if (found) {
                         id = phrase.getInt("category");
                         break;
                     }
                 }
             }
         } catch (JSONException e) {
             e.printStackTrace();
         }
         return id;
     }
}

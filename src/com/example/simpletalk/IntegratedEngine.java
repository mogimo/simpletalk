package com.example.simpletalk;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

public class IntegratedEngine implements Engine {
    private final static boolean DEBUG = BuildConfig.DEBUG;
    private final static String TAG = "SimpleTalk:Engine";
    private ResponseListener mListener;
    private Context mContext;

    private enum Empathy {NORMAL, HAPPINESS, ANGER, SADNESS};
    private Empathy mState = Empathy.NORMAL;

    private Greeting mGreeting;
    private Functions mFunction;
    private Parrot mParrot;

    // for Yahoo! parser
    private final static String YAHOO_MORPHO_URL =
        "http://jlp.yahooapis.jp/MAService/V1/parse";
    private final static String YAHOO_KEYPHRASE_URL =
        "http://jlp.yahooapis.jp/KeyphraseService/V1/extract";
    private final static String APP_ID =
        "dj0zaiZpPUZrMk1HTUwxalZoOSZkPVlXazlaWGcwYkdWaE0yTW1jR285TUEtLSZzPWNvbnN1bWVyc2VjcmV0Jng9YTk-";
    private enum Type {MORPHOLOGICAL, KEYPHRASE};
    private Type mType = null;
    private Map<String, String> mPayload = new HashMap<String, String>();
    private HttpRequestTask mHttpPost;

    public IntegratedEngine(Context context) {
        this.mContext = context;

        mGreeting = new Greeting();
        mFunction = Functions.createInstance(context);
        mParrot = new Parrot();
    }

    @Override
    public void request(String sentence) {
        // yahoo! parser
        mType = Type.MORPHOLOGICAL;
        setDefaultPayload(Type.MORPHOLOGICAL);
        setSentence(sentence);
        mHttpPost = new HttpRequestTask(YAHOO_MORPHO_URL, mPayload);
        mHttpPost.execute();
    }

    @Override
    public void setResponseListener(ResponseListener listener) {
        this.mListener = listener;
    }

    private String intonation(String message) {
        int id = 0;
        switch (mState) {
            case NORMAL:
                return message;
            case HAPPINESS:
                id = Utils.EMOTION_HAPPINESS;
                break;
            case ANGER:
                id = Utils.EMOTION_ANGER;
                break;
            case SADNESS:
                id = Utils.EMOTION_SADNESS;
                break;
        }
        return Utils.addEmotionTag(message, id);
    }

    private String appendOwnerName() {
        return String.format(
                mContext.getString(R.string.owner_connector),
                mFunction.getOwnerName());
    }

    private String analyze(List<SimpleToken> tokens) {
        String response = null;

        if (DEBUG) {
            for (SimpleToken token : tokens) {
                Log.d(TAG, "surface="+token.getSurface());
                Log.d(TAG, "part of speech="+token.getPartOfSpeech());
            }
        }

        // Greeting
        if (mGreeting != null) {
            response = mGreeting.greeting(mContext, tokens);
            if ((response != null) && !mFunction.isUnknownOwner()) {
                response = response + appendOwnerName();
            }
        }
        // Parrot
        if (response == null && mParrot != null) {
            response = mParrot.parrot(mContext, tokens);
        }
        // Function
        if (response == null && mFunction != null) {
            response = mFunction.answer(mContext, tokens);
        }

        // TODO: and more ...

        if (response != null) {
            response = intonation(response);
        }

        return response;
    }

    private class AnalyzerTask extends AsyncTask<List<SimpleToken>, Void, String> {
        @Override
        protected String doInBackground(List<SimpleToken>... params) {
            return analyze(params[0]);
        }

        @Override
        protected void onPostExecute(final String result) {
            if (mListener != null) {
                mListener.onResult(result);
            }
        }
    }

    // for Yahoo! parser
    private void setDefaultPayload(Type type) {
        switch (type) {
            case MORPHOLOGICAL:
                mPayload.put("appid", APP_ID);
                mPayload.put("results", "ma");
                mPayload.put("response", "surface,reading,pos");
                break;
            case KEYPHRASE:
                mPayload.put("appid", APP_ID);
                mPayload.put("output", "xml");
                break;
        }
    }

    private void setSentence(String sentence) {
        mPayload.put("sentence", sentence);
    }

    private String parseMorphologic(String xml) {
        List<SimpleToken> tokens = YahooMorphoParser.parse(xml);
        if (tokens == null || tokens.size() == 0) {
            Log.e(TAG, "Cound not parse sentence");
            mListener.onResult(null);
        }
        return analyze(tokens);
    }

    private String parseResponse(String xml) {
        String str = null;
        switch (mType) {
            case MORPHOLOGICAL:
                str = parseMorphologic(xml);
                break;
            case KEYPHRASE:
                break;
        }
        return str;
    }

    private class HttpRequestTask extends AsyncTask<Void, Void, String> {
        private final String mUrl;
        private final Map<String, String>mPayload;
        private HttpRequest request = new HttpRequest();

        HttpRequestTask(final String url, final Map<String, String>payload) {
            this.mUrl = url;
            this.mPayload = payload;
        }

        @Override
        protected String doInBackground(Void... params) {
            return parseResponse(request.post(mUrl, mPayload));
        }

        @Override
        protected void onPostExecute(final String result) {
            if (mListener != null) {
                mListener.onResult(result);
            }
        }
    }

}

package com.example.simpletalk;

import java.util.List;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

public class IntegratedEngine implements Engine {
    private final static boolean DEBUG = BuildConfig.DEBUG;
    private final static String TAG = "SimpleTalk";
    private ResponseListener mListener;
    private Context mContext;

    private enum Empathy {NORMAL, HAPPINESS, ANGER, SADNESS};
    private Empathy mState = Empathy.NORMAL;

    public IntegratedEngine(Context context) {
        this.mContext = context;
    }

    @Override
    public void request(String sentence) {
        List<SimpleToken> tokens = LuceneGosenParser.parse(sentence);
        if (tokens == null || tokens.size() == 0) {
            Log.e(TAG, "Cound not parse sentence");
            mListener.onResult(null);
        }

        for (SimpleToken token : tokens) {
            if (DEBUG) Log.d(TAG, "surface="+token.getSurface());
            if (DEBUG) Log.d(TAG, "part of speech="+token.getPartOfSpeech());
        }

        AnalyzerTask task = new AnalyzerTask();
        task.execute(tokens);
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

    private String analyze(List<SimpleToken> tokens) {
        // Greeting
        String response = Greeting.greeting(mContext, tokens);
        // Parrot
        if (response == null) {
            response = Parrot.parrot(mContext, tokens);
        }
        // Function
        if (response == null) {
            Functions func = new Functions();
            response = func.answer(mContext, tokens);
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
}

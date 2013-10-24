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

    public IntegratedEngine(Context context) {
        this.mContext = context;
    }

    @Override
    public void request(String sentence) {
        List<SimpleToken> tokens = LuceneGosenParser.parse(sentence);
        for (SimpleToken token : tokens) {
            Log.d(TAG, "surface="+token.getSurface());
            Log.d(TAG, "part of speech="+token.getPartOfSpeech());
        }

        AnalyzerTask task = new AnalyzerTask();
        task.execute(tokens);
    }

    @Override
    public void setResponseListener(ResponseListener listener) {
        this.mListener = listener;
    }

    private String greeting(List<SimpleToken> tokens) {
        String response;
        for (SimpleToken token : tokens) {
            if ((response = Greeting.getResponse(mContext, token.getSurface())) != null) {
                return response;
            }
        }
        return null;
    }

    private String analyze(List<SimpleToken> tokens) {
        String response;
        if ((response = greeting(tokens)) != null) {
            return response;
        }
        return null;
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

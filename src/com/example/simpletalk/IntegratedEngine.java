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

    public IntegratedEngine(Context context) {
        this.mContext = context;

        mGreeting = new Greeting(context);
        mFunction = Functions.createInstance(context);
        mParrot = new Parrot();
    }

    @Override
    public void request(String sentence) {
        YahooMorphoParser ma = new YahooMorphoParser();
        mListener.onResult(analyze(ma.parse(sentence)));
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
}

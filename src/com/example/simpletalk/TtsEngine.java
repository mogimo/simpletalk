package com.example.simpletalk;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

public class TtsEngine implements VoiceEngine, OnInitListener {
    private final static boolean DEBUG = BuildConfig.DEBUG;
    private final static String TAG = "WhisperFairy";
    private final static String PARAM_TAG = TtsEngine.class.getPackage().getName();
    private final static String PARAM_RETRY = PARAM_TAG + ":retry";
    private final static String PARAM_TALK = PARAM_TAG + ":talk";

    private Context mContext;
    private boolean isTtsReady = false;
    private TextToSpeech mTts;
    private TtsProgressListener mTtsListener;
    private HashMap<String, String> mTtsParam = null;

    private class TtsProgressListener extends UtteranceProgressListener {
        @Override
        public void onDone(String utteranceId) {
            if (DEBUG) Log.d(TAG, "onDone: " + utteranceId);
        }

        @Override
        public void onError(String utteranceId) {
            if (DEBUG) Log.d(TAG, "error from TTS Engine!: " + utteranceId);
        }

        @Override
        public void onStart(String utteranceId) {
            if (DEBUG) Log.d(TAG, "onStart: " + utteranceId);
        }
    }

    public TtsEngine(Context context) {
        this.mContext = context;
    }

    @Override
    public void init() {
        mTtsListener = new TtsProgressListener();
        mTts = new TextToSpeech(mContext, this);
        mTts.setOnUtteranceProgressListener(mTtsListener);
        mTtsParam = new HashMap<String, String>();
    }

    @Override
    public void release() {
        if (mTts != null) {
            mTts.shutdown();
        }
    }

    @Override
    public void talk(String text) {
        if (text != null) {
            if (mTts != null && isTtsReady) {
                mTtsParam.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, PARAM_TALK);
                mTts.speak(text, TextToSpeech.QUEUE_FLUSH, mTtsParam);
            }
        }
    }

    @Override
    public void onInit(int arg0) {
        if (DEBUG) Log.d(TAG, "TTS ready!");
        isTtsReady = true;
    }

}

package com.example.simpletalk;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

public class GoogleRecognizer implements Recognizer {
    private final static boolean DEBUG = BuildConfig.DEBUG;
    private final static String TAG = "SimpleTalk:GoogleRecognizer";

    private final static float SCORE_THRESHOLD = 0.3f;

    private Context mContext;

    private SpeechRecognizer mSpeechRecognizer;
    private RecognitionServiceListener mListener;

    private RecognizerListener mClient;

    private class RecognitionServiceListener implements RecognitionListener {
        @Override
        public void onBeginningOfSpeech() {
            if (DEBUG) Log.d(TAG, "onBeginningOfSpeech");
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
            if (DEBUG) Log.d(TAG, "onBufferReceived");
        }

        @Override
        public void onEndOfSpeech() {
            if (DEBUG) Log.d(TAG, "onEndOfSpeech");
        }

        @Override
        public void onError(int error) {
            if (mClient != null) {
                if (DEBUG) Log.d(TAG, "onError: " + error);
                mClient.onError(error);
            }
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
            if (DEBUG) Log.d(TAG, "onEvent: type=" + eventType);
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            if (DEBUG) Log.d(TAG, "onPartialResults");
        }

        @Override
        public void onReadyForSpeech(Bundle params) {
            if (mClient != null) {
                if (DEBUG) Log.d(TAG, "onReadyForSpeech");
                mClient.onReady();
            }
        }

        private String getTopScoredText(ArrayList<String> results, float[] scores) {
            int i = 0;
            for (String text : results) {
                if (DEBUG) Log.d(TAG, "[" + scores[i] + "] " + text);
                i++;
            }
            return (Float.compare(scores[0], SCORE_THRESHOLD) > 0) ?
                    results.get(0) : null;
        }

        @Override
        public void onResults(Bundle results) {
            if (DEBUG) Log.d(TAG, "onResults");
            ArrayList<String> texts =
                results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            float[] scores = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);
            String msg = getTopScoredText(texts, scores);
            mClient.onRecognize(msg, scores[0]);
        }

        @Override
        public void onRmsChanged(float rmsdB) {
            //Log.d(TAG, "onRmsChanged");
        }
    }

    public GoogleRecognizer(Context context) {
        this.mContext = context;
    }

    public void init() {
        mListener = new RecognitionServiceListener();
        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(mContext);
        if (mSpeechRecognizer != null && mListener != null) {
            mSpeechRecognizer.setRecognitionListener(mListener);
        }
    }

    public void start() {
        if (mSpeechRecognizer != null) {
            startSpeechRecognize();
        }
    }

    public void stop() {
        if (mSpeechRecognizer != null) {
            stopSpeechRecognize();
        }
    }

    public void release() {
        if (mSpeechRecognizer != null) {
            mSpeechRecognizer.setRecognitionListener(null);
            mSpeechRecognizer = null;
        }
    }

    public void setRecognizerListener(RecognizerListener listener) {
        if (mClient == null) {
            mClient = listener;
        } else {
            Log.i(TAG, "already registered listener");
        }
    }

    private void startSpeechRecognize() {
        if (DEBUG) Log.d(TAG, "start recognize!");
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_CONFIDENCE_SCORES, "");
        if (mSpeechRecognizer != null) {
            mSpeechRecognizer.startListening(intent);
        }
    }

    private void stopSpeechRecognize() {
        if (mSpeechRecognizer != null) {
            //mSpeechRecognizer.stopListening();
            //mSpeechRecognizer.cancel();

            // SpeechRecognier bind when startListening, then unbind when destroy.
            mSpeechRecognizer.destroy();
        }
    }

}

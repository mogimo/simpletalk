
package com.example.simpletalk;

import java.util.ArrayList;
import java.util.HashMap;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.Intent;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;

public class MainActivity extends Activity implements OnInitListener{
	private final static boolean DEBUG = BuildConfig.DEBUG;	
    private final static String TAG = "SimpleTalk";
    private static final String PARAM_TAG = MainActivity.class.getSimpleName();

    private final static int SPEECH_DURATION = 1500;
    private final static int MSG_SPEECH_AGAIN = 0;
    private final static float SCORE_THRESHOLD = 0.3f;
    
    private boolean isRecognierWorking = false;
    private boolean isTtsReady = false;
    private HashMap<String, String> mTtsParam = null;
    private String mRetrySpeakId;

    private SpeechRecognizer mSpeechRecognizer;
    private RecognitionServiceLisnter mListener;
    private TextView mTextView;
    private TextToSpeech mTts;
    private TtsProgressListener mTtsListener;
    private RepeatHandler mHandler = new RepeatHandler();

    private class RecognitionServiceLisnter implements RecognitionListener {
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
        	if (DEBUG) Log.d(TAG, "onError: " + error);
            isRecognierWorking = false;
            speakTryAgain();
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
        	if (DEBUG) Log.d(TAG, "onReadyForSpeech");
            isRecognierWorking = true;
        }

        @Override
        public void onResults(Bundle results) {
        	if (DEBUG) Log.d(TAG, "onResults");
            ArrayList<String> texts = 
                results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            float[] scores = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);
            mTextView.setText("["+scores[0]+"] " + texts.get(0));
            talk(getTopScoredText(texts, scores));

            // next talk
            messageRetry();
            isRecognierWorking = false;
        }

        @Override
        public void onRmsChanged(float rmsdB) {
            //Log.d(TAG, "onRmsChanged");
        }
    }
    
    private class TtsProgressListener extends UtteranceProgressListener {

        @Override
        public void onDone(String utteranceId) {
            if (DEBUG) Log.d(TAG, "onDone: " + utteranceId);
            if (mRetrySpeakId != null) {
                if (utteranceId.equals(mRetrySpeakId)) {
                    messageRetry();
                }
            }
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

    private class RepeatHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
        	if (DEBUG) Log.d(TAG, "message comming: " + msg);
            switch (msg.what) {
                case MSG_SPEECH_AGAIN:
                    if (!isRecognierWorking && !mTts.isSpeaking()) {
                        startSpeachRecognize();
                    } else {
                        if (DEBUG) Log.d(TAG, "maybe still speaking");
                    	messageRetry();
                    }
                    break;
                default:
                    break;
            }
        }
    }
    
    private void messageRetry() {
        if (DEBUG) Log.d(TAG, "send retry msg!");
    	mHandler.removeMessages(MSG_SPEECH_AGAIN);
        mHandler.sendEmptyMessageDelayed(MSG_SPEECH_AGAIN, SPEECH_DURATION);    	
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextView = (TextView)findViewById(R.id.text);

        mListener = new RecognitionServiceLisnter();
        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        if (mSpeechRecognizer != null && mListener != null) {
            mSpeechRecognizer.setRecognitionListener(mListener);
        }

        mTtsListener = new TtsProgressListener();
        mTts = new TextToSpeech(getApplicationContext(), this);
        mTts.setOnUtteranceProgressListener(mTtsListener);
        mTtsParam = new HashMap<String, String>();
    }

    public void onResume() {
        super.onResume();
        startSpeachRecognize();
    }

    public void onPause() {
    	super.onPause();
    	mHandler.removeMessages(MSG_SPEECH_AGAIN);
    	if (mSpeechRecognizer != null) {
	        mSpeechRecognizer.stopListening();
	        mSpeechRecognizer.cancel();
    	}
    }

    public void onDestory() {
    	super.onDestroy();
    	if (mSpeechRecognizer != null) {
	        mSpeechRecognizer.setRecognitionListener(null);
	        mSpeechRecognizer.destroy();
	        mSpeechRecognizer = null;
    	}
    	if (mTts != null) {
    		mTts.shutdown();
    	}
    }

    private void startSpeachRecognize() {
    	if (DEBUG) Log.d(TAG, "start recognize!");
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_CONFIDENCE_SCORES, "");
    	if (mSpeechRecognizer != null) {
    		mSpeechRecognizer.startListening(intent);
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
    public void onInit(int arg0) {
    	if (DEBUG) Log.d(TAG, "TTS ready!");
    	isTtsReady = true;
    }

    private void speakTryAgain() {
    	if (mTts != null && isTtsReady) {
    	    mTtsParam.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, PARAM_TAG);
	    	mTts.speak(
	                getString(R.string.please_again), TextToSpeech.QUEUE_FLUSH, mTtsParam);
	    	//messageRetry();
	    	mRetrySpeakId = mTtsParam.get(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID);
    	}
    }

    private void talk(String text) {
        if (text != null) {
        	if (mTts != null && isTtsReady) {
        	    mTtsParam.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, PARAM_TAG);
        		mTts.speak(text, TextToSpeech.QUEUE_FLUSH, mTtsParam);
        		mRetrySpeakId = null;
        	}
        } else {
            speakTryAgain();
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
}

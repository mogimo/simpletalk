
package com.example.simpletalk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.Intent;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class MainActivity extends Activity implements Engine.ResponseListener {
    private final static boolean DEBUG = BuildConfig.DEBUG;
    private final static String TAG = "SimpleTalk";

    private final static int SPEECH_DURATION = 1800;
    private final static int MSG_SPEECH_AGAIN = 0;
    private final static float SCORE_THRESHOLD = 0.3f;

    private final static boolean LOGGING_ON = false;
    private final static String GAE_LOGGING = "http://pirobosetting.appspot.com/register";

    static boolean isUseHOYA = true;

    private boolean isRecogniezrWorking = false;

    private SpeechRecognizer mSpeechRecognizer;
    private RecognitionServiceListener mListener;

    private VoiceEngine mVoice;

    private TextView mTextView;
    private RepeatHandler mHandler = new RepeatHandler();

    private Engine mEngine;

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
            if (DEBUG) Log.d(TAG, "onError: " + error);
            isRecogniezrWorking = false;
            switch (error) {
                case SpeechRecognizer.ERROR_NETWORK:
                case SpeechRecognizer.ERROR_SERVER:
                    mVoice.talk(Utils.addEmotionTag(getString(R.string.error_again),
                            Utils.EMOTION_SADNESS));
                    break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                    mVoice.talk(getString(R.string.please_retry));
                    messageRetry();
                    break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    if (!isRecogniezrWorking) {
                        startSpeechRecognize();
                    } else {
                        messageRetry();
                    }
                    break;
                default:
                    mVoice.talk(getString(R.string.please_again));
                    messageRetry();
                    break;
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
            if (DEBUG) Log.d(TAG, "onReadyForSpeech");
            isRecogniezrWorking = true;
        }

        @Override
        public void onResults(Bundle results) {
            if (DEBUG) Log.d(TAG, "onResults");
            ArrayList<String> texts =
                results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            float[] scores = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);
            mTextView.setText("["+scores[0]+"] " + texts.get(0));
            String msg = getTopScoredText(texts, scores);
            if (msg != null) {
                mEngine.request(msg);
            } else {
                mVoice.talk(Utils.addEmotionTag(
                        getString(R.string.low_score), Utils.EMOTION_SADNESS));
            }

            // next talk
            messageRetry();
            isRecogniezrWorking = false;
        }

        @Override
        public void onRmsChanged(float rmsdB) {
            //Log.d(TAG, "onRmsChanged");
        }
    }

    private class RepeatHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            if (DEBUG) Log.d(TAG, "message comming: " + msg);
            switch (msg.what) {
                case MSG_SPEECH_AGAIN:
                    if (!isRecogniezrWorking) {
                        startSpeechRecognize();
                    } else {
                        if (DEBUG) Log.d(TAG, "maybe still speaking or talking");
                        messageRetry();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void messageRetry(int expandDelayTimes) {
        long delay = SPEECH_DURATION * expandDelayTimes;
        mHandler.removeMessages(MSG_SPEECH_AGAIN);
        mHandler.sendEmptyMessageDelayed(MSG_SPEECH_AGAIN, delay);
        if (DEBUG) Log.d(TAG, "send retry msg! delay=" + delay);
    }

    private void messageRetry() {
        messageRetry(1);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextView = (TextView)findViewById(R.id.text);

        // Speech Recognizer
        mListener = new RecognitionServiceListener();
        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        // Text to speech engine
        if (isUseHOYA) {
            mVoice = new HoyaVoiceText(this);
        } else {
            mVoice = new AITalk(this);
        }
        mVoice.init();

        mEngine = new IntegratedEngine(this);
        mEngine.setResponseListener(this);
    }

    public void onResume() {
        Log.v(TAG, "onResume()");
        super.onResume();

        // for speech recognizer
        if (mSpeechRecognizer != null && mListener != null) {
            mSpeechRecognizer.setRecognitionListener(mListener);
        }
        startSpeechRecognize();
    }

    public void onPause() {
        Log.v(TAG, "onPause()");
        super.onPause();
        mHandler.removeMessages(MSG_SPEECH_AGAIN);

        // for speech recognizer
        stopSpeechRecognize();
    }

    public void onDestory() {
        super.onDestroy();

        // for speech recognizer
        if (mSpeechRecognizer != null) {
            mSpeechRecognizer.setRecognitionListener(null);
            mSpeechRecognizer = null;
        }
    }

    //
    // for Speech Recognizer
    //
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

    private String getTopScoredText(ArrayList<String> results, float[] scores) {
        int i = 0;
        for (String text : results) {
            if (DEBUG) Log.d(TAG, "[" + scores[i] + "] " + text);
            i++;
        }
        return (Float.compare(scores[0], SCORE_THRESHOLD) > 0) ?
                results.get(0) : null;
    }

    private void supportiveResponse() {
        if (DEBUG) Log.d(TAG, "maybe not match any phrase!");
        mVoice.talk(Response.getSupportiveResponse(getApplicationContext()));
    }

    // get response from engine
    @Override
    public void onResult(String response) {
        if (LOGGING_ON) {
            logging(response);
        }
        if (response == null) {
            Log.e(TAG, "There is no answer to speak, so just reply supportive response");
            supportiveResponse();
        } else {
            mVoice.talk(response);
        }
        messageRetry();
    }

    /*
     * Menu
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0,0,0,"Use HOYA");
        menu.add(0,1,0,"Use AITalk");
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case 1:
            if (isUseHOYA) {
                mVoice.release();
                mVoice = null;
                mVoice = new AITalk(this);
                mVoice.init();
                isUseHOYA = false;
            }
            break;
        case 0:
        default:
            if (!isUseHOYA) {
                mVoice.release();
                mVoice = null;
                mVoice = new HoyaVoiceText(this);
                mVoice.init();
                isUseHOYA = true;
            }
            break;
        }
        messageRetry(0);
        return true;
    }    

    /*
     * Logging
     */
    private void logging(String result) {
        Map<String, String> payload = new HashMap<String, String>();
        payload.put("id", mTextView.getText().toString());
        payload.put("value", result);
        LoggingTask task = new LoggingTask(GAE_LOGGING, payload);
        task.execute();
    }

    private class LoggingTask extends AsyncTask<Void, Void, Void> {
        private final String mUrl;
        private final Map<String, String>mPayload;
        private HttpRequest request = new HttpRequest();

        LoggingTask(final String url, final Map<String, String>payload) {
            this.mUrl = url;
            this.mPayload = payload;
        }

        @Override
        protected Void doInBackground(Void... params) {
            request.post(mUrl, mPayload);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            //
        }
    }
}

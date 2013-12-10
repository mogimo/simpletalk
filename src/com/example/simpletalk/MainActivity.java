
package com.example.simpletalk;

import java.util.HashMap;
import java.util.Map;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class MainActivity extends Activity {
    private final static boolean DEBUG = BuildConfig.DEBUG;
    private final static String TAG = "SimpleTalk:Activity";

    private final static int SPEECH_DURATION = 1800;
    private final static int MSG_SPEECH_AGAIN = 0;
    private final static int MSG_RECOGNIZE_READY = 1;

    private final static boolean LOGGING_ON = false;
    private final static String GAE_LOGGING = "http://pirobosetting.appspot.com/register";

    private static boolean isUseGoogle = false;
    private static boolean isUseHOYA = true;

    private TextView mTextView;
    private RepeatHandler mHandler = new RepeatHandler();
    private boolean isRecogniezrWorking = false;

    private Recognizer mSpeech;
    private RecognizerServiceListener mSpeechRecognizeListener = new RecognizerServiceListener();
    private VoiceEngine mVoice;
    private Engine mEngine;
    private DialogueListener mDialogListener = new DialogueListener();

    private class RecognizerServiceListener implements Recognizer.RecognizerListener {
        private String addEmotionTag(String message, int emotion) {
            if (!isUseHOYA) {
                return message;
            }
            return Utils.addEmotionTag(message, emotion);
        }

        @Override
        public void onError(int error) {
            if (DEBUG) Log.d(TAG, "onError: " + error);
            isRecogniezrWorking = false;
            switch (error) {
                case Recognizer.ERROR_NETWORK:
                case Recognizer.ERROR_SERVER:
                    mVoice.talk(addEmotionTag(getString(R.string.error_again),
                            Utils.EMOTION_SADNESS));
                    break;
                case Recognizer.ERROR_NO_MATCH:
                    mVoice.talk(getString(R.string.please_retry));
                    messageRetry();
                    break;
                case Recognizer.ERROR_TIMEOUT:
                    if (!isRecogniezrWorking) {
                        mSpeech.start();
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
        public void onReady() {
            if (DEBUG) Log.d(TAG, "onReady");
            mHandler.sendEmptyMessage(MSG_RECOGNIZE_READY);
            isRecogniezrWorking = true;
        }

        @Override
        public void onRecognize(String response, float score) {
            if (DEBUG) Log.d(TAG, "result[" + score + "]=" + response);
            if (response != null) {
                mEngine.request(response);
            } else {
                mVoice.talk(addEmotionTag(
                        getString(R.string.low_score), Utils.EMOTION_SADNESS));
            }
            // next talk
            messageRetry();
            isRecogniezrWorking = false;
        }
    }

    private class DialogueListener implements Engine.ResponseListener {
        private void supportiveResponse() {
            if (DEBUG) Log.d(TAG, "maybe not match any phrase!");
            mVoice.talk(Response.getSupportiveResponse(getApplicationContext()));
        }

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
    }

    private class RepeatHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            if (DEBUG) Log.d(TAG, "message comming: " + msg);
            switch (msg.what) {
                case MSG_SPEECH_AGAIN:
                    if (!isRecogniezrWorking) {
                        mSpeech.start();
                    } else {
                        if (DEBUG) Log.d(TAG, "maybe still speaking or talking");
                        messageRetry();
                    }
                    break;
                case MSG_RECOGNIZE_READY:
                    mTextView.setText(getString(R.string.text));
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

        // Speech recognizer engine
        if (isUseGoogle) {
            mSpeech = new GoogleRecognizer(this);
        } else {
            mSpeech = new VGateASRTypeD();
        }
        mSpeech.setRecognizerListener(mSpeechRecognizeListener);
        mSpeech.init();

        // Text to speech engine
        if (isUseHOYA) {
            mVoice = new HoyaVoiceText(this);
        } else {
            mVoice = new AITalk(this);
        }
        mVoice.init();

        // Dialogue engine
        mEngine = new IntegratedEngine(this);
        mEngine.setResponseListener(mDialogListener);
    }

    public void onResume() {
        Log.v(TAG, "onResume()");
        super.onResume();

        mSpeech.start();
    }

    public void onPause() {
        Log.v(TAG, "onPause()");
        super.onPause();
        mHandler.removeMessages(MSG_SPEECH_AGAIN);

        // for speech recognizer
        mSpeech.stop();
    }

    public void onDestory() {
        super.onDestroy();
        mSpeech.release();
        mHandler = null;
    }

    /*
     * Menu
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0,0,0,"Use HOYA[Voice]");
        menu.add(0,1,0,"Use AITalk[Voice]");
        menu.add(0,2,0,"Use Google[Recognize]");
        menu.add(0,3,0,"Use vGate[Recognize]");
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
        case 2:
            if (!isUseGoogle) {
                mSpeech.stop();
                mSpeech.release();
                mSpeech = null;
                mSpeech = new GoogleRecognizer(this);
                mSpeech.setRecognizerListener(mSpeechRecognizeListener);
                mSpeech.init();
                isUseGoogle = true;
                isRecogniezrWorking = false;
                mTextView.setText(getString(R.string.wait));
            }
            break;
        case 3:
            if (isUseGoogle) {
                mSpeech.stop();
                mSpeech.release();
                mSpeech = null;
                mSpeech = new VGateASRTypeD();
                mSpeech.setRecognizerListener(mSpeechRecognizeListener);
                mSpeech.init();
                isUseGoogle = false;
                isRecogniezrWorking = false;
                mTextView.setText(getString(R.string.wait));
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

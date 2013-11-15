
package com.example.simpletalk;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import kr.co.voiceware.HIKARI;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends Activity implements Engine.ResponseListener {
    private final static boolean DEBUG = BuildConfig.DEBUG;
    private final static String TAG = "SimpleTalk";

    private final static int SPEECH_DURATION = 2000;
    private final static int MSG_SPEECH_AGAIN = 0;
    private final static float SCORE_THRESHOLD = 0.3f;

    // for VoiceTEXT
    private final byte[] mLicense =  new byte[2048];
    private static final String HIKARI_VDB = "tts_single_db_hikari.vtdb";
    private static final String VDB_PATH = "/sdcard/";
    private static final int FLAG_SIZE_CHECK = -1;
    private static final int FLAG_FIRST_FRAME = 0;
    private static final int FLAG_ANOTHRE_FRAME = 1;

    private final static boolean LOGGING_ON = !DEBUG;
    private final static String GAE_LOGGING = "http://pirobosetting.appspot.com/register";

    private boolean isRecogniezrWorking = false;
    private boolean isTalking = false;

    private SpeechRecognizer mSpeechRecognizer;
    private RecognitionServiceListener mListener;
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
                    talk(getString(R.string.error_again));
                    break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                    talk(getString(R.string.please_retry));
                    break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    if (!isRecogniezrWorking) {
                        startSpeechRecognize();
                    } else {
                        messageRetry();
                    }
                    break;
                default:
                    talk(getString(R.string.please_again));
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
                talk(getString(R.string.low_score));
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
                    if (!isRecogniezrWorking && !isTalking) {
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

        // Speech Recognizer
        mListener = new RecognitionServiceListener();
        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        String version = HIKARI.GetVersion();
        Log.d(TAG, "Engine: " + version + "\n");

        if (readLicence()) {
            loadDB();
        }

        mEngine = new IntegratedEngine(this);
        mEngine.setResponseListener(this);
    }

    public void onResume() {
        super.onResume();

        // for speech recognizer
        if (mSpeechRecognizer != null && mListener != null) {
            mSpeechRecognizer.setRecognitionListener(mListener);
        }
        startSpeechRecognize();
    }

    public void onPause() {
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

    //
    // for Speech Synthesis
    //
    private boolean readLicence() {
        int ret = 0;
        try {
            ret =  getResources().openRawResource(R.raw.verification).read(mLicense);
        } catch(Exception ex) {
            Log.e(TAG, "ERROR: fail to load license file");
            ret = -1;
        }
        if (ret == -1) {
            Log.e(TAG, "ERROR: fail to load license file");
            return false;
        }
        Log.d(TAG, "Load license file successfully");
        return true;
    }
    
    private boolean loadDB() {
        AssetManager as = getResources().getAssets();
        int ret = 0;
        try {
            File file = new File(VDB_PATH + HIKARI_VDB);
            if (!file.exists()) {
                Log.d(TAG, "Load vdb file ...");
                InputStream input = as.open(HIKARI_VDB);
                FileOutputStream output = new FileOutputStream(file);
                int DEFAULT_BUFFER_SIZE = 1024 * 4;
                byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
                int n = 0;
                while ((n = input.read(buffer)) != -1) {
                  output.write(buffer, 0, n);
                }
                input.close();
                output.close();
                Log.d(TAG, "done");
            } else {
                Log.d(TAG, "vdb file already exists");
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "\rERROR: fail to load database file");
            ret = -1;
        }
        // load tts
        ret = HIKARI.LOADTTS(VDB_PATH, mLicense);
        Log.d(TAG, "Load tts engine ...");
        if (ret != 0) {
            Log.e(TAG, "\rERROR: LOADTTS error=" + ret);
            return false;
        }
        Log.d(TAG, "done");
        return true;
    }
    
    private void talk(String text) {
        if (text == null) {
            Log.e(TAG, "talk text is null");
            tryAgain();
        }

        Log.d(TAG, "Now talking: " + text + "\n");
        isTalking = true;
        int minBufSize = AudioTrack.getMinBufferSize(
                16000,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        // check frame size (3rd argument is -1)
        HIKARI.TextToBuffer(0, null, FLAG_SIZE_CHECK, -1, -1, -1, -1, -1, -1);
        int frameSize = HIKARI.TextToBufferRTN();
        Log.d(TAG, "minBufSize=" + minBufSize + " frameSize=" + frameSize);
        // adjust buffer size
        if (frameSize < minBufSize)
            frameSize = minBufSize;

        AudioTrack at = new AudioTrack(AudioManager.STREAM_MUSIC,
                16000, 
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                frameSize,
                AudioTrack.MODE_STREAM);
        at.play();
        byte[] audioData = null;
        int flag = FLAG_FIRST_FRAME;
        int ret = 0;
        do {
            audioData = HIKARI.TextToBuffer(0, text, flag, -1, -1, -1, -1, 1, -1);
            ret = HIKARI.TextToBufferRTN();
            Log.d(TAG, "TextToBufferRTN=" + ret + "(" + flag + ")");
            if (audioData != null) {
                at.write(audioData, 0, audioData.length);
            }
            flag = FLAG_ANOTHRE_FRAME;
        } while (ret == 0);
        at.flush();
        at.stop();
        isTalking = false;
    }

    private void tryAgain() {
        Log.d(TAG, "try again to speak!");
        talk(getString(R.string.please_again));
        messageRetry();
    }

    // get response from engine
    @Override
    public void onResult(String response) {
        talk(response);

        if (LOGGING_ON) {
            logging(response);
        }
}

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

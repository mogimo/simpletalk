package com.example.simpletalk;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import kr.co.voiceware.HIKARI;
import android.content.Context;
import android.content.res.AssetManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.SoundPool;
import android.util.Log;

public class HoyaVoiceText implements VoiceEngine, AudioTrack.OnPlaybackPositionUpdateListener {
    private final static boolean DEBUG = BuildConfig.DEBUG;
    private final static String TAG = "SimpleTalk";

    private final Context mContext;

    private final byte[] mLicense =  new byte[2048];
    private final boolean USE_BUFFER = true;
    private static final String HIKARI_VDB = "tts_single_db_hikari.vtdb";
    private static final String VDB_PATH = "/sdcard/";
    private static final int FLAG_SIZE_CHECK = -1;
    private static final int FLAG_FIRST_FRAME = 0;
    private static final int FLAG_ANOTHRE_FRAME = 1;

    private AudioTrack mTrack;

    public HoyaVoiceText(Context context) {
        this.mContext = context;
    }

    @Override
    public void onMarkerReached(AudioTrack track) {
        Log.d(TAG, "onMarkerReached");
        mTrack.stop();
    }

    @Override
    public void onPeriodicNotification(AudioTrack track) {
        Log.d(TAG, "onPeriodicNotification");
    }

    private boolean readLicence() {
        int ret = 0;
        try {
            ret =  mContext.getResources().openRawResource(R.raw.verification).read(mLicense);
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

    private void copyDB() {
        AssetManager as = mContext.getResources().getAssets();
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
        }
    }

    private boolean loadDB() {
        copyDB();

        // load tts
        int ret = HIKARI.LOADTTS(VDB_PATH, mLicense);
        Log.d(TAG, "Load tts engine ...");
        if (ret != 0) {
            Log.e(TAG, "\rERROR: LOADTTS error=" + ret);
            return false;
        }
        Log.d(TAG, "done");
        return true;
    }

    private void createAudioTrack() {
        if (mTrack == null) {
            int minBufSize = AudioTrack.getMinBufferSize(
                    16000,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            // check frame size (3rd argument is -1)
            HIKARI.TextToBuffer(0, null, FLAG_SIZE_CHECK, -1, -1, -1, -1, -1, -1);
            int frameSize = HIKARI.TextToBufferRTN();
            Log.d(TAG, "minBufSize=" + minBufSize + " frameSize=" + frameSize);
            // adjust buffer size
            if (frameSize < minBufSize) {
                // getMinBufferSize以下じゃないと再生されないことがある
                frameSize = minBufSize;
            }
            mTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                    16000,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    frameSize,
                    AudioTrack.MODE_STREAM);
            mTrack.setStereoVolume(AudioTrack.getMaxVolume(), AudioTrack.getMaxVolume());
            mTrack.setPlaybackPositionUpdateListener(this);
        }
    }

    @Override
    public void init() {
        String version = HIKARI.GetVersion();
        Log.d(TAG, "Engine: " + version + "\n");
        if (readLicence()) {
            loadDB();
        }

        createAudioTrack();
    }

    @Override
    public void release() {
        HIKARI.UNLOADTTS();
        mTrack = null;
    }

    private void talkByBuffer(String text) {
        if (mTrack == null) {
            Log.e(TAG, "AudioTrack is something wrong...");
            return;
        }

        if (DEBUG) Log.d(TAG, "Now talking: " + text + "\n");

        byte[] audioData = null;
        int flag = FLAG_FIRST_FRAME;
        int ret = 0, length = 0, repeat = 0;
        mTrack.play();
        do {
            audioData = HIKARI.TextToBuffer(0, text, flag, -1, -1, -1, -1, 1, -1);
            ret = HIKARI.TextToBufferRTN();
            if (audioData != null) {
                length = audioData.length;
                mTrack.write(audioData, 0, length);
            }
            if (DEBUG) Log.d(TAG, "TextToBufferRTN=" + ret + "(" + flag + ") length=" + length);
            flag = FLAG_ANOTHRE_FRAME;
            repeat++;
        } while (ret == 0);
        mTrack.flush();
        mTrack.stop(); // ここでstopしないとうまくflushされず遅延することがある
        mTrack.setNotificationMarkerPosition(length);
    }

    private void talkByFile(String text) {
        if (DEBUG) Log.d(TAG, "Now talking: " + text + "\n");

        final String filename = (mContext.getFilesDir().getAbsolutePath()+"/voice.wav");;
        int ret = HIKARI.TextToFile(HIKARI.VT_FILE_API_FMT_S16PCM_WAVE,
                text, filename, -1, -1, -1, -1, 0, -1);
        if (ret > 0) {
            SoundPool sp = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
            final int id = sp.load(filename, 1);
            sp.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
                public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                    soundPool.play(id, 1.0f, 1.0f, 1, 0, 1.0f);
                }
            });
        }
    }

    @Override
    public void talk(String text) {
        if (USE_BUFFER) {
            talkByBuffer(text);
        } else {
            talkByFile(text);
        }
    }

}

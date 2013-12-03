package com.example.simpletalk;

import android.content.Context;
import android.util.Log;
import jp.aij.microaitalkjni.MicroAITalkJni;
import jp.aij.microaitalkjni.MicroAITalkJni.ErrCode;

public class AITalk implements VoiceEngine {
    private final static boolean DEBUG = BuildConfig.DEBUG;
    private final static String TAG = "SimpleTalk";

    private final Context mContext;

    private MicroAITalkJni mAITalk = MicroAITalkJni.getInstance();
    private StreamThreads mAudioStream   = StreamThreads.getInstance();

    public AITalk(Context context) {
        this.mContext = context;
    }

    @Override
    public void init() {
        final String voiceFile = "nozomi_n16";

        ErrCode ret = mAITalk.init();
        if (ret != ErrCode.NONE) {
            Log.e(TAG, "AI Talk init() error=" + ret);
        }
        //true = compact, false = normal
        ret = mAITalk.loadLang(true);
        if (ret != ErrCode.NONE) {
            Log.e(TAG, "AI Talk loadLang() error=" + ret);
        }
        ret = mAITalk.loadVoice(voiceFile);
        if (ret != ErrCode.NONE) {
            Log.e(TAG, "AI Talk loadVoice() error=" + ret);
        }
    }

    @Override
    public void release() {
        ErrCode ret = mAITalk.unloadLang();
        if (ret != ErrCode.NONE) {
            Log.e(TAG, "AI Talk unloadLang() error=" + ret);
        }
        ret = mAITalk.unloadVoice();
        if (ret != ErrCode.NONE) {
            Log.e(TAG, "AI Talk unloadVoice() error=" + ret);
        }
        ret = mAITalk.end();
        mAudioStream.stopStream();
    }

    @Override
    public void talk(String text) {
        if (mAudioStream.getStreamStatus() != 0) {
            mAudioStream.stopStream();
        }
        mAudioStream.StreamStart(text);
    }

}

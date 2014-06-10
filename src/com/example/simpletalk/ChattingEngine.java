package com.example.simpletalk;

import android.content.Context;

public class ChattingEngine implements Engine {
    private final static boolean DEBUG = BuildConfig.DEBUG;
    private final static String TAG = "SimpleTalk:Chatting";
    private ResponseListener mListener;
    private Context mContext;
    private Greeting mGreeting;

    public ChattingEngine(Context context) {
        this.mContext = context;
        mGreeting = new Greeting(context);
    }

    @Override
    public void request(String sentence) {
        String answer = mGreeting.greeting(mContext, sentence);
        mListener.onResult(answer);
    }

    @Override
    public void setResponseListener(ResponseListener listener) {
        this.mListener = listener;
    }

}

package com.example.simpletalk;

import com.example.simpletalk.Engine.ResponseListener;

public interface VoiceEngine {
    public void init();
    public void release();
    public void talk(String text);
    public boolean hasListener();
    public void setProcessListener(ProcessListener listener);

    public interface ProcessListener {
        public void onStart();
        public void onEnd();
    }
}

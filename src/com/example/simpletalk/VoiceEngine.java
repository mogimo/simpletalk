package com.example.simpletalk;

public interface VoiceEngine {
    public void init();
    public void release();
    public void talk(String text);
}

package com.example.simpletalk;

public interface Recognizer {
    public static final int ERROR_NETWORK = 1;
    public static final int ERROR_SERVER = 2;
    public static final int ERROR_CLIENT = 3;
    public static final int ERROR_NO_MATCH = 4;
    public static final int ERROR_TIMEOUT = 5;

    public void init();
    public void start();
    public void stop();
    public void release();
    public void setRecognizerListener(RecognizerListener listener);

    public interface RecognizerListener {
        public void onError(int error);
        public void onReady();
        public void onRecognize(String response, float score);
    }
}

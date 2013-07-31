package com.example.simpletalk;

public interface Engine {
    public void request(String sentence);
    public void setResponseListener(ResponseListener listener);

    public interface ResponseListener {
        public void onResult(String response);
    }
}

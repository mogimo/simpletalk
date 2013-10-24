package com.example.simpletalk;

public class SimpleToken {
    private String mSurface;
    private String mPartOfSpeech;

    public SimpleToken() {
    }

    public SimpleToken(String surface, String partOfSpeech) {
        this.mSurface = surface;
        this.mPartOfSpeech = partOfSpeech;
    }

    public String getSurface() {
        return mSurface;
    }

    public String getPartOfSpeech() {
        return mPartOfSpeech;
    }

    public void putSurface(String surface) {
        this.mSurface = surface;
    }

    public void putPartOfSpeech(String partOfSpeech) {
        this.mPartOfSpeech = partOfSpeech;
    }

}

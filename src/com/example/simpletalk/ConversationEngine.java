package com.example.simpletalk;

import android.content.Context;

public class ConversationEngine {
    private Context mContext;
    
    public ConversationEngine(Context context) {
        this.mContext = context;
    }
    
    public String getResponse(String text) {
        String response = Greeting.getResponse(mContext, text);
        if (response == null) {
            //
        }
        return response;
    }

}

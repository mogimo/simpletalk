package com.example.simpletalk;

import java.util.List;

import android.content.Context;

public class Parrot {
    private static int lastIndex(List<SimpleToken> tokens, String pos) {
        int lastIndex = tokens.size() - 1;
        for (int i=lastIndex; i>=0; i--) {
            SimpleToken token = tokens.get(i);
            if (token.getPartOfSpeech().startsWith(pos)) {
                return i;
            }
        }
        return -1;
    }

    /*
     * 形容詞 + 助動詞 + 助詞 + 助詞 ... 暑いですよね
     * 形容詞 + 助動詞 + 助詞               ... 暑いですね
     * 形容詞 + 助詞 + 助詞                   ... 暑いよね
     * 形容詞 + 助詞                                 ... 暑いね
     */
    public static String parrot(Context context, List<SimpleToken> tokens) {
        // 形容詞
        String ADJECTIVE = context.getResources().getString(R.string.adjective);
        // 助動詞
        String AUXILIARY_VERB = context.getResources().getString(R.string.auxiliary_verb);
        // 助詞
        String POSTPOSITION = context.getResources().getString(R.string.postposition);

        int count = tokens.size();
        if (count < 2) {
            return null;
        }
        int lastIndex = count - 1;
        // 形容詞
        int index = lastIndex(tokens, ADJECTIVE);
        if (index < 0 || (lastIndex - index) < 1) {
            // not found
            return null;
        }
        StringBuilder reply = new StringBuilder();
        reply.append(tokens.get(index).getSurface());
        // next word
        if (index++ > lastIndex) {
            return null;
        }
        if (tokens.get(index).getPartOfSpeech().startsWith(AUXILIARY_VERB)) {
            // 助動詞
            reply.append(tokens.get(index).getSurface());
            // next word
            if (index++ > lastIndex) {
                return null;
            }
            if (tokens.get(index).getPartOfSpeech().startsWith(POSTPOSITION)) {
                // 助詞
                if (lastIndex == index) {
                    reply.append(tokens.get(index).getSurface());
                    return reply.toString();
                }
                // next word
                if (index++ > lastIndex) {
                    return null;
                }
                if (tokens.get(index).getPartOfSpeech().startsWith(POSTPOSITION) && lastIndex == index) {
                    // 助詞
                    reply.append(tokens.get(index).getSurface());
                    return reply.toString();
                }
            }
        } else if (tokens.get(index).getPartOfSpeech().startsWith(POSTPOSITION)) {
            // 助詞
            if (lastIndex == index) {
                reply.append(tokens.get(index).getSurface());
                return reply.toString();
            }
            // next word
            if (index++ > lastIndex) {
                return null;
            }
            if (tokens.get(index).getPartOfSpeech().startsWith(POSTPOSITION) && lastIndex == index) {
                // 助詞
                reply.append(tokens.get(index).getSurface());
                return reply.toString();
            }
        }
        return null;
    }
}

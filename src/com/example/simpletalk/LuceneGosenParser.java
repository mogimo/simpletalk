package com.example.simpletalk;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.java.sen.SenFactory;
import net.java.sen.StringTagger;
import net.java.sen.dictionary.Token;

public class LuceneGosenParser {
    public static ArrayList<SimpleToken> parse(String text) {
        if (text == null) {
            return null;
        }

        ArrayList<SimpleToken> parsedTokens = new ArrayList<SimpleToken>();

        StringTagger tagger = SenFactory.getStringTagger(null);
        List<Token> tokens = new ArrayList<Token>();
        try {
            tagger.analyze(text, tokens);
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (Token token : tokens) {
            SimpleToken word = new SimpleToken();
            word.putSurface(token.getSurface());
            word.putPartOfSpeech(token.getMorpheme().getPartOfSpeech());
            parsedTokens.add(word);
        }

        return parsedTokens;
    }
}

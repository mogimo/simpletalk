package com.example.simpletalk;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import net.java.sen.dictionary.Token;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.util.Xml;

public class YahooMorphoParser {

    public static ArrayList<SimpleToken> parse(String xml) {
        int count = 0;
        ArrayList<SimpleToken> parsedTokens = new ArrayList<SimpleToken>();
        XmlPullParser parser = Xml.newPullParser();
        try {
            parser.setInput(new StringReader(xml));

            int eventType = parser.getEventType();
            String reading = null;
            String pos = null;
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_DOCUMENT) {
                    // start parse
                } else if (eventType == XmlPullParser.START_TAG) {
                    if (parser.getName().equals("ma_result")) {
                        // do nothing
                    } else if (parser.getName().equals("total_count")) {
                        count = Integer.parseInt(parser.nextText());
                    } else if (parser.getName().equals("reading")) {
                        reading = parser.nextText();
                    } else if (parser.getName().equals("pos")) {
                        pos = parser.nextText();
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    if (parser.getName().equals("word")) {
                        if (reading != null & pos != null) {
                            SimpleToken word = new SimpleToken();
                            word.putSurface(reading);
                            reading = null;
                            word.putPartOfSpeech(pos);
                            pos = null;
                            parsedTokens.add(word);
                        }
                    }
                }
                eventType = parser.next();
            }
            return parsedTokens;
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}

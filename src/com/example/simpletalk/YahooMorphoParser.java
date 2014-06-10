package com.example.simpletalk;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.os.AsyncTask;
import android.util.Log;
import android.util.Xml;

public class YahooMorphoParser {
    private final static boolean DEBUG = BuildConfig.DEBUG;
    private final static String TAG = "SimpleTalk:MorphologicalAnalysis";

    private final static int TIMEOUT = 5000;
    private final static int TIMESLICE = 1000;
    // for Yahoo! parser
    private final static String YAHOO_MORPHO_URL =
        "http://jlp.yahooapis.jp/MAService/V1/parse";
    private final static String APP_ID =
        "dj0zaiZpPUZrMk1HTUwxalZoOSZkPVlXazlaWGcwYkdWaE0yTW1jR285TUEtLSZzPWNvbnN1bWVyc2VjcmV0Jng9YTk-";

    private Map<String, String> mPayload = new HashMap<String, String>();
    private HttpRequestTask mHttpPost;
    private boolean isDone = false;
    private List<SimpleToken> mTokens = new ArrayList<SimpleToken>();

    public YahooMorphoParser() {
        mPayload.put("appid", APP_ID);
        mPayload.put("results", "ma");
        mPayload.put("response", "surface,reading,pos");
    }

    public List<SimpleToken> parse(String sentence) {
        mPayload.put("sentence", sentence);
        mHttpPost = new HttpRequestTask(YAHOO_MORPHO_URL, mPayload);
        mHttpPost.execute();

        int duration = 0;
        int timeout = TIMEOUT;
        try {
            while(duration < timeout) {
                Thread.sleep(TIMESLICE);
                if (isDone) {
                    break;
                }
                duration += TIMESLICE;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return mTokens;
    }

    private void parseXml(String xml) {
        int count = 0;
        if (mTokens != null && !mTokens.isEmpty()) {
            mTokens.clear();
        }
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
                            mTokens.add(word);
                        }
                    }
                }
                eventType = parser.next();
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class HttpRequestTask extends AsyncTask<Void, Void, Void> {
        private final String mUrl;
        private final Map<String, String>mPayload;
        private HttpRequest request = new HttpRequest();

        HttpRequestTask(final String url, final Map<String, String>payload) {
            this.mUrl = url;
            this.mPayload = payload;
            isDone = false;
        }

        @Override
        protected Void doInBackground(Void... params) {
            parseXml(request.post(mUrl, mPayload));
            return null;
        }

        @Override
        protected void onPostExecute(Void param) {
            isDone = true;
        }
    }

}

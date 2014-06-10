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

public class WordSearch {
    private final static boolean DEBUG = BuildConfig.DEBUG;
    private final static String TAG = "SimpleTalk:WordSearch";

    private final static int TIMEOUT = 5000;
    private final static int TIMESLICE = 1000;
    private final static String MEDIAWIKI_URL = "http://ja.wikipedia.org/w/api.php?";
    private Map<String, String> mPayload = new HashMap<String, String>();
    private MediaWikiTask mSearch;
    private List<String> mResult = new ArrayList<String>();
    private boolean isDone = false;


    public WordSearch() {
        setDefaultPayload();
    }

    private void setDefaultPayload() {
        mPayload.put("format", "xml");
        mPayload.put("action", "query");
    }

    public List<String> getCategories(String word) {
        if (mResult != null && !mResult.isEmpty()) {
            mResult.clear();
        }
        mPayload.put("prop", "categories");
        mPayload.put("titles", word);
        mSearch = new MediaWikiTask(MEDIAWIKI_URL, mPayload);
        mSearch.execute();

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
        return mResult;
    }

    private ArrayList<String> parseXml(String xml) {
        ArrayList<String> categories = new ArrayList<String>();
        XmlPullParser parser = Xml.newPullParser();
        try {
            parser.setInput(new StringReader(xml));
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_DOCUMENT) {
                    // start parse
                } else if (eventType == XmlPullParser.START_TAG) {
                    if (parser.getName().equals("cl")) {
                        String category = parser.getAttributeValue(null, "title");
                        categories.add(category.substring(category.indexOf(':')+1));
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    // do nothing ...
                }
                eventType = parser.next();
            }
            return categories;
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private class MediaWikiTask extends AsyncTask<Void, Void, Void> {
        private final String mUrl;
        private final Map<String, String>mPayload;
        private HttpRequest request = new HttpRequest();

        MediaWikiTask(final String url, final Map<String, String>payload) {
            this.mUrl = url;
            this.mPayload = payload;
            isDone = false;
        }

        @Override
        protected Void doInBackground(Void... params) {
            String response = request.post(mUrl, mPayload);
            mResult = parseXml(response);
            isDone = true;
            return null;
        }

        @Override
        protected void onPostExecute(Void param) {
        }
    }

}

package com.example.simpletalk;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Xml;

public class WordSearch {
    private final static boolean DEBUG = BuildConfig.DEBUG;
    private final static String TAG = "SimpleTalk:WordSearch";

    public final static String MISSING = "missing";

    private Context mContext;
    private final static int TIMEOUT = 5000;
    private final static int TIMESLICE = 1000;
    private final static String MEDIAWIKI_URL = "http://ja.wikipedia.org/w/api.php?";
    private Map<String, String> mPayload = new HashMap<String, String>();
    private MediaWikiTask mSearch;
    private enum Kinds {CATEGORY, CONTENT};
    private List<String> mCategories = new ArrayList<String>();
    private String mContent;
    private boolean isDone = false;


    public WordSearch(Context context) {
        mContext = context;
        setDefaultPayload();
    }

    private void setDefaultPayload() {
        mPayload.put("format", "xml");
        mPayload.put("action", "query");
    }

    public String getContent(String word) {
        mContent = "";

        mPayload.put("prop", "revisions");
        mPayload.put("titles", word);
        mPayload.put("rvprop", "content");
        mSearch = new MediaWikiTask(MEDIAWIKI_URL, mPayload);
        mSearch.execute(Kinds.CONTENT);

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
        return mContent;
    }

    public List<String> getCategories(String word) {
        if (mCategories != null && !mCategories.isEmpty()) {
            mCategories.clear();
        }
        mPayload.put("prop", "categories");
        mPayload.put("titles", word);
        mSearch = new MediaWikiTask(MEDIAWIKI_URL, mPayload);
        mSearch.execute(Kinds.CATEGORY);

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
        return mCategories;
    }

    private String parseContent(String xml) {
        String content = MISSING;
        XmlPullParser parser = Xml.newPullParser();
        try {
            parser.setInput(new StringReader(xml));
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_DOCUMENT) {
                    // start parse
                } else if (eventType == XmlPullParser.START_TAG) {
                    if (parser.getName().equals("rev")) {
                        String contents = parser.nextText();
                        int end = contents.indexOf(
                                mContext.getResources().getString(R.string.period));
                        if (end != -1) {
                            content = contents.substring(0, end);
                        }
                        break;
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    // do nothing ...
                }
                eventType = parser.next();
            }
            return content;
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private ArrayList<String> parseCategories(String xml) {
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

    private class MediaWikiTask extends AsyncTask<Kinds, Void, Void> {
        private final String mUrl;
        private final Map<String, String>mPayload;
        private HttpRequest request = new HttpRequest();

        MediaWikiTask(final String url, final Map<String, String>payload) {
            this.mUrl = url;
            this.mPayload = payload;
            isDone = false;
        }

        @Override
        protected Void doInBackground(Kinds... params) {
            if (params.length < 1) {
                return null;
            }
            Kinds kind = params[0];
            if (kind == Kinds.CATEGORY) {
                String response = request.post(mUrl, mPayload);
                mCategories = parseCategories(response);
            } else if (kind == Kinds.CONTENT) {
                String response = request.post(mUrl, mPayload);
                mContent = parseContent(response);
            }
            isDone = true;
            return null;
        }

        @Override
        protected void onPostExecute(Void param) {
        }
    }

}

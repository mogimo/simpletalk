package com.example.simpletalk;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import android.util.Log;

public class HttpRequest {
    private final static boolean DEBUG = BuildConfig.DEBUG;
    private final static String TAG = "SimpleTalk";

    public String post(String uri, Map<String, String>payload) {
        URI url = null;

        try {
            url = new URI(uri);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        HttpPost request = new HttpPost(url);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        Set<String> keys = payload.keySet();
        for (String key : keys) {
            params.add(new BasicNameValuePair(key, payload.get(key)));
        }
        try {
            request.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        DefaultHttpClient httpClient = new DefaultHttpClient();
        String ret = null;
        try {
            ResponseHandler<String> handler = new SimpleResponseHandler<String>();
            ret = httpClient.execute(request, handler);
            if (DEBUG) Log.d(TAG, "http request: return: " + ret);
        } catch (IOException e) {
            if (DEBUG) Log.d(TAG, "http request: error:" + e.toString());
            ret = "error: " + e.toString();
        } finally {
            httpClient.getConnectionManager().shutdown();
        }

        return ret;
    }

    private class SimpleResponseHandler<String> implements ResponseHandler<String> {
        @Override
        public String handleResponse(HttpResponse response) throws IOException {
            if (DEBUG) Log.d(TAG, "http response: code=" + response.getStatusLine().getStatusCode());
            int code = response.getStatusLine().getStatusCode();
            String str = null;
            switch (code) {
                case HttpStatus.SC_OK:
                    if (DEBUG) Log.d(TAG, "http response: Status OK");
                    HttpEntity entity = response.getEntity();
                    str = (String) EntityUtils.toString(entity, "UTF-8");
                    break;
                case HttpStatus.SC_NOT_FOUND:
                    if (DEBUG) Log.d(TAG, "http response: Not Found");
                    break;
                default:
                    if (DEBUG) Log.d(TAG, "http response: Error");
                    str = (String) ("code=" + code + " " 
                            + response.getStatusLine().getReasonPhrase());
                    break;
            }
            if (DEBUG) Log.d(TAG, "http response: " + str);
            return str;
        }
    }
}

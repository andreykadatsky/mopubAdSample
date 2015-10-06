package com.loopme.tasks;

import com.loopme.AdFormat;
import com.loopme.AdParams;
import com.loopme.Logging;
import com.loopme.Logging.LogLevel;
import com.loopme.LoopMeError;
import com.loopme.ResponseParser;
import com.loopme.StaticParams;
import com.loopme.Utils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;

public class AdFetcher implements Runnable {

    private static final String LOG_TAG = AdFetcher.class.getSimpleName();

    private final String mRequestUrl;
    private Listener mListener;

    private int mFormat;

    /*
     * Timeout for response from server 20 seconds
     */
    static int TIMEOUT = 20000;

    private static final String USER_AGENT = "User-Agent";
    private static final String AGENT_PROPERTY = "http.agent";

    private static final String INVALID_APPKEY_MESS = "Missing or invalid app key";
    private static final String PAGE_NOT_FOUND = "Page not found";

    private LoopMeError mLoopMeError;

    public interface Listener {
        void onComplete(AdParams params, LoopMeError error);
    }

    public AdFetcher(String requestUrl, Listener listener, int format) {
        mRequestUrl = requestUrl;
        mListener = listener;
        mFormat = format;
    }

    @Override
    public void run() {
        String result = getResponse(mRequestUrl);

        if (result == null) {
            complete(null, mLoopMeError);
        } else {
            ResponseParser parser = new ResponseParser(new ResponseParser.Listener() {

                @Override
                public void onParseError(LoopMeError error) {
                    complete(null, error);
                }
            }, mFormat);
            AdParams adParams = parser.getAdParams(result);
            if (adParams != null) {
                complete(adParams, null);
            }
        }
    }

    private void complete(final AdParams params, final LoopMeError error) {
        if (mListener != null) {
            mListener.onComplete(params, error);
        }
    }

    public String getResponse(String url) {
        String result = null;
        HttpURLConnection urlConnection = null;
        try {
            URL request = new URL(url);
            urlConnection = (HttpURLConnection) request.openConnection();
            urlConnection.setRequestProperty(USER_AGENT, System.getProperty(AGENT_PROPERTY));
            urlConnection.setReadTimeout(TIMEOUT);
            urlConnection.setConnectTimeout(TIMEOUT);

            String type = mFormat == AdFormat.INTERSTITIAL ? StaticParams.INTERSTITIAL_TAG
                    : StaticParams.BANNER_TAG;
            Logging.out(LOG_TAG, type + " loads ad with URL: " + url, LogLevel.DEBUG);

            int status = urlConnection.getResponseCode();
            Logging.out(LOG_TAG, "status code: " + status, LogLevel.DEBUG);
            handleStatusCode(status);

            if (status == HttpURLConnection.HTTP_NOT_FOUND) {
                InputStream errorStream = urlConnection.getErrorStream();
                String errorString = Utils.getStringFromStream(errorStream);
                if (errorString != null && errorString.contains(INVALID_APPKEY_MESS)) {
                    mLoopMeError = new LoopMeError(INVALID_APPKEY_MESS);
                } else {
                    mLoopMeError = new LoopMeError(PAGE_NOT_FOUND);
                }
                return null;
            }

            if (status == HttpURLConnection.HTTP_OK) {
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                if (in != null) {
                    result = Utils.getStringFromStream(in);
                } else {
                    return null;
                }
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
            mLoopMeError = new LoopMeError("Error during establish connection");

        } catch (NullPointerException e) {
            e.printStackTrace();
            mLoopMeError = new LoopMeError("Error during establish connection");

        } catch (SocketTimeoutException e) {
            mLoopMeError = new LoopMeError("Request timeout");

        } catch (IOException e) {
            e.printStackTrace();
            mLoopMeError = new LoopMeError("Error during establish connection");

        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return result;
    }

    private void handleStatusCode(int statusCode) {
        switch (statusCode) {
            case HttpURLConnection.HTTP_NO_CONTENT:
                mLoopMeError = new LoopMeError("No ads found");
                break;

            case HttpURLConnection.HTTP_NOT_FOUND:
                break;

            case HttpURLConnection.HTTP_OK:
                break;

            default:
                mLoopMeError = new LoopMeError("Unknown server code " + statusCode);
                break;
        }
    }
}

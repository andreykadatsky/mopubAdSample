package com.loopme;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.loopme.Logging.LogLevel;

/**
 * Custom WebViewClient for AdBrowserWebView which handles different url schemes.
 * Has listener to communicate with buttons on AdBrowserLayout.
 */
class AdBrowserWebViewClient extends WebViewClient {

    private static final String LOG_TAG = AdBrowserWebViewClient.class.getSimpleName();

    private static final String HEADER_PLAIN_TEXT = "plain/text";

    private static final String TEL_SCHEME = "tel";
    private static final String MAILTO_SCHEME = "mailto";
    private static final String GEO_SCHEME = "geo";
    private static final String MARKET_SCHEME = "market";
    private static final String YOUTUBE_SCHEME = "vnd.youtube";
    private static final String HTTP_SCHEME = "http";
    private static final String HTTPS_SCHEME = "https";

    private static final String GEO_HOST = "maps.google.com";
    private static final String MARKET_HOST = "play.google.com";
    private static final String YOUTUBE_HOST1 = "www.youtube.com";
    private static final String YOUTUBE_HOST2 = "m.youtube.com";

    private Listener mListener;

    private Listener mEmptyListener = new Listener() {
        @Override
        public void onPageStarted() {

        }

        @Override
        public void onPageFinished(boolean canGoBack) {

        }

        @Override
        public void onReceiveError() {

        }

        @Override
        public void onLeaveApp() {

        }
    };

    public interface Listener {
        void onPageStarted();

        void onPageFinished(boolean canGoBack);

        void onReceiveError();

        void onLeaveApp();
    }

    public AdBrowserWebViewClient(Listener listener) {
        if (listener == null) {
            Logging.out(LOG_TAG, "Error: Wrong listener", LogLevel.ERROR);
            mListener = mEmptyListener;
        } else {
            mListener = listener;
        }
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        Logging.out(LOG_TAG, "shouldOverrideUrlLoading url=" + url, LogLevel.DEBUG);
        Context context = view.getContext();

        Uri uri = null;
        try {
            uri = Uri.parse(url);
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }
        if (uri == null) {
            return false;
        }

        String scheme = uri.getScheme();
        String host = uri.getHost();

        if (scheme.equalsIgnoreCase(TEL_SCHEME)) {
            Intent intent = new Intent(Intent.ACTION_DIAL, uri);
            resolveAndStartActivity(intent, context);

        } else if (scheme.equalsIgnoreCase(MAILTO_SCHEME)) {
            url = url.replaceFirst("mailto:", "");
            url = url.trim();
            Intent intent = new Intent(Intent.ACTION_SEND, uri);
            intent.setType(HEADER_PLAIN_TEXT).putExtra(Intent.EXTRA_EMAIL, new String[]{url});
            resolveAndStartActivity(intent, context);

        } else if (scheme.equalsIgnoreCase(GEO_SCHEME)) {
            Intent searchAddress = new Intent(Intent.ACTION_VIEW, uri);
            resolveAndStartActivity(searchAddress, context);

        } else if (scheme.equalsIgnoreCase(MARKET_SCHEME)
                || scheme.equalsIgnoreCase(YOUTUBE_SCHEME)) {
            leaveApp(url, context);

        } else if (scheme.equalsIgnoreCase(HTTP_SCHEME)
                || scheme.equalsIgnoreCase(HTTPS_SCHEME)) {
            return checkHost(url, host, context);

        } else {
            return true;
        }

        return true;
    }

    /**
     * Checks host
     *
     * @param url  - full url
     * @param host - host from url
     * @return true - if param host equals with geo, market or youtube host
     * false - otherwise
     */
    private boolean checkHost(String url, String host, Context context) {
        if (host.equalsIgnoreCase(GEO_HOST)) {
            Intent searchAddress = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            resolveAndStartActivity(searchAddress, context);

        } else if (host.equalsIgnoreCase(MARKET_HOST)
                || host.equalsIgnoreCase(YOUTUBE_HOST1)
                || host.equalsIgnoreCase(YOUTUBE_HOST2)) {
            leaveApp(url, context);

        } else {
            return false;
        }
        return true;
    }

    private void leaveApp(String url, Context context) {

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        resolveAndStartActivity(intent, context);
        mListener.onLeaveApp();
    }

    private void resolveAndStartActivity(Intent intent, Context context) {
        boolean isActivityResolved = context.getPackageManager()
                .resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null
                ? true : false;
        if (isActivityResolved) {
            context.startActivity(intent);
        } else {
            mListener.onReceiveError();
        }
    }

    @Override
    public final void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        mListener.onPageStarted();
    }

    @Override
    public final void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        mListener.onPageFinished(view.canGoBack());
    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        super.onReceivedError(view, errorCode, description, failingUrl);

        String mess = "onReceivedError code: " + errorCode + " description: " + description;
        Logging.out(LOG_TAG, mess, LogLevel.ERROR);
        mListener.onReceiveError();
    }
}

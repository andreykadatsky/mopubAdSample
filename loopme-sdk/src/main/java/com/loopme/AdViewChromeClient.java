package com.loopme;

import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

public class AdViewChromeClient extends WebChromeClient {

    private static final String LOG_TAG = AdViewChromeClient.class.getSimpleName();

    @Override
    public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
        Logging.out(LOG_TAG, "Console Message: " + consoleMessage.message(),
                Logging.LogLevel.DEBUG);
        return super.onConsoleMessage(consoleMessage);
    }

    @Override
    public void onProgressChanged(WebView view, int newProgress) {
        super.onProgressChanged(view, newProgress);
    }
}

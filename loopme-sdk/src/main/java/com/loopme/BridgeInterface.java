package com.loopme;

public interface BridgeInterface {

    //webview commands
    void setWebViewState(int state);

    void setFullscreenMode(boolean mode);

    //video commands
    void setVideoState(int state);

    void setVideoDuration(int duration);

    void setVideoCurrentTime(int currentTime);

    void setVideoMute(boolean mute);

    void sendNativeCallFinished();
}

package com.loopme;

public class LoopMeError {

    private String mErrorMessage;

    public LoopMeError(String errMessage) {
        mErrorMessage = errMessage;
    }

    public String getMessage() {
        return mErrorMessage != null ? mErrorMessage : "Unknown error";
    }
}

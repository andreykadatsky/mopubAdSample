package com.loopme;

public class WebviewState {

    public static final int VISIBLE = 1;
    public static final int HIDDEN = 2;
    public static final int CLOSED = 3;

    private WebviewState() {}

    public static String toString(int state) {
        switch (state) {
            case VISIBLE:
                return "VISIBLE";

            case HIDDEN:
                return "HIDDEN";

            case CLOSED:
                return "CLOSED";

            default:
                return "UNKNOWN";
        }
    }
}

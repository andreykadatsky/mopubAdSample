package com.loopme;

class VideoState {

    public static final int BROKEN = 0;
    public static final int READY = 1;
    public static final int PLAYING = 2;
    public static final int PAUSED = 3;
    public static final int COMPLETE = 4;

    public static String toString(int state) {
        switch (state) {
            case BROKEN:
                return "BROKEN";

            case READY:
                return "READY";

            case PLAYING:
                return "PLAYING";

            case PAUSED:
                return "PAUSED";

            case COMPLETE:
                return "COMPLETE";

            default:
                return "UNKNOWN";
        }
    }

    private VideoState() {}
}

package com.loopme;

/**
 * Class helper to build LoopMe javascript bridge commands
 */
class BridgeCommandBuilder {

    private static final String PREFIX = "javascript:window.L.bridge.set";

    String isNativeCallFinished(boolean b) {
        StringBuilder builder = new StringBuilder();
        builder.append(PREFIX)
                .append("('webview', {isNativeCallFinished: ")
                .append("'")
                .append(b)
                .append("'});");
        return builder.toString();
    }

    String shake(boolean b) {
        StringBuilder builder = new StringBuilder();
        builder.append(PREFIX)
                .append("('webview', {shake: ")
                .append("'")
                .append(b)
                .append("'});");
        return builder.toString();
    }

    String videoMute(boolean b) {
        StringBuilder builder = new StringBuilder();
        builder.append(PREFIX)
                .append("('video', {mute: ")
                .append(b)
                .append("});");
        return builder.toString();
    }

    String videoCurrentTime(int time) {
        StringBuilder builder = new StringBuilder();
        builder.append(PREFIX)
                .append("('video', {currentTime: ")
                .append(time)
                .append("});");
        return builder.toString();
    }

    String videoDuration(int time) {
        StringBuilder builder = new StringBuilder();
        builder.append(PREFIX)
                .append("('video', {duration: ")
                .append(time)
                .append("});");
        return builder.toString();
    }

    String videoState(int state) {
        StringBuilder builder = new StringBuilder();
        builder.append(PREFIX)
                .append("('video', {state: ")
                .append("'")
                .append(VideoState.toString(state))
                .append("'});");
        return builder.toString();
    }

    String webviewState(int state) {
        StringBuilder builder = new StringBuilder();
        builder.append(PREFIX)
                .append("('webview', {state: ")
                .append("'")
                .append(WebviewState.toString(state))
                .append("'});");
        return builder.toString();
    }

    String fullscreenMode(boolean b) {
        StringBuilder builder = new StringBuilder();
        builder.append(PREFIX)
                .append("('webview', {fullscreenMode: ")
                .append(b)
                .append("});");
        return builder.toString();
    }
}

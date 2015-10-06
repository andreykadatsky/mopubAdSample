package com.loopme.tasks;

import com.loopme.Logging;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class VideoHelper {

    private static final String LOG_TAG = VideoHelper.class.getSimpleName();

    /**
     * 127 chars is max lenght of file name with extension (4 chars)
     */
    private static final int MAX_FILE_NAME_LENGHT = 127 - 4;

    private static final String MP4_FORMAT = ".mp4";

    /**
     * Timeout for downloading video (3 minutes)
     */
    private static final int TIMEOUT = 3 * 60 * 1000;

    String detectFileName(String videoUrl) {
        String fileName = null;
        try {
            URL url = new URL(videoUrl);
            fileName = url.getFile();
            if (fileName != null && !fileName.isEmpty()) {

                if (!fileName.endsWith(MP4_FORMAT)) {
                    Logging.out(LOG_TAG, "Wrong video url (not .mp4 format)", Logging.LogLevel.DEBUG);
                    return null;

                } else {
                    fileName = fileName.replace(MP4_FORMAT, "");
                    int lastSlash = fileName.lastIndexOf("/");
                    int lenght = fileName.length();
                    fileName = fileName.substring(lastSlash + 1, lenght);

                    if (fileName.length() > MAX_FILE_NAME_LENGHT) {
                        fileName = fileName.substring(0, MAX_FILE_NAME_LENGHT);
                    }
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return fileName;
    }

    InputStream getVideoInputStream(String videoUrl) {
        Logging.out(LOG_TAG, "Download video", Logging.LogLevel.DEBUG);

        InputStream inputStream = null;
        try {
            URL url = new URL(videoUrl);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setReadTimeout(TIMEOUT);
            urlConnection.setConnectTimeout(TIMEOUT);

            inputStream = new BufferedInputStream(urlConnection.getInputStream());

        } catch (MalformedURLException e) {
            Logging.out(LOG_TAG, "Error: " + e.getMessage(), Logging.LogLevel.ERROR);

        } catch (IOException e) {
            Logging.out(LOG_TAG, "Error: " + e.getMessage(), Logging.LogLevel.ERROR);
        }
        return inputStream;
    }

    String writeStreamToFile(InputStream stream, File file) {
        Logging.out(LOG_TAG, "Write to file", Logging.LogLevel.DEBUG);

        String filePath = null;
        try {
            FileOutputStream out = new FileOutputStream(file);
            byte buffer[] = new byte[32 * 1024];
            int length = 0;
            while ((length = stream.read(buffer)) != -1) {
                out.write(buffer, 0, length);
            }
            out.close();
            filePath = file.getAbsolutePath();

            Logging.out(LOG_TAG, "Video file cached", Logging.LogLevel.DEBUG);

        } catch (IOException e) {
            Logging.out(LOG_TAG, e.getMessage(), Logging.LogLevel.ERROR);
            e.printStackTrace();
            file.delete();
        }
        return filePath;
    }
}

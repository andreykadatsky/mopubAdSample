package com.loopme.tasks;

import android.content.Context;
import android.text.TextUtils;

import android.util.Log;
import com.loopme.AdRequestParametersProvider;
import com.loopme.ConnectionType;
import com.loopme.ExecutorHelper;
import com.loopme.Logging;
import com.loopme.Logging.LogLevel;
import com.loopme.StaticParams;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.Future;

public class VideoTask {

    private static final String LOG_TAG = VideoTask.class.getSimpleName();

    private static final String MP4_FORMAT = ".mp4";
    private static final String VIDEO_FOLDER = "LoopMeAds";

    private String mVideoUrl;
    private File mVideoFile;
    private String mVideoFileName;

    private Context mContext;

    private Listener mListener;
    private VideoHelper mHelper;

    private Future mFuture;
    private Runnable mRunnable;

    public interface Listener {
        void onComplete(String path, boolean isCached);
    }

    public interface DownloadFileListener {
        void onDownloaded();
    }

    public VideoTask(String videoUrl, Context context, Listener listener) {
        mVideoUrl = videoUrl;
        mContext = context;
        mListener = listener;
        mHelper = new VideoHelper();

        initRunnable();
    }

    private void initRunnable() {
        mRunnable = new Runnable() {
            @Override
            public void run() {
                deleteInvalidVideoFiles();

                mVideoFileName = mHelper.detectFileName(mVideoUrl);
                if (TextUtils.isEmpty(mVideoFileName)) {
                    complete(null, false);
                    return;
                }
                File f = checkFileNotExists(mVideoFileName);
                if (f != null) {
                    Logging.out(LOG_TAG, "Video file already exists", LogLevel.DEBUG);
                    complete(f.getAbsolutePath(), true);
                    return;

                } else {
                    if (!StaticParams.USE_PART_PRELOAD) {
                        downloadVideo(new DownloadFileListener() {
                            @Override
                            public void onDownloaded() {
                                if (mVideoFile.exists()) {
                                    complete(mVideoFile.getAbsolutePath(), true);
                                } else {
                                    complete(null, false);
                                }
                            }
                        });
                    } else {
                        complete(mVideoUrl, false);
                    }
                }
            }
        };
    }

    public void start() {
        mFuture = ExecutorHelper.getExecutor().submit(mRunnable);
    }

    public void stop(boolean interruptFile) {
        if (mFuture != null) {
            boolean b = mFuture.cancel(true);
            if (b || interruptFile) {
                deleteCorruptedVideoFile();
            }
            mFuture = null;
        }
    }

    private void deleteCorruptedVideoFile() {
        if (mVideoFileName != null && mVideoFile != null) {
            Logging.out(LOG_TAG, "Delete corrupted video file", LogLevel.DEBUG);
            mVideoFile.delete();
        }
    }

    private void deleteInvalidVideoFiles() {
        File parentDir = getParentDir();

        ArrayList<File> inFiles = new ArrayList<File>();
        File[] files = parentDir.listFiles();
        for (File file : files) {
            if (!file.isDirectory()) {
                if (file.getName().endsWith(MP4_FORMAT)) {
                    inFiles.add(file);

                    File f = new File(file.getAbsolutePath());
                    long creationTime = f.lastModified();
                    long currentTime = System.currentTimeMillis();

                    if ((creationTime + StaticParams.CACHED_VIDEO_LIFE_TIME < currentTime) ||
                            (f.length() == 0)) {
                        f.delete();
                        Logging.out(LOG_TAG, "Deleted cached file: " + file.getAbsolutePath(), LogLevel.DEBUG);
                    }
                }
            }
        }
    }

    private File getParentDir() {
        return mContext.getDir(VIDEO_FOLDER, Context.MODE_WORLD_READABLE);
    }

    private File checkFileNotExists(String filename) {
        File parentDir = getParentDir();
        Logging.out(LOG_TAG, "Cache dir: " + parentDir.getAbsolutePath(), LogLevel.DEBUG);

        File[] files = parentDir.listFiles();
        for (File file : files) {
            if (!file.isDirectory() && file.getName().startsWith(filename)) {
                return file;
            }
        }
        return null;
    }

    public void downloadVideo(final DownloadFileListener listener) {
        ExecutorHelper.getExecutor().submit(new Runnable() {
            @Override
            public void run() {
                int connectiontype = AdRequestParametersProvider.getInstance().getConnectionType(mContext);
                if (connectiontype == ConnectionType.WIFI) {
                    downloadVideoToNewFile();
                } else {
                    if (StaticParams.USE_MOBILE_NETWORK_FOR_CACHING) {
                        downloadVideoToNewFile();
                    } else {
                        Logging.out(LOG_TAG, "Mobile network. Video will not be cached", LogLevel.DEBUG);
                    }
                }
                if (listener != null) {
                    listener.onDownloaded();
                }
            }
        });
    }

    private void downloadVideoToNewFile() {
        createNewFile(mVideoFileName);

        if (mVideoFile != null) {
            InputStream stream = mHelper.getVideoInputStream(mVideoUrl);
            if (stream != null && mVideoFile.exists()) {
                mHelper.writeStreamToFile(stream, mVideoFile);//fixme can be timeout 3 minutes
            } else {
                mVideoFile.delete();
            }
        }
    }

    private void createNewFile(String fileName) {
        try {
            File dir = getParentDir();
            mVideoFile = File.createTempFile(fileName, MP4_FORMAT, dir);

        } catch (IOException e) {
            Logging.out(LOG_TAG, e.getMessage(), LogLevel.DEBUG);
        }
    }

    private void complete(String filePath, boolean isCached) {
        if (mListener != null) {
            mListener.onComplete(filePath, isCached);
        }
    }
}
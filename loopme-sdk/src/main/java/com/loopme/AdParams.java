package com.loopme;

import com.loopme.Logging.LogLevel;

import java.util.ArrayList;
import java.util.List;

/**
 * Ad parameters. Builds from server response.
 */
public class AdParams {

    private static final String LOG_TAG = AdParams.class.getSimpleName();

    private final String mHtml;
    private final String mFormat;
    private final String mOrientation;
    private final int mExpiredDate;

    private List<String> mPackageIds = new ArrayList<String>();
    private String mToken;

    private AdParams(AdParamsBuilder builder) {
        mFormat = builder.mBuilderFormat;
        mHtml = builder.mBuilderHtml;
        mOrientation = builder.mBuilderOrientation;
        mExpiredDate = builder.mBuilderExpiredDate;

        mPackageIds = builder.mPackageIds;
        mToken = builder.mToken;

        Logging.out(LOG_TAG, "Server response indicates  ad params: "
                + "format: " + mFormat + ", orientation: " + mOrientation
                + ", expire in: " + mExpiredDate, LogLevel.DEBUG);
    }

    public String getHtml() {
        return mHtml;
    }

    public String getAdFormat() {
        return mFormat;
    }

    public String getAdOrientation() {
        return mOrientation;
    }

    public int getExpiredTime() {
        return mExpiredDate;
    }

    public List<String> getPackageIds() {
        return mPackageIds;
    }

    public String getToken() {
        return mToken;
    }

    static class AdParamsBuilder {

        private final String mBuilderFormat;

        private String mBuilderHtml;
        private String mBuilderOrientation;
        private int mBuilderExpiredDate;

        private List<String> mPackageIds = new ArrayList<String>();
        private String mToken;

        public AdParamsBuilder(String format) {
            mBuilderFormat = format;
        }

        public AdParamsBuilder packageIds(List<String> installPacakage) {
            mPackageIds = installPacakage;
            return this;
        }

        public AdParamsBuilder token(String token) {
            mToken = token;
            return this;
        }

        public AdParamsBuilder html(String html) {
            mBuilderHtml = html;
            return this;
        }

        public AdParamsBuilder orientation(String orientation) {
            if (isValidOrientationValue(orientation)) {
                mBuilderOrientation = orientation;
            }
            return this;
        }

        public AdParamsBuilder expiredTime(int time) {
            int timeSec = time * 1000;
            mBuilderExpiredDate = Math.max(StaticParams.DEFAULT_EXPIRED_TIME,
                    timeSec);
            return this;
        }

        public AdParams build() {
            if (isValidFormatValue()) {
                return new AdParams(this);
            } else {
                Logging.out(LOG_TAG, "Wrong ad format value", LogLevel.ERROR);
                return null;
            }
        }

        private boolean isValidFormatValue() {
            if (mBuilderFormat == null) {
                return false;
            }

            return mBuilderFormat.equalsIgnoreCase(StaticParams.BANNER_TAG)
                    || mBuilderFormat.equalsIgnoreCase(StaticParams.INTERSTITIAL_TAG);
        }

        private boolean isValidOrientationValue(String or) {
            if (or == null) {
                return false;
            }
            return or.equalsIgnoreCase(StaticParams.ORIENTATION_PORT)
                    || or.equalsIgnoreCase(StaticParams.ORIENTATION_LAND);
        }
    }
}

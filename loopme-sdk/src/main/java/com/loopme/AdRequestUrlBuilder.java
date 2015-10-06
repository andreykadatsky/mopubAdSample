package com.loopme;

import android.content.Context;
import android.net.Uri;

import com.loopme.Logging.LogLevel;

import java.util.Arrays;
import java.util.List;

/**
 * Builds request url.
 */
public class AdRequestUrlBuilder {

    private static final String LOG_TAG = AdRequestUrlBuilder.class.getSimpleName();

    private static final String PARAM_APPKEY = "ak";
    private static final String PARAM_CONNECTION_TYPE = "ct";
    private static final String PARAM_LANGUAGE = "lng";
    private static final String PARAM_SDK_VERSION = "sv";
    private static final String PARAM_APP_VERSION = "av";
    private static final String PARAM_MRAID = "mr";
    private static final String PARAM_ORIENTATION = "or";
    private static final String PARAM_VIEWER_TOKEN = "vt";
    private static final String PARAM_DNT = "dnt";
    private static final String PARAM_LATITUDE = "lat";
    private static final String PARAM_LONGITUDE = "lon";
    private static final String PARAM_CARRIER = "carrier";

    /**
     * Optional targeting parameters
     */
    private static final String PARAM_KEYWORDS = "keywords";
    private static final String PARAM_YEAR_OF_BIRTH = "yob";
    private static final String PARAM_GENDER = "gender";

    private final Context mContext;

    public AdRequestUrlBuilder(Context context) {
        mContext = context;
        if (context == null) {
            Logging.out(LOG_TAG, "Context should not be null. Can't build request url", LogLevel.ERROR);
        }
    }

    /**
     * Build and return request url
     */
    public String buildRequestUrl(String appKey, AdTargetingData metadata) {
        if (mContext == null) {
            return null;
        }
        AdRequestParametersProvider provider = AdRequestParametersProvider.getInstance();

        String str = StaticParams.BASE_URL;
        List<String> list = Arrays.asList(str.split("/"));

        Uri.Builder builder = new Uri.Builder();
        builder.scheme("https");

        for (String s : list) {
            if (list.indexOf(s) == 0) {
                builder.authority(s);
            } else {
                builder.appendPath(s);
            }
        }

        builder.appendQueryParameter(PARAM_APPKEY, appKey)
                .appendQueryParameter(PARAM_CONNECTION_TYPE, String.valueOf(provider.getConnectionType(mContext)))
                .appendQueryParameter(PARAM_LANGUAGE, provider.getLanguage())
                .appendQueryParameter(PARAM_SDK_VERSION, StaticParams.SDK_VERSION)
                .appendQueryParameter(PARAM_APP_VERSION, provider.getAppVersion(mContext))
                .appendQueryParameter(PARAM_MRAID, provider.getMraidSupport())
                .appendQueryParameter(PARAM_ORIENTATION, provider.getOrientation(mContext))
                .appendQueryParameter(PARAM_VIEWER_TOKEN, provider.getViewerToken());

        String latitude = provider.getLatitude();
        if (latitude != null) {
            builder.appendQueryParameter(PARAM_LATITUDE, latitude);
        }

        String longitude = provider.getLongitude();
        if (longitude != null) {
            builder.appendQueryParameter(PARAM_LONGITUDE, longitude);
        }

        String carrier = provider.getCarrier(mContext);
        if (carrier != null) {
            builder.appendQueryParameter(PARAM_CARRIER, carrier);
        }

        if (provider.isDntPresent()) {
            builder.appendQueryParameter(PARAM_DNT, "1");
        }

        if (metadata != null && metadata.getKeywords() != null) {
            builder.appendQueryParameter(PARAM_KEYWORDS, metadata.getKeywords());
        }

        if (metadata != null && metadata.getGender() != null) {
            builder.appendQueryParameter(PARAM_GENDER, metadata.getGender());
        }

        if (metadata != null && metadata.getYob() != 0) {
            builder.appendQueryParameter(PARAM_YEAR_OF_BIRTH, String.valueOf(metadata.getYob()));
        }

        if (!metadata.getCustomParameters().isEmpty()) {
            for (CustomRequestParameter crp : metadata.getCustomParameters()) {
                builder.appendQueryParameter(crp.getParamName(), crp.getParamValue());
            }
        }

        String url = builder.build().toString();
        return url;
    }
}
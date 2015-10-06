package com.loopme;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.loopme.Logging.LogLevel;

import java.util.Locale;

public class AdRequestParametersProvider {

    private static final String LOG_TAG = AdRequestParametersProvider.class.getSimpleName();

    private static AdRequestParametersProvider sProvider;

    private volatile String mAdvertisingId;
    private String mLoopMeId;

    private String mAppVersion;
    private String mMraid;
    private boolean mDntPresent;
    private String mCarrier;
    private boolean mCarrierInited;

    private AdRequestParametersProvider() {
    }

    public static AdRequestParametersProvider getInstance() {
        if (sProvider == null) {
            sProvider = new AdRequestParametersProvider();
        }
        return sProvider;
    }

    void reset() {
        sProvider = null;
    }

    void setGoogleAdvertisingId(String advId) {
        Logging.out(LOG_TAG, "Advertising Id = " + advId, LogLevel.DEBUG);
        mAdvertisingId = advId;
    }

    String getGoogleAdvertisingId() {
        return mAdvertisingId;
    }

    public int getConnectionType(Context context) {
        if (context == null) {
            return ConnectionType.UNKNOWN;
        }

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return ConnectionType.UNKNOWN;
        }

        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni == null) {
            return ConnectionType.UNKNOWN;
        }

        int type = ni.getType();

        if (type == ConnectivityManager.TYPE_WIFI) {
            return ConnectionType.WIFI;
        } else if (type == ConnectivityManager.TYPE_ETHERNET) {
            return ConnectionType.ETHERNET;
        } else if (type == ConnectivityManager.TYPE_MOBILE) {

            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager == null) {
                return ConnectionType.UNKNOWN;
            }

            int networkType = telephonyManager.getNetworkType();
            switch (networkType) {
                case TelephonyManager.NETWORK_TYPE_GPRS:
                case TelephonyManager.NETWORK_TYPE_EDGE:
                case TelephonyManager.NETWORK_TYPE_CDMA:
                case TelephonyManager.NETWORK_TYPE_1xRTT:
                case TelephonyManager.NETWORK_TYPE_IDEN:
                    return ConnectionType.MOBILE_2G;

                case TelephonyManager.NETWORK_TYPE_UMTS:
                case TelephonyManager.NETWORK_TYPE_EVDO_0:
                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                case TelephonyManager.NETWORK_TYPE_HSPA:
                case TelephonyManager.NETWORK_TYPE_EVDO_B:
                case TelephonyManager.NETWORK_TYPE_EHRPD:
                case TelephonyManager.NETWORK_TYPE_HSPAP:
                    return ConnectionType.MOBILE_3G;

                case TelephonyManager.NETWORK_TYPE_LTE:
                    return ConnectionType.MOBILE_4G;

                default:
                    return ConnectionType.MOBILE_UNKNOWN_GENERATION;
            }
        } else {
            return ConnectionType.UNKNOWN;
        }
    }

    public String getLanguage() {
        return Locale.getDefault().getLanguage();
    }

    public String getAppVersion(Context context) {
        if (mAppVersion == null) {
            initAppVersion(context);
        }
        return mAppVersion;
    }

    private void initAppVersion(Context context) {
        if (context == null) {
            mAppVersion = "0.0";
            return;
        }
        try {
            mAppVersion = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (NameNotFoundException e) {
            Logging.out(LOG_TAG, "Can't get app version. Exception: " + e.getMessage(),
                    LogLevel.ERROR);
            mAppVersion = "0.0";
        }
    }

    public String getMraidSupport() {
        if (mMraid == null) {
            initMraidSupport();
        }
        return mMraid;
    }

    private void initMraidSupport() {
        mMraid = "1";
        try {
            Class.forName("com.loopme.MraidView");
        } catch (ClassNotFoundException e) {
            mMraid = "0";
        }
    }

    public String getOrientation(Context context) {
        if (context == null) {
            return "";
        }
        int orientation = context.getResources().getConfiguration().orientation;
        if (Configuration.ORIENTATION_LANDSCAPE == orientation) {
            return "l";
        } else {
            return "p";
        }
    }

    public String getViewerToken() {
        String advId = mAdvertisingId;
        if (TextUtils.isEmpty(advId)) {
            mDntPresent = true;
            if (mLoopMeId == null) {
                String loopmeId = Long.toHexString(Double.doubleToLongBits(Math.random()));
                Logging.out(LOG_TAG, "LoopMe Id = " + loopmeId, LogLevel.DEBUG);
                mLoopMeId = loopmeId;
            }
            return mLoopMeId;
        } else {
            return advId;
        }
    }

    public String getLatitude() {
        Location location = Utils.getLastKnownLocation();
        if (location != null) {
            return String.valueOf(location.getLatitude());
        } else {
            return null;
        }
    }

    public String getLongitude() {
        Location location = Utils.getLastKnownLocation();
        if (location != null) {
            return String.valueOf(location.getLongitude());
        } else {
            return null;
        }
    }

    public String getCarrier(Context context) {
        if (!mCarrierInited) {
            initCarrier(context);
        }
        return mCarrier;
    }

    private void initCarrier(Context context) {
        if (context == null) {
            mCarrierInited = true;
            return;
        }
        TelephonyManager telephonyManager =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        if (telephonyManager == null) {
            mCarrierInited = true;
            return;
        }
        mCarrier = telephonyManager.getNetworkOperator();
        if (mCarrier.isEmpty()) {
            mCarrier = null;
        }
        mCarrierInited = true;
    }

    public boolean isDntPresent() {
        return mDntPresent;
    }
}

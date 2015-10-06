package com.loopme.tasks;

import com.loopme.AdFormat;
import com.loopme.AdParams;
import com.loopme.Logging;
import com.loopme.LoopMeError;
import com.loopme.StaticParams;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Logging.class})
public class AdFetcherTest {

    @Before
    public void setUp() throws Exception {
        AdFetcher.TIMEOUT = 20000;
    }

    @Test(timeout = 20000)
    public void validUrl_invalidFormat() {
        PowerMockito.mockStatic(Logging.class);

        String requestUrl = "http://loopme.me/api/loopme/ads/v3?ak=test_mpu&ct=2&vt=2f15b41a-30b1-43e2-822f-18117e6df8b8";
        final int adFormat = AdFormat.INTERSTITIAL;
        AdFetcher.Listener listener = new AdFetcher.Listener() {
            @Override
            public void onComplete(AdParams params, LoopMeError error) {
                Assert.assertNull(params);
                Assert.assertNotNull(error);
                assertEquals(error.getMessage(), "Wrong Ad format");
            }
        };
        AdFetcher fetcher = new AdFetcher(requestUrl, listener, adFormat);
        fetcher.run();
    }

    @Test(timeout = 20000)
    public void invalidUrlFile() {
        PowerMockito.mockStatic(Logging.class);

        String requestUrl = "http://loopme.me/api/loopme/ads1/v3?ak=test_mpu&ct=2&vt=2f15b41a-30b1-43e2-822f-18117e6df8b8";
        final int adFormat = AdFormat.BANNER;
        AdFetcher.Listener listener = new AdFetcher.Listener() {
            @Override
            public void onComplete(AdParams params, LoopMeError error) {
                Assert.assertNull(params);
                Assert.assertNotNull(error);
                assertEquals(error.getMessage(), "Page not found");
            }
        };
        AdFetcher fetcher = new AdFetcher(requestUrl, listener, adFormat);
        fetcher.run();
    }

    @Test(timeout = 20000)
    public void invalidUrlHost() {
        PowerMockito.mockStatic(Logging.class);

        String requestUrl = "http://loopme123.me/api/loopme/ads/v3?ak=test_mpu&ct=2&vt=2f15b41a-30b1-43e2-822f-18117e6df8b8";
        final int adFormat = AdFormat.BANNER;
        AdFetcher.Listener listener = new AdFetcher.Listener() {
            @Override
            public void onComplete(AdParams params, LoopMeError error) {
                Assert.assertNull(params);
                Assert.assertNotNull(error);
                assertEquals(error.getMessage(), "Error during establish connection");
            }
        };
        AdFetcher fetcher = new AdFetcher(requestUrl, listener, adFormat);
        fetcher.run();
    }

    @Test(timeout = 20000)
    public void httpsUrl() {
        PowerMockito.mockStatic(Logging.class);

        String requestUrl = "https://loopme.me/api/loopme/ads/v3?ak=test_mpu&ct=2&vt=2f15b41a-30b1-43e2-822f-18117e6df8b8";
        final int adFormat = AdFormat.BANNER;
        AdFetcher.Listener listener = new AdFetcher.Listener() {
            @Override
            public void onComplete(AdParams params, LoopMeError error) {
                Assert.assertNotNull(params);
                Assert.assertNull(error);
                assertEquals(params.getAdFormat(), StaticParams.BANNER_TAG);
            }
        };
        AdFetcher fetcher = new AdFetcher(requestUrl, listener, adFormat);
        fetcher.run();
    }

    @Test(timeout = 20000)
    public void validUrl_validFormat() {
        PowerMockito.mockStatic(Logging.class);

        String requestUrl = "http://loopme.me/api/loopme/ads/v3?ak=test_mpu&ct=2&vt=2f15b41a-30b1-43e2-822f-18117e6df8b8";
        final int adFormat = AdFormat.BANNER;
        AdFetcher.Listener listener = new AdFetcher.Listener() {
            @Override
            public void onComplete(AdParams params, LoopMeError error) {
                Assert.assertNotNull(params);
                Assert.assertNull(error);
                assertEquals(params.getAdFormat(), StaticParams.BANNER_TAG);
            }
        };
        AdFetcher fetcher = new AdFetcher(requestUrl, listener, adFormat);
        fetcher.run();
    }

    @Test(timeout = 20000)
    public void invalidAppKey() {
        PowerMockito.mockStatic(Logging.class);

        String requestUrl = "http://loopme.me/api/loopme/ads/v3?ak=test_mpu123&ct=2&vt=2f15b41a-30b1-43e2-822f-18117e6df8b8";
        final int adFormat = AdFormat.BANNER;
        AdFetcher.Listener listener = new AdFetcher.Listener() {
            @Override
            public void onComplete(AdParams params, LoopMeError error) {
                Assert.assertNull(params);
                Assert.assertNotNull(error);
                assertEquals(error.getMessage(), "Missing or invalid app key");
            }
        };
        AdFetcher fetcher = new AdFetcher(requestUrl, listener, adFormat);
        fetcher.run();
    }

//    @Test(timeout = 20000)
//    public void requestTimeout() {
//        PowerMockito.mockStatic(Logging.class);
//
//        String requestUrl = "http://loopme.me/api/loopme/ads/v3?ak=test_mpu&ct=2&vt=2f15b41a-30b1-43e2-822f-18117e6df8b8";
//        final int adFormat = AdFormat.BANNER;
//        AdFetcher.Listener listener = new AdFetcher.Listener() {
//            @Override
//            public void onComplete(AdParams params, LoopMeError error) {
//                Assert.assertNull(params);
//                Assert.assertNotNull(error);
//                assertEquals(error.getMessage(), "Request timeout");
//            }
//        };
//        AdFetcher fetcher = new AdFetcher(requestUrl, listener, adFormat);
//        AdFetcher.TIMEOUT = 100;
//        fetcher.run();
//    }

    @Test(timeout = 20000)
    public void emptyRequestUrl() {
        PowerMockito.mockStatic(Logging.class);

        final int adFormat = AdFormat.BANNER;
        AdFetcher.Listener listener = new AdFetcher.Listener() {
            @Override
            public void onComplete(AdParams params, LoopMeError error) {
                Assert.assertNull(params);
                Assert.assertNotNull(error);
                assertEquals(error.getMessage(), "Error during establish connection");
            }
        };
        AdFetcher fetcher = new AdFetcher(null, listener, adFormat);
        fetcher.run();
    }
}
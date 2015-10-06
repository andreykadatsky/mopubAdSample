package com.loopme;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Logging.class, ResponseParser.Listener.class})
public class ResponseParserTest {

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(Logging.class);
        PowerMockito.mockStatic(ResponseParser.Listener.class);
    }

    @Test
    public void testResponseParserCreation() {
        ResponseParser parser1 = new ResponseParser(null, 0);
        assertNotNull(parser1);

        ResponseParser parser2 = new ResponseParser(null, AdFormat.BANNER);
        assertNotNull(parser2);

        ResponseParser parser3 = new ResponseParser(null, AdFormat.INTERSTITIAL);
        assertNotNull(parser3);
    }

    @Test
    public void testGetAdParams1() {
        String responseString = "{\"script\": \"html\", \"settings\": {\"format\": \"interstitial\",\"orientation\": \"portrait\",\"ad_refresh_time\": 30,\"ad_expiry_time\": 3600 }}";

        ResponseParser parser = new ResponseParser(null, 0);
        AdParams params = parser.getAdParams(responseString);

        assertNull(params);
    }

    @Test
    public void testGetAdParams() {
        String responseString = "{\"script\": \"html\", \"settings\": {\"format\": \"interstitial\",\"orientation\": \"portrait\",\"ad_expiry_time\": 3600 }}";

        ResponseParser parser = new ResponseParser(null, AdFormat.INTERSTITIAL);
        AdParams params = parser.getAdParams(responseString);

        assertNotNull(params);
        assertEquals(params.getAdFormat(), "interstitial");
        assertEquals(params.getHtml(), "html");
        assertEquals(params.getAdOrientation(), "portrait");
        assertEquals(params.getExpiredTime(), 3600 * 1000);
    }

    @Test
    public void testGetAdParamsWhenWrongAdExpireTime() {
        //case 1: expire time = 0
        String responseString = "{\"script\": \"html\", \"settings\": {\"format\": \"interstitial\",\"orientation\": \"portrait\",\"ad_refresh_time\": 30,\"ad_expiry_time\": 0}}";

        ResponseParser parser = new ResponseParser(null, AdFormat.INTERSTITIAL);
        AdParams params = parser.getAdParams(responseString);

        assertNotNull(params);
        assertEquals(params.getAdFormat(), "interstitial");
        assertEquals(params.getHtml(), "html");
        assertEquals(params.getAdOrientation(), "portrait");
        assertEquals(params.getExpiredTime(), StaticParams.DEFAULT_EXPIRED_TIME);

        //case 2: expire time is empty (JSONException)
        String responseString2 = "{\"script\": \"html\", \"settings\": {\"format\": \"interstitial\",\"orientation\": \"portrait\",\"ad_refresh_time\": 30,\"ad_expiry_time\": }}";

        ResponseParser parser2 = new ResponseParser(null, AdFormat.INTERSTITIAL);
        AdParams params2 = parser2.getAdParams(responseString2);

        assertNull(params2);

        //case 3: expire time absent in response
        String responseString3 = "{\"script\": \"html\", \"settings\": {\"format\": \"interstitial\",\"orientation\": \"portrait\",\"ad_refresh_time\": 30}}";

        ResponseParser parser3 = new ResponseParser(null, AdFormat.INTERSTITIAL);
        AdParams params3 = parser3.getAdParams(responseString3);

        assertNotNull(params3);
        assertEquals(params3.getAdFormat(), "interstitial");
        assertEquals(params3.getHtml(), "html");
        assertEquals(params3.getAdOrientation(), "portrait");
        assertEquals(params3.getExpiredTime(), StaticParams.DEFAULT_EXPIRED_TIME);

        //case 4: wrong format
        String responseString4 = "{\"script\": \"html\", \"settings\": {\"format\": \"interstitial\",\"orientation\": \"portrait\",\"ad_refresh_time\": 30,\"ad_expiry_time\": \"asd\"}}";

        ResponseParser parser4 = new ResponseParser(null, AdFormat.INTERSTITIAL);
        AdParams params4 = parser4.getAdParams(responseString4);

        assertNotNull(params4);
        assertEquals(params4.getAdFormat(), "interstitial");
        assertEquals(params4.getHtml(), "html");
        assertEquals(params4.getAdOrientation(), "portrait");
        assertEquals(params4.getExpiredTime(), StaticParams.DEFAULT_EXPIRED_TIME);
    }

    @Test
    public void testGetAdParamsWhenWrongOrientation() {
        //case 1: orientation = ""
        String responseString = "{\"script\": \"html\", \"settings\": {\"format\": \"interstitial\",\"orientation\": \"\",\"ad_refresh_time\": 30,\"ad_expiry_time\": 3600}}";

        ResponseParser parser = new ResponseParser(null, AdFormat.INTERSTITIAL);
        AdParams params = parser.getAdParams(responseString);

        assertNotNull(params);
        assertEquals(params.getAdFormat(), "interstitial");
        assertEquals(params.getHtml(), "html");
        assertEquals(params.getAdOrientation(), null);
        assertEquals(params.getExpiredTime(), 3600 * 1000);

        //case 2: orientation is empty (JSONException)
        String responseString2 = "{\"script\": \"html\", \"settings\": {\"format\": \"interstitial\",\"orientation\": ,\"ad_refresh_time\": 30,\"ad_expiry_time\": 3600}}";

        ResponseParser parser2 = new ResponseParser(null, AdFormat.INTERSTITIAL);
        AdParams params2 = parser2.getAdParams(responseString2);

        assertNull(params2);

        //case 3: orientation absent in response
        String responseString3 = "{\"script\": \"html\", \"settings\": {\"format\": \"interstitial\",\"ad_expiry_time\": 3600}}";

        ResponseParser parser3 = new ResponseParser(null, AdFormat.INTERSTITIAL);
        AdParams params3 = parser3.getAdParams(responseString3);

        assertNotNull(params3);
        assertEquals(params3.getAdFormat(), "interstitial");
        assertEquals(params3.getHtml(), "html");
        assertEquals(params3.getAdOrientation(), null);
        assertEquals(params3.getExpiredTime(), 3600 * 1000);

        //case 4: wrong format
        String responseString4 = "{\"script\": \"html\", \"settings\": {\"format\": \"interstitial\",\"orientation\": 10,\"ad_refresh_time\": 30,\"ad_expiry_time\": 3600}}";

        ResponseParser parser4 = new ResponseParser(null, AdFormat.INTERSTITIAL);
        AdParams params4 = parser4.getAdParams(responseString4);

        assertNotNull(params4);
        assertEquals(params4.getAdFormat(), "interstitial");
        assertEquals(params4.getHtml(), "html");
        assertEquals(params4.getAdOrientation(), null);
        assertEquals(params4.getExpiredTime(), 3600 * 1000);
    }

    @Test
    public void testGetAdParamsWhenWrongFormat() {
        //case 1: format = ""
        String responseString = "{\"script\": \"html\", \"settings\": {\"format\": \"\",\"ad_refresh_time\": 30,\"ad_expiry_time\": 3600}}";

        ResponseParser parser = new ResponseParser(null, AdFormat.INTERSTITIAL);
        AdParams params = parser.getAdParams(responseString);

        assertNull(params);

        //case 2: format is empty (JSONException)
        String responseString2 = "{\"script\": \"html\", \"settings\": {\"format\": ,\"ad_refresh_time\": 30,\"ad_expiry_time\": 3600}}";

        ResponseParser parser2 = new ResponseParser(null, AdFormat.INTERSTITIAL);
        AdParams params2 = parser2.getAdParams(responseString2);

        assertNull(params2);

        //case 3: format absent in response
        String responseString3 = "{\"script\": \"html\", \"settings\": {\"ad_expiry_time\": 3600}}";

        ResponseParser parser3 = new ResponseParser(null, AdFormat.INTERSTITIAL);
        AdParams params3 = parser3.getAdParams(responseString3);

        assertNull(params3);

        //case 4: wrong format
        String responseString4 = "{\"script\": \"html\", \"settings\": {\"format\": 10,\"ad_refresh_time\": 30,\"ad_expiry_time\": 3600}}";

        ResponseParser parser4 = new ResponseParser(null, AdFormat.INTERSTITIAL);
        AdParams params4 = parser4.getAdParams(responseString4);

        assertNull(params4);
    }

    @Test
    public void testGetAdParamsWhenWrongScript() {
        //case 1: script = ""
        String responseString = "{\"script\": \"\", \"settings\": {\"format\": \"interstitial\"}}";

        ResponseParser parser = new ResponseParser(null, AdFormat.INTERSTITIAL);
        AdParams params = parser.getAdParams(responseString);

        assertNotNull(params);
        assertEquals(params.getHtml(), "");

        //case 2: script is empty (JSONException)
        String responseString2 = "{\"script\": , \"settings\": {\"format\": \"interstitial\"}}";

        ResponseParser parser2 = new ResponseParser(null, AdFormat.INTERSTITIAL);
        AdParams params2 = parser2.getAdParams(responseString2);

        assertNull(params2);

        //case 3: script absent in response
        String responseString3 = "{\"settings\": {\"format\": \"interstitial\", \"ad_expiry_time\": 3600}}";

        ResponseParser parser3 = new ResponseParser(null, AdFormat.INTERSTITIAL);
        AdParams params3 = parser3.getAdParams(responseString3);

        assertNotNull(params3);
        assertEquals(params3.getHtml(), null);

        //case 4: wrong format
        String responseString4 = "{\"script\": 10, \"settings\": {\"format\": \"interstitial\"}}";

        ResponseParser parser4 = new ResponseParser(null, AdFormat.INTERSTITIAL);
        AdParams params4 = parser4.getAdParams(responseString4);

        assertNotNull(params4);
        assertNull(params4.getHtml());
    }

    @Test
    public void testGetAdParamsWhenWrongSettings() {
        //case 1: settings = {}
        String responseString = "{\"script\": \"html\", \"settings\": {}}";

        ResponseParser parser = new ResponseParser(null, AdFormat.INTERSTITIAL);
        AdParams params = parser.getAdParams(responseString);

        assertNull(params);

        //case 2: settings is empty (JSONException)
        String responseString2 = "{\"script\": \"html\", \"settings\": }";

        ResponseParser parser2 = new ResponseParser(null, AdFormat.INTERSTITIAL);
        AdParams params2 = parser2.getAdParams(responseString2);

        assertNull(params2);

        //case 3: settings absent in response
        String responseString3 = "{\"script\": \"html\"}";

        ResponseParser parser3 = new ResponseParser(null, AdFormat.INTERSTITIAL);
        AdParams params3 = parser3.getAdParams(responseString3);

        assertNull(params3);

        //case 4: wrong format
        String responseString4 = "{\"script\": \"html\", \"settings\": \"asd\"}";

        ResponseParser parser4 = new ResponseParser(null, AdFormat.INTERSTITIAL);
        AdParams params4 = parser4.getAdParams(responseString4);

        assertNull(params4);
    }
}

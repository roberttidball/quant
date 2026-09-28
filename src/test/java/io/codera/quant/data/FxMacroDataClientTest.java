package io.codera.quant.data;

import org.junit.Test;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class FxMacroDataClientTest {
    @Test
    public void buildsForexUriWithQueryAndNoApiKey() throws Exception {
        FxMacroDataClient client = new FxMacroDataClient(
                "test-key",
                "https://example.com/v1/",
                null,
                null);

        Map<String, String> query = new HashMap<>();
        query.put("limit", "1");
        URI uri = client.buildUri("forex/eur/usd", query);

        assertEquals("https://example.com/v1/forex/eur/usd?limit=1", uri.toString());
    }

    @Test
    public void buildsPagedUriWithLimitAndOffset() throws Exception {
        FxMacroDataClient client = new FxMacroDataClient(
                "test-key",
                "https://example.com/v1/",
                null,
                null);

        URI uri = client.buildUri("forex/eur/usd", FxMacroDataClient.page(100, 200));

        assertEquals("https://example.com/v1/forex/eur/usd?limit=100&offset=200", uri.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsLimitAboveApiMaximum() {
        FxMacroDataClient.page(101, 0);
    }

    @Test
    public void buildsFullEndpointSurfaceUnderV1() throws Exception {
        FxMacroDataClient client = new FxMacroDataClient(
                "test-key",
                "https://example.com/v1/",
                null,
                null);

        String[] paths = new String[] {
                "data_catalogue/usd",
                "announcements/usd/non_farm_payrolls",
                "announcements/usd/latest",
                "announcements/changes",
                "calendar/usd",
                "predictions/usd/non_farm_payrolls",
                "forex/eur/usd",
                "cot/usd",
                "commodities/brent",
                "commodities/latest",
                "curves/usd",
                "curve_proxies/usd",
                "forward_curves/usd",
                "rate_differentials/eur/usd",
                "forward_differentials/eur/usd",
                "market_sessions",
                "risk_sentiment",
                "news/usd",
                "press-releases/usd",
                "graphql"
        };

        for (String path : paths) {
            URI uri = client.buildUri(path, Collections.emptyMap());
            assertTrue(uri.toString().startsWith("https://example.com/v1/"));
            assertNull(uri.getQuery());
        }
    }
}

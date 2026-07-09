package io.codera.quant.data;

import org.junit.Test;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FxMacroDataClientTest {
    @Test
    public void buildsForexUriWithApiKeyAndQuery() throws Exception {
        FxMacroDataClient client = new FxMacroDataClient(
                "test-key",
                "https://example.com/api/v1/",
                null,
                null);

        Map<String, String> query = new HashMap<>();
        query.put("limit", "1");
        URI uri = client.buildUri("forex/eur/usd", query);

        assertEquals("https://example.com/api/v1/forex/eur/usd?limit=1&api_key=test-key", uri.toString());
    }

    @Test
    public void buildsFullEndpointSurfaceUnderApiV1() throws Exception {
        FxMacroDataClient client = new FxMacroDataClient(
                "test-key",
                "https://example.com/api/v1/",
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
            assertTrue(uri.toString().startsWith("https://example.com/api/v1/"));
            assertTrue(uri.getQuery().contains("api_key=test-key"));
        }
    }
}

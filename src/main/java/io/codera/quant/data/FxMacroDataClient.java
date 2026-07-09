package io.codera.quant.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class FxMacroDataClient {
    private static final String DEFAULT_BASE_URL = "https://fxmacrodata.com/api/v1/";

    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;

    public FxMacroDataClient(String apiKey) {
        this(apiKey, DEFAULT_BASE_URL, HttpClients.createDefault(), new ObjectMapper());
    }

    public FxMacroDataClient(
            String apiKey,
            String baseUrl,
            CloseableHttpClient httpClient,
            ObjectMapper objectMapper) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("FXMacroData API key is required.");
        }
        this.apiKey = apiKey;
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.httpClient = httpClient == null ? HttpClients.createDefault() : httpClient;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    public static FxMacroDataClient fromEnvironment() {
        String apiKey = System.getenv("FXMACRODATA_API_KEY");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            apiKey = System.getenv("FXMD_API_KEY");
        }
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("Set FXMACRODATA_API_KEY or FXMD_API_KEY.");
        }
        return new FxMacroDataClient(apiKey);
    }

    public Observable<JsonNode> request(String path, Map<String, String> query) {
        return get(path, query);
    }

    public Observable<JsonNode> dataCatalogue(String currency) {
        return get("data_catalogue/" + currency, Collections.emptyMap());
    }

    public Observable<JsonNode> announcements(String currency, String indicator) {
        return get("announcements/" + currency + "/" + indicator, Collections.emptyMap());
    }

    public Observable<JsonNode> latestAnnouncements(String currency) {
        return get("announcements/" + currency + "/latest", Collections.emptyMap());
    }

    public Observable<JsonNode> announcementChanges() {
        return get("announcements/changes", Collections.emptyMap());
    }

    public Observable<JsonNode> calendar(String currency) {
        return get("calendar/" + currency, Collections.emptyMap());
    }

    public Observable<JsonNode> predictions(String currency, String indicator) {
        return get("predictions/" + currency + "/" + indicator, Collections.emptyMap());
    }

    public Observable<JsonNode> forex(String base, String quote) {
        return get("forex/" + base + "/" + quote, Collections.emptyMap());
    }

    public Observable<JsonNode> forex(String base, String quote, int limit) {
        Map<String, String> query = new HashMap<>();
        query.put("limit", String.valueOf(limit));
        return get("forex/" + base + "/" + quote, query);
    }

    public Observable<JsonNode> cot(String currency) {
        return get("cot/" + currency, Collections.emptyMap());
    }

    public Observable<JsonNode> commodity(String indicator) {
        return get("commodities/" + indicator, Collections.emptyMap());
    }

    public Observable<JsonNode> commoditiesLatest() {
        return get("commodities/latest", Collections.emptyMap());
    }

    public Observable<JsonNode> curves(String currency) {
        return get("curves/" + currency, Collections.emptyMap());
    }

    public Observable<JsonNode> curveProxies(String currency) {
        return get("curve_proxies/" + currency, Collections.emptyMap());
    }

    public Observable<JsonNode> forwardCurves(String currency) {
        return get("forward_curves/" + currency, Collections.emptyMap());
    }

    public Observable<JsonNode> rateDifferentials(String base, String quote) {
        return get("rate_differentials/" + base + "/" + quote, Collections.emptyMap());
    }

    public Observable<JsonNode> forwardDifferentials(String base, String quote) {
        return get("forward_differentials/" + base + "/" + quote, Collections.emptyMap());
    }

    public Observable<JsonNode> marketSessions() {
        return get("market_sessions", Collections.emptyMap());
    }

    public Observable<JsonNode> riskSentiment() {
        return get("risk_sentiment", Collections.emptyMap());
    }

    public Observable<JsonNode> news(String currency) {
        return get("news/" + currency, Collections.emptyMap());
    }

    public Observable<JsonNode> pressReleases(String currency) {
        return get("press-releases/" + currency, Collections.emptyMap());
    }

    public Observable<JsonNode> graphQl(String query, JsonNode variables) {
        return Observable.<JsonNode>create(subscriber -> {
            try {
                HttpPost post = new HttpPost(buildUri("graphql", Collections.emptyMap()));
                Map<String, Object> body = new HashMap<>();
                body.put("query", query);
                if (variables != null) {
                    body.put("variables", variables);
                }
                post.setEntity(new StringEntity(
                        objectMapper.writeValueAsString(body),
                        ContentType.APPLICATION_JSON));

                subscriber.onNext(parseResponse(httpClient.execute(post)));
                subscriber.onCompleted();
            } catch (Exception e) {
                subscriber.onError(e);
            }
        }).subscribeOn(Schedulers.io());
    }

    public URI buildUri(String path, Map<String, String> query) throws URISyntaxException {
        URIBuilder builder = new URIBuilder(baseUrl + path.replaceFirst("^/+", ""));
        if (query != null) {
            for (Map.Entry<String, String> entry : query.entrySet()) {
                if (entry.getValue() != null) {
                    builder.addParameter(entry.getKey(), entry.getValue());
                }
            }
        }
        builder.addParameter("api_key", apiKey);
        return builder.build();
    }

    private Observable<JsonNode> get(String path, Map<String, String> query) {
        return Observable.<JsonNode>create(subscriber -> {
            try {
                HttpGet get = new HttpGet(buildUri(path, query));
                subscriber.onNext(parseResponse(httpClient.execute(get)));
                subscriber.onCompleted();
            } catch (Exception e) {
                subscriber.onError(e);
            }
        }).subscribeOn(Schedulers.io());
    }

    private JsonNode parseResponse(HttpResponse response) throws IOException {
        HttpEntity entity = response.getEntity();
        String body = entity == null ? "" : EntityUtils.toString(entity);
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode < HttpStatus.SC_OK || statusCode >= HttpStatus.SC_MULTIPLE_CHOICES) {
            throw new IOException("FXMacroData HTTP " + statusCode + ": " + body);
        }
        return objectMapper.readTree(body);
    }

    private static String normalizeBaseUrl(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }
}

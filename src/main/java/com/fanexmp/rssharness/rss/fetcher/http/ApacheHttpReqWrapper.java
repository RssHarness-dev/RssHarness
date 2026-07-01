package com.fanexmp.rssharness.rss.fetcher.http;

import com.fanexmp.rssharness.rss.exception.HttpReqException;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ApacheHttpReqWrapper implements HttpReqWrapper {

    private static final Logger log = LoggerFactory.getLogger(ApacheHttpReqWrapper.class);

    private static final Timeout CONNECT_TIMEOUT = Timeout.ofSeconds(5);
    private static final Timeout RESPONSE_TIMEOUT = Timeout.ofSeconds(30);

    private final CloseableHttpClient httpClient;

    public ApacheHttpReqWrapper() {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(CONNECT_TIMEOUT)
                .setConnectionRequestTimeout(CONNECT_TIMEOUT)
                .setResponseTimeout(RESPONSE_TIMEOUT)
                .build();
        this.httpClient = HttpClients.custom()
                .setDefaultRequestConfig(config)
                .build();
    }

    @Override
    public InputStream get(String url) {
        HttpGet request = new HttpGet(url);
        request.setHeader("User-Agent", "RssFetcher/1.0");
        request.setHeader("Accept", "application/rss+xml, application/xml, text/xml");

        try {
            log.debug("Fetching URL: {}", url);
            CloseableHttpResponse response = httpClient.execute(request);
            int status = response.getCode();
            if (status != 200) {
                String reason = classifyError(status, response);
                log.warn("HTTP {} from {} — {}", status, url, reason);
                response.close();
                throw new HttpReqException(reason);
            }
            return response.getEntity().getContent();
        } catch (HttpReqException e) {
            throw e;
        } catch (IOException e) {
            log.warn("Request failed: {}", url, e.getMessage());
            throw new HttpReqException("Connection failed: " + e.getMessage());
        }
    }

    /** Map HTTP status + RSSHub error body to a human-readable reason. */
    private String classifyError(int status, CloseableHttpResponse response) {
        String body = "";
        try {
            if (response.getEntity() != null && response.getEntity().getContentLength() < 2048) {
                body = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {}

        // RSSHub server-level errors → DO NOT retry, tell user directly
        if (body.contains("Unmatched") || body.contains("regex") || body.contains("index")) {
            return "RSSHub服务器内部错误 (正则/路由定义bug) — 请检查RSSHub实例版本";
        }
        if (body.contains("ConfigNotFoundError") || body.contains("config")) {
            return "需要服务器端配置 — 该路由未在RSSHub实例上启用";
        }

        // Route-level errors
        if (body.contains("NotFoundError") || body.contains("not found")) {
            return "路由路径不存在 — 请用listRoutes确认路径拼写";
        }

        return switch (status) {
            case 404 -> "路由路径不存在 — 请用listRoutes检查路径";
            case 503 -> "路径错误或上游网站不可用 — 尝试其他路由";
            case 502 -> "上游网站错误 (502)";
            case 504 -> "上游网站超时 (504)";
            case 429 -> "请求过于频繁 (429)";
            default -> "HTTP " + status;
        };
    }
}
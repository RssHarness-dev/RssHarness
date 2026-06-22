package com.fanexmp.rssgse.rss.fetcher.http;

import com.fanexmp.rssgse.rss.exception.HttpReqException;
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

public class ApacheHttpReqWrapper implements HttpReqWrapper {

    private static final Logger log = LoggerFactory.getLogger(ApacheHttpReqWrapper.class);

    private static final Timeout CONNECT_TIMEOUT = Timeout.ofSeconds(3);
    private static final Timeout RESPONSE_TIMEOUT = Timeout.ofSeconds(3);

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
            // Note: Do NOT use try-with-resources here, the stream would be closed prematurely
            log.debug("Fetching URL: {}", url);
            CloseableHttpResponse response = httpClient.execute(request);
            int status = response.getCode();
            if (status != 200) {
                log.warn("HTTP {} from {}", status, url);
                response.close();
                throw new HttpReqException("HTTP " + status + " from " + url);
            }
            // Return response stream; caller is responsible for closing it
            return response.getEntity().getContent();
        } catch (IOException e) {
            log.warn("Request failed: {}", url, e);
            throw new HttpReqException("Request failed: " + url, e);
        }
    }
}
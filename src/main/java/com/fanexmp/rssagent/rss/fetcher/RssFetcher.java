package com.fanexmp.rssagent.rss.fetcher;

import com.fanexmp.rssagent.rss.dto.RssInstance;
import com.fanexmp.rssagent.rss.exception.HttpReqException;
import com.fanexmp.rssagent.rss.fetcher.http.HttpReqWrapper;
import com.fanexmp.rssagent.rss.fetcher.http.RssInstanceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class RssFetcher {

    private static final Logger log = LoggerFactory.getLogger(RssFetcher.class);

    private final RssInstanceManager rssInstanceManager;
    private final HttpReqWrapper reqWrapper;

    public RssFetcher(RssInstanceManager rssInstanceManager, HttpReqWrapper reqWrapper) {
        this.reqWrapper = reqWrapper;
        this.rssInstanceManager = rssInstanceManager;
    }

    public InputStream fetchInputStream(String route) {
        if (route == null || route.isEmpty()) {
            throw new HttpReqException("Route empty");
        }
        if (!rssInstanceManager.hasInstances()) {
            throw new HttpReqException("No RSS instances configured. "
                    + "Check the instance file at the configured rss.instance-path.");
        }
        switch (route.charAt(0)) {
            case '\\' -> route = "/" + route.substring(1);
            case '/' -> {
            }
            default -> route = "/" + route;
        }
        // Encode each path segment (Chinese chars → percent-encoding)
        StringBuilder encoded = new StringBuilder();
        for (String seg : route.split("/")) {
            if (seg.isEmpty()) continue;
            encoded.append("/").append(URLEncoder.encode(seg, StandardCharsets.UTF_8)
                    .replace("+", "%20"));
        }
        if (encoded.isEmpty()) encoded.append("/");
        String safeRoute = encoded.toString();

        StringBuilder errors = new StringBuilder();
        for (RssInstance instance : rssInstanceManager) {
            String fullUrl = instance.getBaseUrl() + safeRoute;
            try {
                log.debug("Fetching from instance {}: {}", instance.getName(), fullUrl);
                InputStream res = reqWrapper.get(fullUrl);
                rssInstanceManager.markSuccess(instance);
                return res;
            } catch (HttpReqException e) {
                String reason = e.getMessage() != null ? e.getMessage() : "Unknown";
                log.warn("Instance {} failed for route {}: {}", instance.getName(), route, reason);
                rssInstanceManager.markFailure(instance);
                if (!errors.isEmpty()) errors.append("; ");
                errors.append(reason);
            }
        }
        log.warn("All instances failed for route: {} — {}", route, errors);
        throw new HttpReqException(errors.isEmpty() ? "All instances failed" : errors.toString());
    }
}

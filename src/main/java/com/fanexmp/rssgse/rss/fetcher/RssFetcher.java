package com.fanexmp.rssgse.rss.fetcher;

import com.fanexmp.rssgse.rss.dto.RssInstance;
import com.fanexmp.rssgse.rss.exception.HttpReqException;
import com.fanexmp.rssgse.rss.fetcher.http.HttpReqWrapper;
import com.fanexmp.rssgse.rss.fetcher.http.RssInstanceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

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
        for (RssInstance instance : rssInstanceManager) {
            String fullUrl = instance.getBaseUrl() + route;
            try {
                log.debug("Fetching from instance {}: {}", instance.getName(), fullUrl);
                InputStream res = reqWrapper.get(fullUrl);
                rssInstanceManager.markSuccess(instance);
                return res;
            } catch (HttpReqException e) {
                log.warn("Instance {} failed for route {}: {}", instance.getName(), route, e.getMessage());
                rssInstanceManager.markFailure(instance);
            }
        }
        log.error("All instances failed for route: {}", route);
        throw new HttpReqException("All instances failed");
    }
}

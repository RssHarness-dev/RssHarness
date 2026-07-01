package com.fanexmp.rssharness.rss;

import com.fanexmp.rssharness.dto.FetchResponse;
import com.fanexmp.rssharness.rss.services.RouteFetchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Controller
public class RssController {

    RouteFetchService routeFetchService;

    @Autowired
    public RssController(RouteFetchService routeFetchService) {
        this.routeFetchService = routeFetchService;
    }

    public CompletableFuture<List<FetchResponse>> fetchRss(List<String> routes) {
        return routeFetchService.fetchRoutes(routes);
    }
}
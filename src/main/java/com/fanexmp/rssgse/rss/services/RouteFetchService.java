package com.fanexmp.rssgse.rss.services;

import com.fanexmp.rssgse.dto.FetchResponse;
import com.fanexmp.rssgse.dto.FetchStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class RouteFetchService {

    private static final Logger log = LoggerFactory.getLogger(RouteFetchService.class);

    @Autowired
    private ArticlesFetchService articlesFetchService;

    @Autowired
    private AiSummaryService aiSummaryService;

    @Autowired
    private SummaryStorageService summaryStorageService;

    public CompletableFuture<List<FetchResponse>> fetchRoutes(List<String> routes) {
        log.debug("fetchRoutes get {}", routes);
        List<CompletableFuture<FetchResponse>> futures = routes.stream()
                .map(this::processSingleRoute)
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .toList());
    }

    private CompletableFuture<FetchResponse> processSingleRoute(String route) {
        log.debug("Route {} is in calling", route);
        return articlesFetchService.fetchArticles(route)
                .thenCompose(articles -> {
                    if (articles == null) {
                        log.debug("Route {} is in cooldown period", route);
                        return CompletableFuture.completedFuture(
                                new FetchResponse(FetchStatus.INTERVAL, route, "Route is in cooldown period"));
                    }
                    return aiSummaryService.articlesToSummary(articles)
                            .thenCompose(summaries ->
                                    summaryStorageService.saveToDB(route, summaries)
                                            .thenApply(success -> {
                                                if (!success) {
                                                    log.error("Summary storage failed for route: {}", route);
                                                    return new FetchResponse(FetchStatus.SAVE_FAILED, route, "Summary storage failed");
                                                }
                                                log.info("Route {} processed successfully: {} articles", route, articles.getArticleList().size());
                                                return new FetchResponse(FetchStatus.SUCCESS, route,
                                                        String.format("Successfully fetched %d articles", articles.getArticleList().size()));
                                            })
                            );
                })
                .exceptionally(ex -> {
                    log.warn("Route {} processing failed: {}", route, ex.getMessage());
                    return new FetchResponse(FetchStatus.FAILED, route, ex.getMessage());
                });
    }
}
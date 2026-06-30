package com.fanexmp.rssagent.rss.services;

import com.fanexmp.rssagent.rss.dto.Articles;
import com.fanexmp.rssagent.rss.fetcher.RssFetcher;
import com.fanexmp.rssagent.rss.fetcher.config.RssConfigRepository;
import com.fanexmp.rssagent.rss.parser.RssXmlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

@Service
class ArticlesFetchService {

    private static final Logger log = LoggerFactory.getLogger(ArticlesFetchService.class);
    @Autowired
    private final RssXmlParser rssXmlParser;
    @Autowired
    private final RssFetcher rssFetcher;
    @Autowired
    private final RssConfigRepository rssConfigRepository;
    @Value("${rss.fetch-interval:1800}")
    private long fetchInterval;

    public ArticlesFetchService(RssXmlParser rssXmlParser, RssFetcher rssFetcher, RssConfigRepository rssConfigRepository) {
        this.rssXmlParser = rssXmlParser;
        this.rssFetcher = rssFetcher;
        this.rssConfigRepository = rssConfigRepository;
    }

    @Async
    public CompletableFuture<Articles> fetchArticles(String route) {
        if (!rssConfigRepository.tryMarkRefresh(route, fetchInterval)) {
            log.debug("Route {} is in cooldown period", route);
            return CompletableFuture.completedFuture(null);
        }

        try (InputStream inputStream = rssFetcher.fetchInputStream(route)) {
            Articles articles = rssXmlParser.parse(inputStream);
            log.info("Successfully fetched and parsed route: {}", route);
            return CompletableFuture.completedFuture(articles);
        } catch (RuntimeException e) {
            log.warn("Fetch failed for route {}: {}", route, e.getMessage());
            return CompletableFuture.failedFuture(e);
        } catch (IOException e) {
            log.warn("Stream close failed for route {}: {}", route, e.getMessage());
            return CompletableFuture.failedFuture(new RuntimeException(e));
        }
    }
}
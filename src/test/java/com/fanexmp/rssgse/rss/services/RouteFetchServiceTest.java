package com.fanexmp.rssgse.rss.services;

import com.fanexmp.rssgse.dto.FetchResponse;
import com.fanexmp.rssgse.dto.FetchStatus;
import com.fanexmp.rssgse.dto.Summary;
import com.fanexmp.rssgse.rss.dto.Articles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RouteFetchServiceTest {

    private ArticlesFetchService mockArticlesFetchService;
    private AiSummaryService mockAiSummaryService;
    private SummaryStorageService mockSummaryStorageService;
    private RouteFetchService service;

    @BeforeEach
    void setUp() {
        mockArticlesFetchService = mock(ArticlesFetchService.class);
        mockAiSummaryService = mock(AiSummaryService.class);
        mockSummaryStorageService = mock(SummaryStorageService.class);

        service = new RouteFetchService();
        injectField("articlesFetchService", mockArticlesFetchService);
        injectField("aiSummaryService", mockAiSummaryService);
        injectField("summaryStorageService", mockSummaryStorageService);
    }

    private void injectField(String name, Object value) {
        try {
            var field = RouteFetchService.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(service, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void should_return_interval_when_in_cooldown() {
        when(mockArticlesFetchService.fetchArticles("/route"))
                .thenReturn(CompletableFuture.completedFuture(null));

        List<FetchResponse> results = service.fetchRoutes(List.of("/route")).join();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(FetchStatus.INTERVAL);
    }

    @Test
    void should_return_success_on_full_pipeline() {
        Articles articles = new Articles(123L, "channel", "http://link");
        articles.setArticleList(List.of());
        when(mockArticlesFetchService.fetchArticles("/route"))
                .thenReturn(CompletableFuture.completedFuture(articles));

        List<Summary> summaries = List.of(new Summary());
        when(mockAiSummaryService.articlesToSummary(articles))
                .thenReturn(CompletableFuture.completedFuture(summaries));

        when(mockSummaryStorageService.saveToDB(summaries))
                .thenReturn(CompletableFuture.completedFuture(true));

        List<FetchResponse> results = service.fetchRoutes(List.of("/route")).join();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(FetchStatus.SUCCESS);
    }

    @Test
    void should_return_save_failed_when_storage_fails() {
        Articles articles = new Articles(123L, "channel", "http://link");
        articles.setArticleList(List.of());
        when(mockArticlesFetchService.fetchArticles("/route"))
                .thenReturn(CompletableFuture.completedFuture(articles));

        List<Summary> summaries = List.of(new Summary());
        when(mockAiSummaryService.articlesToSummary(articles))
                .thenReturn(CompletableFuture.completedFuture(summaries));

        when(mockSummaryStorageService.saveToDB(summaries))
                .thenReturn(CompletableFuture.completedFuture(false));

        List<FetchResponse> results = service.fetchRoutes(List.of("/route")).join();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(FetchStatus.SAVE_FAILED);
    }

    @Test
    void should_return_failed_on_exception() {
        CompletableFuture<Articles> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("Boom"));
        when(mockArticlesFetchService.fetchArticles("/route")).thenReturn(failed);

        List<FetchResponse> results = service.fetchRoutes(List.of("/route")).join();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(FetchStatus.FAILED);
    }
}
package com.fanexmp.rssharness.rss.services;

import com.fanexmp.rssharness.rss.dto.Articles;
import com.fanexmp.rssharness.rss.fetcher.RssFetcher;
import com.fanexmp.rssharness.rss.fetcher.config.RssConfigRepository;
import com.fanexmp.rssharness.rss.parser.RssXmlParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ArticlesFetchServiceTest {

    private RssXmlParser mockParser;
    private RssFetcher mockFetcher;
    private RssConfigRepository mockRepo;
    private ArticlesFetchService service;

    @BeforeEach
    void setUp() throws Exception {
        mockParser = mock(RssXmlParser.class);
        mockFetcher = mock(RssFetcher.class);
        mockRepo = mock(RssConfigRepository.class);
        service = new ArticlesFetchService(mockParser, mockFetcher, mockRepo);

        var field = ArticlesFetchService.class.getDeclaredField("fetchInterval");
        field.setAccessible(true);
        field.set(service, 1800L);
    }

    @Test
    void should_return_null_when_in_cooldown() {
        when(mockRepo.tryMarkRefresh("/route", 1800L)).thenReturn(false);

        Articles result = service.fetchArticles("/route").join();
        assertThat(result).isNull();
        verify(mockFetcher, never()).fetchInputStream(anyString());
    }

    @Test
    void should_fetch_and_parse_when_not_in_cooldown() {
        when(mockRepo.tryMarkRefresh("/route", 1800L)).thenReturn(true);

        InputStream mockStream = mock(InputStream.class);
        when(mockFetcher.fetchInputStream("/route")).thenReturn(mockStream);

        Articles expected = new Articles(0L, "channel", "http://link");
        when(mockParser.parse(mockStream)).thenReturn(expected);

        Articles result = service.fetchArticles("/route").join();
        assertThat(result).isSameAs(expected);
    }
}
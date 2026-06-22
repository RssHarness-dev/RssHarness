package com.fanexmp.rssgse.rss.fetcher;

import com.fanexmp.rssgse.rss.dto.Articles;
import com.fanexmp.rssgse.rss.parser.RssXmlParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full-stack integration test against a real RSSHub instance.
 * Requires rsshub-local at http://127.0.0.1:1200 to be running.
 */
@SpringBootTest
@ActiveProfiles("test")
class RssFetcherIntegrationTest {

    @Autowired
    private RssFetcher fetcher;

    @Autowired
    private RssXmlParser parser;

    @Test
    void should_fetch_and_parse_real_rss_from_zhihu_hot() {
        // Fetch from the real RSSHub instance
        InputStream stream = fetcher.fetchInputStream("/zhihu/hot");
        assertThat(stream).isNotNull();

        // Parse the RSS XML
        Articles articles = parser.parse(stream);
        assertThat(articles).isNotNull();
        assertThat(articles.getChannel()).isEqualTo("知乎热榜");
        assertThat(articles.getArticleList()).isNotEmpty();

        // Verify first article has reasonable fields
        var first = articles.getArticleList().get(0);
        assertThat(first.getTitle()).isNotEmpty();
        assertThat(first.getLink()).startsWith("https://www.zhihu.com");
        assertThat(first.getDescription()).isNotNull();
        assertThat(first.getPublisher()).isNotNull();
    }
}
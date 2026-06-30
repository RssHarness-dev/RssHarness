package com.fanexmp.rssagent.rss.dto;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndFeedImpl;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ArticlesTest {

    @Test
    void should_create_articles_via_constructor() {
        // Given
        int updateTime = 1234567890;
        String channel = "bilibili热搜";
        String link = "https://bilibili.com";

        // When
        Articles articles = new Articles(updateTime, channel, link);

        // Then
        assertThat(articles.getUpdateTime()).isEqualTo(updateTime);
        assertThat(articles.getChannel()).isEqualTo(channel);
        assertThat(articles.getLink()).isEqualTo(link);
        assertThat(articles.getArticleList()).isNull(); // 尚未设置
    }

    @Test
    void should_set_and_get_article_list() {
        // Given
        Articles articles = new Articles(1234567890, "bilibili热搜", "https://bilibili.com");
        List<Article> expected = Arrays.asList(
                createMockArticle("标题1", "link1"),
                createMockArticle("标题2", "link2")
        );

        // When
        articles.setArticleList(expected);

        // Then
        assertThat(articles.getArticleList())
                .isNotNull()
                .hasSize(2)
                .isSameAs(expected); // 验证是同一个对象引用
    }

    @Test
    void should_iterate_over_articles() {
        // Given
        Articles articles = new Articles(1234567890, "bilibili热搜", "https://bilibili.com");
        List<Article> mockList = Arrays.asList(
                createMockArticle("标题1", "link1"),
                createMockArticle("标题2", "link2")
        );
        articles.setArticleList(mockList);

        // When & Then
        int count = 0;
        for (Article article : articles) {
            assertThat(article).isNotNull();
            count++;
        }
        assertThat(count).isEqualTo(2);
    }

    @Test
    void should_iterate_empty_when_null_list() {
        // Given
        Articles articles = new Articles(1234567890L, "bilibili热搜", "https://bilibili.com");
        // articleList not set, still null — now treated as empty

        // When & Then: no NPE, iterates zero times
        int count = 0;
        for (Article article : articles) {
            count++;
        }
        assertThat(count).isZero();
    }

    @Test
    void should_create_from_synd_feed() {
        // Given: 构造一个真实的 SyndFeed
        SyndFeed feed = new SyndFeedImpl();
        feed.setTitle("bilibili热搜");
        feed.setLink("https://bilibili.com");
        feed.setAuthor("RSSHub");
        feed.setPublishedDate(new Date(1234567890000L));

        // When
        Articles articles = Articles.fromFeed(feed);

        // Then
        assertThat(articles.getChannel()).isEqualTo("bilibili热搜");
        assertThat(articles.getLink()).isEqualTo("https://bilibili.com");
        assertThat(articles.getUpdateTime()).isEqualTo(1234567890L);
        assertThat(articles.getArticleList()).isNull();
    }

    // ---------- 辅助方法 ----------
    private Article createMockArticle(String title, String link) {
        return new Article(
                link,
                title,
                "content",
                "publisher",
                "description",
                0,
                0,
                null,
                null,
                null
        );
    }
}
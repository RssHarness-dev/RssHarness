package com.fanexmp.rssgse.rss.dto;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class ArticleTest {

    private final SyndEntry entry;

    {
        ClassPathResource path = new ClassPathResource("xml/bilibili_hot_20260615_163816.xml");
        try (InputStream fis = path.getInputStream()) {
            XmlReader reader = new XmlReader(fis);
            entry = new SyndFeedInput().build(reader).getEntries().get(0);
        } catch (IOException | FeedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void should_return_correct_values_when_factory_called() {
        // When: 调用工厂方法
        Article article = Article.fromEntry(entry);
        System.out.println(article);

        // Then: 验证字段映射是否正确
        assertThat(article.title()).isEqualTo(entry.getTitle());
        assertThat(article.link()).isEqualTo(entry.getLink());

        // description 可能为 null，需判空
        String expectedDesc = entry.getDescription() != null ? entry.getDescription().getValue() : null;
        assertThat(article.content()).isEqualTo(expectedDesc);

        // publisher: RSS 中 item 没有 author，所以应为 null
        assertThat(article.publisher()).isEqualTo("");

        // updateTime: 这个 RSS 没有 pubDate，所以应为 0（或默认值）
        assertThat(article.updateTime()).isZero();

        // parserTime: 当前时间戳，应该大于 0
        assertThat(article.parserTime()).isGreaterThan(0);

        // tag/image/video: 如果构造器里把 null 转为空列表，则断言为空列表
        assertThat(article.tag()).isEmpty();
        assertThat(article.image()).isEmpty();
        assertThat(article.video()).isEmpty();
    }
}

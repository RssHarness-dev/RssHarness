package com.fanexmp.rssharness.rss.dto;

import com.rometools.rome.feed.synd.SyndEntry;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * @param updateTime Unix 时间戳（秒）
 */
public record Article(String link, String title, String content, String publisher, String description, long updateTime,
                      long parserTime, List<String> tag, List<String> video, List<String> image) {
    public Article(
            String link,
            String title,
            String content,
            String publisher,
            String description,
            long updateTime,
            long parserTime,
            List<String> tag,
            List<String> video,
            List<String> image
    ) {
        this.link = link;
        this.title = title;
        this.content = content;
        this.publisher = publisher;
        this.description = description;
        this.updateTime = updateTime;
        this.parserTime = parserTime;
        this.tag = tag != null ? tag : Collections.emptyList();
        this.video = video != null ? video : Collections.emptyList();
        this.image = image != null ? image : Collections.emptyList();
    }

    /**
     * 从 Rome 的 SyndFeed 转换为 Article
     * 注意：RSS 2.0 标准没有 tag/video/image 的专用字段，此处做简单映射
     */
    public static Article fromEntry(SyndEntry entry) {
        // 基础字段直接从 SyndEntry 获取
        String link = entry.getLink();
        String title = entry.getTitle();
        String description = (entry.getDescription() != null) ? entry.getDescription().getValue() : null;

        // content: RSS 2.0 没有独立的 content 字段，用 description 代替
        String content = description;

        // publisher: 使用 author 或 copyright
        String publisher = entry.getAuthor();

        // updateTime: 使用 publishedDate 或 updatedDate 的 epoch second
        long updateTime = 0;
        if (entry.getPublishedDate() != null) {
            updateTime = (entry.getPublishedDate().getTime() / 1000);
        }

        // parserTime: 当前时间
        long parserTime = Instant.now().getEpochSecond();

        // tag/video/image: RSS 2.0 标准没有这些字段，暂时留空
        // 如果你需要从扩展字段中提取，可以在这里添加逻辑
        List<String> tag = Collections.emptyList();
        List<String> video = Collections.emptyList();
        List<String> image = Collections.emptyList();

        return new Article(
                link,
                title,
                content,
                publisher,
                description,
                updateTime,
                parserTime,
                tag,
                video,
                image
        );
    }

    @Override
    public String toString() {
        return "Article{" +
                "link='" + link + '\'' +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", publisher='" + publisher + '\'' +
                ", description='" + description + '\'' +
                ", updateTime=" + updateTime +
                ", parserTime=" + parserTime +
                ", tag=" + tag +
                ", video=" + video +
                ", image=" + image +
                '}';
    }
}
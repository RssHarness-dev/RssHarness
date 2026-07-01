package com.fanexmp.rssagent.rss.parser;

import com.fanexmp.rssagent.rss.dto.Article;
import com.fanexmp.rssagent.rss.dto.Articles;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class RssXmlParserV1 implements RssXmlParser {

    @Override
    public Articles parse(InputStream is) {

        SyndFeed feed = inputStreamToFeed(is);


        List<Article> articleArrayList = this.feedToArticleList(feed);

        Articles articles = Articles.fromFeed(feed);
        articles.setArticleList(articleArrayList);

        return articles;

    }

    private SyndFeed inputStreamToFeed(@NonNull InputStream is) {

        try (XmlReader reader = new XmlReader(is)) {

            SyndFeedInput feedInput = new SyndFeedInput();
            return feedInput.build(reader);

        } catch (FeedException | IOException e) {
            throw new RuntimeException(e);
        }

    }

    private List<Article> feedToArticleList(@NonNull SyndFeed feed) {
        List<SyndEntry> entries = feed.getEntries();
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyList();
        }
        return entries.stream()
                .map(Article::fromEntry)
                .collect(Collectors.toList());
    }

}

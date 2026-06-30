package com.fanexmp.rssagent.rss.dto;


import com.rometools.rome.feed.synd.SyndFeed;
import org.jspecify.annotations.NonNull;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class Articles implements Iterable<Article> {


    private final long updateTime;
    private final String channel;
    private final String link;
    private List<Article> articleList;

    public Articles(long updateTime, String channel, String link) {
        this.updateTime = updateTime;
        this.channel = channel;
        this.link = link;
    }

    static public Articles fromFeed(SyndFeed feed) {
        long updateTime = feed.getPublishedDate() != null
                ? feed.getPublishedDate().getTime() / 1000
                : 0L;
        return new Articles(
                updateTime,
                feed.getTitle(),
                feed.getLink()
        );
    }

    public String getLink() {
        return link;
    }

    public String getChannel() {
        return channel;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public List<Article> getArticleList() {
        return articleList;
    }

    public void setArticleList(List<Article> articleList) {
        this.articleList = articleList;
    }

    @Override
    public @NonNull Iterator<Article> iterator() {
        if (articleList == null) {
            return Collections.emptyIterator();
        }
        return articleList.iterator();
    }

    public int length() {
        return articleList != null ? articleList.size() : 0;
    }

}

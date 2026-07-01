package com.fanexmp.rssharness.rss.fetcher.config;

import com.fanexmp.rssharness.rss.dto.RssSource;

import java.util.List;

public interface RssConfigRepository {
    List<RssSource> findAll();

    RssSource findByName(String name);

    RssSource findByRoute(String route);

    void addSource(RssSource source);

    void addSource(List<RssSource> sources);

    void delete(String name);

    void clear();

    void save(List<RssSource> sources);

    void save();

    List<RssSource> load();

    boolean tryMarkRefresh(String route, long intervalSeconds);

    /** Clear all fetch cooldowns — next fetch always goes through. */
    void clearCooldowns();
}
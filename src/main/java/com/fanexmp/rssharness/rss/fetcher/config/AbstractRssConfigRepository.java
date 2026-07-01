package com.fanexmp.rssharness.rss.fetcher.config;

import com.fanexmp.rssharness.rss.dto.RssSource;
import com.fanexmp.rssharness.rss.exception.RssConfigRepoException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;


public abstract class AbstractRssConfigRepository implements RssConfigRepository {

    protected final Map<String, RssSource> cache = new ConcurrentHashMap<>();
    protected final Map<String, RssSource> routeCache = new ConcurrentHashMap<>();

    public AbstractRssConfigRepository() {
    }

    @Override
    public abstract List<RssSource> load();

    @Override
    public abstract void save(List<RssSource> sources);

    @Override
    public void save() {
        save(new ArrayList<>(this.cache.values()));
    }

    protected void refreshCache() {
        List<RssSource> loaded = load();
        cache.clear();
        for (RssSource source : loaded) {
            cache.put(source.getName(), source);
        }
        for (RssSource source : loaded) {
            routeCache.put(source.getRoute(), source);
        }
    }

    public List<RssSource> findAll() {
        return new ArrayList<>(this.cache.values());
    }

    public RssSource findByName(String name) {
        if (!this.cache.containsKey(name)) {
            throw new RssConfigRepoException("findByName not find | " + name);
        }
        return this.cache.get(name);
    }

    public RssSource findByRoute(String route) {
        if (!this.routeCache.containsKey(route)) {
            RssSource newSource = new RssSource(route, route, true, 0);
            this.cache.put(route, newSource);
            this.routeCache.put(route, newSource);
            this.save(new ArrayList<>(this.cache.values()));
        }
        return this.routeCache.get(route);
    }

    /**
     * Atomically check and update refresh time for a route.
     * Returns true if the route was marked for refresh (interval elapsed),
     * false if still in cooldown.
     */
    public boolean tryMarkRefresh(String route, long intervalSeconds) {
        AtomicBoolean refreshed = new AtomicBoolean(false);
        routeCache.compute(route, (k, source) -> {
            if (source == null) {
                source = new RssSource(route, route, true, 0);
                cache.put(route, source);
            }
            long now = Instant.now().getEpochSecond();
            if (now - source.getLatestFetchTime() >= intervalSeconds) {
                source.setLatestFetchTime(now);
                refreshed.set(true);
            }
            return source;
        });
        return refreshed.get();
    }

    public void addSource(RssSource rssSource) {
        this.cache.put(rssSource.getName(), rssSource);
    }

    public void addSource(List<RssSource> sources) {
        for (RssSource source : sources) {
            this.cache.put(source.getName(), source);
        }
    }

    public void delete(String name) {
        if (!this.cache.containsKey(name)) {
            throw new RssConfigRepoException("delete not find | " + name);
        }
        this.cache.remove(name);
    }

    @Override
    public void clearCooldowns() {
        routeCache.clear();
    }

    public void clear() {
        this.cache.clear();
    }
}
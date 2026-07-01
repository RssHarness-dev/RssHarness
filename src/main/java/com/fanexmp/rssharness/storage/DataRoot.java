package com.fanexmp.rssharness.storage;

import com.fanexmp.rssharness.dto.Summary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataRoot {

    private final Map<String, List<Summary>> routeSummariesListPair = new ConcurrentHashMap<>();
    private List<String> routePlatforms = new ArrayList<>();
    private long lastRouteSync;

    public List<Summary> getSummaries(String route) {
        return this.routeSummariesListPair.computeIfAbsent(route, k -> new ArrayList<>());
    }

    // ── Route catalog persistence ──

    public List<String> getRoutePlatforms() {
        return routePlatforms;
    }

    public void setRoutePlatforms(List<String> routePlatforms) {
        this.routePlatforms = new ArrayList<>(routePlatforms);
    }

    public long getLastRouteSync() {
        return lastRouteSync;
    }

    public void setLastRouteSync(long lastRouteSync) {
        this.lastRouteSync = lastRouteSync;
    }
}
package com.fanexmp.rssgse.rss.fetcher.http;

import com.fanexmp.rssgse.rss.dto.RssInstance;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class RssInstanceManager implements Iterable<RssInstance> {

    private static final Logger log = LoggerFactory.getLogger(RssInstanceManager.class);
    private static final int WINDOW_SIZE = 10;

    private final Path path;
    private final Map<String, RssInstance> cache = new ConcurrentHashMap<>();
    private final Map<String, Deque<Boolean>> recentResults = new ConcurrentHashMap<>();
    private ObjectMapper mapper;

    public RssInstanceManager(Path path) {
        this.path = path;
        this.initMapper();
        this.initCache();

    }

    private void initMapper() {
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        try {
            Files.createDirectories(path.getParent());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create config directory", e);
        }
    }

    private void initCache() {
        try {
            List<RssInstance> rssInstanceList = mapper.readValue(path.toFile(), new TypeReference<>() {
            });
            for (RssInstance instance : rssInstanceList) {
                this.cache.put(instance.getName(), instance);
                this.recentResults.put(instance.getName(), new ConcurrentLinkedDeque<>());
            }
        } catch (IOException e) {
            log.error("Failed to read instance file: {}. Instance cache will be empty.", path, e);
        }
    }

    public void markSuccess(RssInstance instance) {
        String name = instance.getName();
        Deque<Boolean> window = this.recentResults.get(name);
        if (window != null) {
            window.addLast(true);
            if (window.size() > WINDOW_SIZE) {
                window.pollFirst();
            }
        }
    }

    public void markFailure(RssInstance instance) {
        String name = instance.getName();
        Deque<Boolean> window = this.recentResults.get(name);
        if (window != null) {
            window.addLast(false);
            if (window.size() > WINDOW_SIZE) {
                window.pollFirst();
            }
        }
    }

    public boolean hasInstances() {
        return !cache.isEmpty();
    }

    /**
     * Returns the recent success count (0–10) for an instance.
     */
    private int recentScore(String name) {
        Deque<Boolean> window = this.recentResults.get(name);
        if (window == null) return 0;
        return (int) window.stream().filter(Boolean::booleanValue).count();
    }

    @Override
    public Iterator<RssInstance> iterator() {
        List<RssInstance> rssInstances = new ArrayList<>(this.cache.values());
        rssInstances.sort((fir, sec) -> {
            int firScore = recentScore(fir.getName());
            int secScore = recentScore(sec.getName());
            return Integer.compare(secScore, firScore);
        });
        return rssInstances.iterator();
    }
}

package com.fanexmp.rssharness.rss.fetcher.config;

import com.fanexmp.rssharness.rss.dto.RssSource;
import com.fanexmp.rssharness.rss.exception.RssConfigRepoException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class JsonRssConfigRepository extends AbstractRssConfigRepository {

    private static final Logger log = LoggerFactory.getLogger(JsonRssConfigRepository.class);

    private final Path configPath;
    private ObjectMapper mapper;


    public JsonRssConfigRepository(Path configPath) {
        super();
        this.configPath = configPath;
        initMapper();
        refreshCache();
    }

    @Override
    public List<RssSource> load() {
        if (!Files.exists(configPath)) {
            log.info("Config file not found at {}, starting with empty config", configPath);
            return new ArrayList<>();
        }
        List<RssSource> rssSourceList;
        try {
            rssSourceList = mapper.readValue(configPath.toFile(), new TypeReference<>() {
            });
            log.info("Loaded {} RSS sources from {}", rssSourceList.size(), configPath);
        } catch (IOException e) {
            throw new RssConfigRepoException("Failed to read config from " + configPath, e);
        }
        return rssSourceList;
    }

    @Override
    public void save(List<RssSource> sources) {
        try {
            mapper.writeValue(configPath.toFile(), sources);
            log.debug("Saved {} RSS sources to {}", sources.size(), configPath);
        } catch (IOException e) {
            throw new RssConfigRepoException("Failed to save config to " + configPath, e);
        }
    }

    private void initMapper() {
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        try {
            Files.createDirectories(configPath.getParent());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create config directory: " + configPath.getParent(), e);
        }
    }
}
package com.fanexmp.rssgse.rss.services;

import com.fanexmp.rssgse.dto.Summary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
class SummaryStorageService {

    private static final Logger log = LoggerFactory.getLogger(SummaryStorageService.class);

    @Async
    public CompletableFuture<Boolean> saveToDB(List<Summary> summaries) {
        // TODO: Replace with Storage layer Instance
        log.debug("Saving {} summaries to storage", summaries.size());
        return CompletableFuture.completedFuture(true);
    }
}
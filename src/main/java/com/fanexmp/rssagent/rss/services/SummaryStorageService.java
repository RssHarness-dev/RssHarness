package com.fanexmp.rssagent.rss.services;

import com.fanexmp.rssagent.dto.Summary;
import com.fanexmp.rssagent.storage.dataview.DataViewFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
class SummaryStorageService {

    private static final Logger log = LoggerFactory.getLogger(SummaryStorageService.class);

    @Autowired
    private DataViewFactory dataViewFactory;

    @Async
    public CompletableFuture<Boolean> saveToDB(String route, List<Summary> summaries) {
        log.debug("Saving {} summaries for route {}", summaries.size(), route);
        boolean success = dataViewFactory.fromRoute(route).updateSummaries(summaries);
        return CompletableFuture.completedFuture(success);
    }
}
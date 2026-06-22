package com.fanexmp.rssgse.rss.services;

import com.fanexmp.rssgse.dto.Summary;
import com.fanexmp.rssgse.rss.dto.Article;
import com.fanexmp.rssgse.rss.dto.Articles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
class AiSummaryService {

    private static final Logger log = LoggerFactory.getLogger(AiSummaryService.class);

    @Async
    public CompletableFuture<List<Summary>> articlesToSummary(Articles articles) {
        // TODO: replace with AI Domain

        log.debug("Generating summaries for {} articles", articles.length());
        List<Summary> res = new ArrayList<>();

        for (Article article : articles) {
            Summary summary = new Summary(
                    article.getLink(),
                    articles.getChannel() + "/" + article.getPublisher(),
                    articles.getUpdateTime() + "/" + article.getUpdateTime(),
                    "Model Which cheapest",
                    "Something will exist",
                    Instant.now().getEpochSecond(),
                    article.getTitle(),
                    "Something maybe exist in Demo, or dev in future",
                    article.getDescription(),
                    article.getContent(),
                    100
            );
            res.add(summary);
        }

        return CompletableFuture.completedFuture(res);
    }

}
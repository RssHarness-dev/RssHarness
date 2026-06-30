package com.fanexmp.rssagent.rss.services;

import com.fanexmp.rssagent.dto.Summary;
import com.fanexmp.rssagent.rss.dto.Article;
import com.fanexmp.rssagent.rss.dto.Articles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
class AiSummaryService {

    private static final Logger log = LoggerFactory.getLogger(AiSummaryService.class);

    private static final String JSON_SYSTEM = """
            You are a JSON-only API. Always respond with a valid JSON array of objects.
            Never include explanations, preambles, markdown, or any text outside the JSON array.\
            """;

    @Autowired
    @Lazy
    @Qualifier("ChatClientForSummary")
    private ChatClient chatClient;

    @Async
    public CompletableFuture<List<Summary>> articlesToSummary(Articles articles) {
        log.debug("Generating AI summaries for {} articles from channel {}", articles.length(), articles.getChannel());

        if (articles.length() == 0) {
            return CompletableFuture.completedFuture(List.of());
        }

        // Build a prompt with article info — truncate descriptions to avoid LLM JSON breakage
        StringBuilder articlesText = new StringBuilder();
        var articleList = articles.getArticleList();
        for (int i = 0; i < articleList.size(); i++) {
            Article a = articleList.get(i);
            String desc = a.description() != null ? a.description() : "";
            if (desc.length() > 200) desc = desc.substring(0, 200) + "...";
            // Strip characters that break JSON in LLM output
            desc = desc.replace("\"", "'").replace("\\", "");
            articlesText.append("[%d] %s  (%s)\n    %s\n\n"
                    .formatted(i + 1, a.title(), a.publisher(), desc));
        }

        String prompt = """
                You are a news summarizer. For each article below, write a one-sentence
                Chinese summary. Return ONLY a JSON array — nothing else.
                Format: [{"index":1,"summary":"100字总结","score":你认为信息的可信度与强度的综合评分}, ...]

                Channel: %s
                Articles:
                %s
                """.formatted(articles.getChannel(), articlesText.toString());

        try {
            String response = chatClient.prompt()
                    .system(JSON_SYSTEM)
                    .user(prompt)
                    .call()
                    .content();

            List<Summary> summaries = parseSummaryResponse(response, articles, articleList);
            if (summaries.isEmpty()) {
                log.warn("AI returned no parseable summaries, using fallback for channel {}", articles.getChannel());
                return CompletableFuture.completedFuture(buildFallbackSummaries(articles));
            }
            log.debug("Generated {} summaries for channel {}", summaries.size(), articles.getChannel());
            return CompletableFuture.completedFuture(summaries);
        } catch (Exception e) {
            log.warn("AI summary failed, falling back to placeholder summaries", e);
            return CompletableFuture.completedFuture(buildFallbackSummaries(articles));
        }
    }

    private List<Summary> buildFallbackSummaries(Articles articles) {
        List<Summary> fallback = new ArrayList<>();
        for (Article article : articles) {
            fallback.add(new Summary(
                    article.link(),
                    articles.getChannel() + "/" + article.publisher(),
                    articles.getUpdateTime() + "/" + article.updateTime(),
                    "fallback", "v1",
                    Instant.now().getEpochSecond(),
                    article.title(),
                    "AI summary unavailable",
                    article.description(),
                    article.content(),
                    50
            ));
        }
        return fallback;
    }

    /**
     * Extracts a JSON array from LLM output that may contain surrounding natural language.
     */
    private String extractJsonArray(String llmResponse) {
        if (llmResponse == null || llmResponse.isBlank()) {
            return "[]";
        }
        String trimmed = llmResponse.trim();

        // Strip markdown code fences
        String cleaned = trimmed
                .replaceAll("^```(json)?\\s*", "")
                .replaceAll("\\s*```$", "");

        // Try to find JSON array boundaries
        int openBracket = cleaned.indexOf('[');
        int closeBracket = cleaned.lastIndexOf(']');
        if (openBracket >= 0 && closeBracket > openBracket) {
            return cleaned.substring(openBracket, closeBracket + 1);
        }
        return cleaned;
    }

    @SuppressWarnings("unchecked")
    private List<Summary> parseSummaryResponse(String response, Articles articles,
                                                List<Article> articleList) {
        try {
            String json = extractJsonArray(response);
            log.debug("Extracted summary JSON: {}", json);
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            List<java.util.Map<String, Object>> list = mapper.readValue(json, List.class);

            List<Summary> results = new ArrayList<>();
            for (java.util.Map<String, Object> item : list) {
                int idx = ((Number) item.get("index")).intValue() - 1;
                if (idx >= 0 && idx < articleList.size()) {
                    Article a = articleList.get(idx);
                    results.add(new Summary(
                            a.link(),
                            articles.getChannel() + "/" + a.publisher(),
                            String.valueOf(articles.getUpdateTime()),
                            "deepseek-chat", "v1",
                            Instant.now().getEpochSecond(),
                            a.title(),  // use actual article title, not LLM output
                            (String) item.getOrDefault("summary", ""),
                            a.description(),
                            a.content(),
                            ((Number) item.getOrDefault("score", 50)).intValue()
                    ));
                }
            }
            return results;
        } catch (Exception e) {
            log.warn("Failed to parse AI summary response. Raw: {}", response, e);
            return List.of();
        }
    }
}
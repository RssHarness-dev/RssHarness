package com.fanexmp.rssgse.ai.conversation;

import com.fanexmp.rssgse.ai.rag.RouteCatalog;
import com.fanexmp.rssgse.ai.tool.RssTools;
import com.fanexmp.rssgse.dto.FetchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Single-call conversational search.  Conversation history is handled
 * automatically by {@link MessageChatMemoryAdvisor} (configured in
 * {@code AiConfig}) — the sessionId is passed per-request via advisor param.
 */
@Service
public class ConversationService {

    private static final Logger log = LoggerFactory.getLogger(ConversationService.class);

    @Autowired private ChatClient chatClient;
    @Autowired private RouteCatalog routeCatalog;
    @Autowired private RssTools rssTools;

    public List<FetchResponse> search(String sessionId, String question) {
        chatClient.prompt()
                .user(question)
                .advisors(a -> a.param("chat_memory_conversation_id", sessionId))
                .call()
                .content();
        return rssTools.getLastResults();
    }

    public List<FetchResponse> searchStreaming(String sessionId, String question, SearchCallback cb) {
        cb.onThinking("Thinking …");
        var charCount = new int[1];
        try {
            chatClient.prompt()
                    .user(question)
                    .advisors(a -> a.param("chat_memory_conversation_id", sessionId))
                    .stream()
                    .content()
                    .doOnNext(token -> {
                        charCount[0] += token.length();
                        cb.onResponseToken(token);
                    })
                    .blockLast();
            cb.onTokens(charCount[0] / 3);
        } catch (Exception e) {
            log.warn("Streaming failed", e);
            cb.onError("ai", e.getMessage());
        }

        List<FetchResponse> results = rssTools.getLastResults();
        if (!results.isEmpty()) {
            cb.onSeparator();
            for (FetchResponse r : results) {
                cb.onFetchResult(r.getRoute(), r.getStatus().name(), r.getInfo());
            }
            long ok = results.stream()
                    .filter(r -> r.getStatus() == com.fanexmp.rssgse.dto.FetchStatus.SUCCESS)
                    .count();
            cb.onResponse(String.format("%d/%d routes fetched · %d success",
                    results.size(), results.size(), ok));
        }
        return results;
    }
}
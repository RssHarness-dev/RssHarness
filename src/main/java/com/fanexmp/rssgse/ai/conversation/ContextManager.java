package com.fanexmp.rssgse.ai.conversation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages multi-turn conversation state with token budget enforcement.
 * Sessions are in-memory (backed by ChatMemory for Spring AI advisors).
 */
@Component
public class ContextManager {

    private static final Logger log = LoggerFactory.getLogger(ContextManager.class);

    // Rough estimate: English ~4 chars/token, Chinese ~1.5 chars/token
    private static final double CHARS_PER_TOKEN = 3.0;

    @Value("${rssgse.context.max-tokens:8000}")
    private int maxTokens;

    @Value("${rssgse.context.reserve-tokens:2000}")
    private int reserveTokens; // tokens reserved for the response

    private final Map<String, List<Message>> sessions = new ConcurrentHashMap<>();

    /**
     * Add a message to the conversation and trim if needed.
     */
    public void addMessage(String sessionId, Message message) {
        List<Message> messages = sessions.computeIfAbsent(sessionId, k -> new ArrayList<>());
        messages.add(message);
        trimIfNeeded(sessionId);
    }

    /**
     * Get all messages for a session in chronological order.
     */
    public List<Message> getMessages(String sessionId) {
        return Collections.unmodifiableList(
                sessions.getOrDefault(sessionId, Collections.emptyList()));
    }

    /**
     * Estimate the total tokens used by this session's messages.
     */
    public int estimateTokens(String sessionId) {
        List<Message> messages = sessions.get(sessionId);
        if (messages == null) return 0;

        int total = 0;
        for (Message msg : messages) {
            String text = msg.getText();
            if (text != null) {
                total += (int) (text.length() / CHARS_PER_TOKEN);
            }
        }
        return total;
    }

    /**
     * Trim oldest messages (excluding system) until under budget.
     * Keeps the first system message and the last N messages.
     */
    private void trimIfNeeded(String sessionId) {
        List<Message> messages = sessions.get(sessionId);
        if (messages == null) return;

        int budget = maxTokens - reserveTokens;
        int currentTokens = estimateTokens(sessionId);

        while (currentTokens > budget && messages.size() > 1) {
            // Never remove system messages
            int removeIndex = -1;
            for (int i = 0; i < messages.size(); i++) {
                if (messages.get(i).getMessageType() != MessageType.SYSTEM) {
                    removeIndex = i;
                    break;
                }
            }
            if (removeIndex < 0) break;

            Message removed = messages.remove(removeIndex);
            currentTokens -= (int) (removed.getText().length() / CHARS_PER_TOKEN);
        }

        if (currentTokens > budget) {
            log.warn("Session {}: unable to trim below token budget ({} > {})",
                    sessionId, currentTokens, budget);
        }
    }

    public void clearSession(String sessionId) {
        sessions.remove(sessionId);
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public int getReserveTokens() {
        return reserveTokens;
    }
}
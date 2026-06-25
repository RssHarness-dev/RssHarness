package com.fanexmp.rssgse.ai.conversation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ContextManagerTest {

    private ContextManager manager;

    @BeforeEach
    void setUp() {
        manager = new ContextManager();
        // Inject values via reflection for testing
        setField("maxTokens", 2000);
        setField("reserveTokens", 500);
    }

    private void setField(String name, int value) {
        try {
            var field = ContextManager.class.getDeclaredField(name);
            field.setAccessible(true);
            field.setInt(manager, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void should_add_and_retrieve_messages() {
        manager.addMessage("s1", new UserMessage("hello"));
        manager.addMessage("s1", new AssistantMessage("hi there"));

        List<Message> messages = manager.getMessages("s1");
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).getText()).isEqualTo("hello");
    }

    @Test
    void should_isolate_sessions() {
        manager.addMessage("s1", new UserMessage("q1"));
        manager.addMessage("s2", new UserMessage("q2"));

        assertThat(manager.getMessages("s1")).hasSize(1);
        assertThat(manager.getMessages("s2")).hasSize(1);
    }

    @Test
    void should_estimate_tokens() {
        manager.addMessage("s1", new UserMessage("Hello world")); // ~4 tokens
        int tokens = manager.estimateTokens("s1");
        assertThat(tokens).isGreaterThan(0);
    }

    @Test
    void should_clear_session() {
        manager.addMessage("s1", new UserMessage("test"));
        manager.clearSession("s1");
        assertThat(manager.getMessages("s1")).isEmpty();
    }

    @Test
    void should_return_empty_for_unknown_session() {
        assertThat(manager.getMessages("nonexistent")).isEmpty();
        assertThat(manager.estimateTokens("nonexistent")).isZero();
    }
}
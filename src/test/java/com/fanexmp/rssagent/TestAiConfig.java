package com.fanexmp.rssagent;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

/**
 * Provides a mock ChatModel for test contexts where no real API key is available.
 * Returns plausible responses so the AI summary pipeline and streaming flows work.
 */
@TestConfiguration
public class TestAiConfig {

    @Bean
    @Primary
    public ChatModel mockChatModel() {
        return new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                // Return a valid summary JSON so AiSummaryService can parse it.
                // Format: [{"index":1,"title":"...","summary":"...","score":80}]
                String json = """
                        [{"index":1,"title":"Mock Article","summary":"Mock summary for testing.","score":80}]\
                        """;
                AssistantMessage msg = new AssistantMessage(json);
                return ChatResponse.builder()
                        .generations(List.of(new Generation(msg)))
                        .build();
            }

            @Override
            public Flux<ChatResponse> stream(Prompt prompt) {
                // Simulate streaming with a few chunks
                return Flux.just("[\"zhihu\",", "\"weibo\",", "\"github\"]")
                        .map(chunk -> ChatResponse.builder()
                                .generations(List.of(
                                        new Generation(new AssistantMessage(chunk))))
                                .build())
                        .delayElements(Duration.ofMillis(10));
            }
        };
    }
}
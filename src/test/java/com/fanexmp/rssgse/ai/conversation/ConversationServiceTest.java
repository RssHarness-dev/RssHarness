package com.fanexmp.rssgse.ai.conversation;

import com.fanexmp.rssgse.ai.rag.RouteCatalog;
import com.fanexmp.rssgse.ai.tool.RssTools;
import com.fanexmp.rssgse.dto.FetchResponse;
import com.fanexmp.rssgse.dto.FetchStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ConversationServiceTest {

    private ChatClient chatClient;
    private RouteCatalog routeCatalog;
    private RssTools rssTools;
    private ConversationService service;

    // Mock chain: prompt() → user() → call() → entity()
    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.CallResponseSpec responseSpec;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        chatClient = mock(ChatClient.class);
        routeCatalog = new RouteCatalog();
        rssTools = mock(RssTools.class);

        requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        responseSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.advisors(any())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);

        service = new ConversationService();
        injectField("chatClient", chatClient);
        injectField("routeCatalog", routeCatalog);
        injectField("rssTools", rssTools);

        // Seed route catalog
        routeCatalog.loadFromJson(Map.of(
                "zhihu", Map.of(
                        "hot", Map.of("description", "知乎热榜"),
                        "daily", Map.of("description", "知乎日报")
                ),
                "github", Map.of(
                        "trending", Map.of("description", "GitHub Trending")
                )
        ));
    }

    private void injectField(String name, Object value) {
        try {
            var field = ConversationService.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(service, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void should_return_empty_when_no_results() {
        when(rssTools.getLastResults()).thenReturn(List.of());

        List<FetchResponse> results = service.search("s1", "test");

        assertThat(results).isEmpty();
    }

    @Test
    void should_return_results_when_llm_fetched() {
        when(rssTools.getLastResults()).thenReturn(List.of(
                new FetchResponse(FetchStatus.SUCCESS, "/zhihu/hot", "OK"),
                new FetchResponse(FetchStatus.SUCCESS, "/github/trending", "OK")));

        List<FetchResponse> results = service.search("s1", "有什么热点");

        assertThat(results).hasSize(2);
    }
}
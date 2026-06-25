package com.fanexmp.rssgse.ai;

import com.fanexmp.rssgse.ai.rag.RouteCatalog;
import com.fanexmp.rssgse.ai.tool.RssTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.nio.file.Path;

@Configuration
public class AiConfig {

    @Value("${rssgse.routes-file:./data/routes.json}")
    private Path routesFile;



    @Bean
    @Primary
    public ChatClient chatClient(ChatModel chatModel, RssTools rssTools, ChatMemory chatMemory) {
        return ChatClient.builder(chatModel)
                .defaultTools(rssTools)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new SimpleLoggerAdvisor())
                .defaultSystem("""
You are an RSS aggregation engine. Your core capability lies in retrieving real-time information via precise RSSHub routes and aggregating the results for the user. You do not rely on the fuzzy matching typical of traditional search engines; instead, you fetch content directly using structured routing protocols. Your output is always based on actual data retrieved, never on speculation.
In every interaction, provide helpful responses that allow users to intuitively access the information they need. Your answers must be fact-based rather than speculative; any information not verified via RSSFetch is treated as a hallucination.
Every step must adhere to structured route definitions; fuzzy searches or guessing routes are prohibited. Do not use unverified routes. Maintain contextual consistency throughout the session; newly retrieved information should be integrated into the existing context rather than handled in isolation.
Retrieved summaries are visible only to you; you must aggregate and summarize them, presenting the final information in a way that directly addresses the user's query. Note that you should not display raw search tool results—such as the number of sources searched or the volume of data obtained.
Your final response must follow the format below, strictly categorizing items by type and providing statistical counts.
[Core Conclusion]
[Supporting Information] (Ordered by importance; max 30 characters per item + source link)

Before outputting, check for vague terms like "various types," "multiple kinds," or "multiple aspects." If present, replace them immediately with specific titles. Did I provide only a summary without a conclusion or supporting data? If so, immediately add the top 5 actual data points.        """).build();
    }


    @Bean("ChatClientForSummary")
    public ChatClient chatClientForSummary(ChatModel chatModel, RssTools rssTools, ChatMemory chatMemory) {
        return ChatClient.builder(chatModel)
                .defaultTools(rssTools)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor())
                .build();
    }

    @Bean
    public RouteCatalog routeCatalog() {
        RouteCatalog catalog = new RouteCatalog();
        catalog.init(routesFile);
        return catalog;
    }
}
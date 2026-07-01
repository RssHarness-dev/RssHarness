package com.fanexmp.rssagent.rss;

import com.fanexmp.rssagent.rss.fetcher.RssFetcher;
import com.fanexmp.rssagent.rss.fetcher.config.JsonRssConfigRepository;
import com.fanexmp.rssagent.rss.fetcher.config.RssConfigRepository;
import com.fanexmp.rssagent.rss.fetcher.http.ApacheHttpReqWrapper;
import com.fanexmp.rssagent.rss.fetcher.http.HttpReqWrapper;
import com.fanexmp.rssagent.rss.fetcher.http.RssInstanceManager;
import com.fanexmp.rssagent.rss.parser.RssXmlParser;
import com.fanexmp.rssagent.rss.parser.RssXmlParserV1;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executor;


@Configuration
@EnableAsync
public class RssConfig {

    @Value("${rss.config-path:config/rss-sources.json}")
    private String configPath;
    @Value("${rss.instance-path:config/rss-instances.json}")
    private String instancePath;

    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("rss-async-");
        executor.initialize();
        return executor;
    }

    @Bean
    public RssConfigRepository rssConfigRepository() {
        Path path = Paths.get(configPath);
        return new JsonRssConfigRepository(path);
    }

    @Bean
    public RssXmlParser rssXmlParser() {
        return new RssXmlParserV1();
    }

    @Bean
    public HttpReqWrapper httpReqWrapper() {
        return new ApacheHttpReqWrapper();
    }

    @Bean
    public RssFetcher rssFetcher(HttpReqWrapper httpReqWrapper, RssInstanceManager rssInstanceManager) {
        return new RssFetcher(rssInstanceManager, httpReqWrapper);
    }

    @Bean
    public RssInstanceManager rssInstanceManager() {
        Path path = Paths.get(instancePath);
        return new RssInstanceManager(path);
    }
}
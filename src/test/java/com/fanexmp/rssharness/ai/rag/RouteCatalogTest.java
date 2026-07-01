package com.fanexmp.rssharness.ai.rag;

import com.fanexmp.rssharness.ai.dto.RouteEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RouteCatalogTest {

    private RouteCatalog catalog;

    @BeforeEach
    void setUp() {
        catalog = new RouteCatalog();
    }

    @Test
    void should_parse_valid_routes_json() {
        // Simulate the RSSHub /routes JSON structure
        Map<String, Object> rawJson = Map.of(
                "bilibili", Map.of(
                        "hot-search", Map.of("description", "哔哩哔哩热搜", "example", "rsshub.app/bilibili/hot-search"),
                        "user/video/:uid", Map.of("description", "B站用户投稿视频", "example", "rsshub.app/bilibili/user/video/123")
                ),
                "zhihu", Map.of(
                        "hot", Map.of("description", "知乎热榜", "example", "rsshub.app/zhihu/hot")
                )
        );

        catalog.loadFromJson(rawJson);

        assertThat(catalog.platformCount()).isEqualTo(2);
        assertThat(catalog.routeCount()).isEqualTo(3);
    }

    @Test
    void should_list_platforms() {
        Map<String, Object> rawJson = Map.of(
                "github", Map.of("trending", Map.of()),
                "v2ex", Map.of("hot", Map.of())
        );

        catalog.loadFromJson(rawJson);

        List<String> platforms = catalog.getAllPlatforms();
        assertThat(platforms).contains("github", "v2ex");
    }

    @Test
    void should_get_routes_for_platform() {
        Map<String, Object> rawJson = Map.of(
                "github", Map.of(
                        "trending", Map.of("description", "GitHub Trending"),
                        "search/:keyword", Map.of("description", "Search GitHub")
                ),
                "zhihu", Map.of("hot", Map.of())
        );

        catalog.loadFromJson(rawJson);

        List<RouteEntry> githubRoutes = catalog.getRoutesForPlatform("github");
        assertThat(githubRoutes).hasSize(2);
        assertThat(githubRoutes.get(0).namespace()).isEqualTo("github");
    }

    @Test
    void should_handle_empty_json() {
        catalog.loadFromJson(Map.of());
        assertThat(catalog.platformCount()).isZero();
        assertThat(catalog.routeCount()).isZero();
    }

    @Test
    void should_format_platform_list() {
        Map<String, Object> rawJson = Map.of(
                "zhihu", Map.of("hot", Map.of()),
                "weibo", Map.of("keyword/:keyword", Map.of())
        );
        catalog.loadFromJson(rawJson);

        String formatted = catalog.formatPlatformList();
        assertThat(formatted).contains("zhihu").contains("weibo");
    }
}
package com.fanexmp.rssagent.ai.dto;

import java.util.List;
import java.util.Map;

/**
 * Typed records for structured LLM output via {@code ChatClient.entity()}.
 */
public final class AiSelections {

    private AiSelections() {}

    /** Tier 1: top 5 platforms. */
    public record PlatformSelection(List<String> platforms) {}

    /** Tier 2: a route template + param values to substitute. */
    public record RouteParam(
            /** Exact catalog path, e.g. "/weibo/keyword/:keyword" */
            String template,
            /** Values for each :param placeholder, e.g. {"keyword": "AI"} */
            Map<String, String> params
    ) {}

    /** Tier 2: up to 10 route-param pairs. */
    public record RouteSelection(List<RouteParam> routes) {}
}
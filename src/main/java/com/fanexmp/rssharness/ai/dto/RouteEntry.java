package com.fanexmp.rssagent.ai.dto;

import java.util.List;

/**
 * Cleaned route metadata from RSSHub /routes endpoint.
 * Example:
 *   path=/weibo/keyword/:keyword
 *   namespace=weibo
 *   description=你想订阅的微博关键词
 *   example=rsshub.app/weibo/keyword/RSSHub
 *   requiresConfig=WEIBO_COOKIES (optional)
 */
public record RouteEntry(
        String path,
        String namespace,
        String description,
        String example,
        List<String> params,
        boolean requiresConfig,
        boolean usesPuppeteer
) {
    public boolean isPathParam() {
        return params != null && !params.isEmpty();
    }
}
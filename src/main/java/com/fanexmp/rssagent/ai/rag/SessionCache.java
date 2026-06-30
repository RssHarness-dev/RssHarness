package com.fanexmp.rssagent.ai.rag;

import com.fanexmp.rssagent.ai.dto.RouteEntry;

import java.util.*;

/**
 * Per-query temporary storage. Lifecycle: created at query start, discarded after response.
 * Not persisted — only lives in memory for the duration of a single ConversationService call.
 */
public class SessionCache {

    private final String sessionId;
    private List<String> selectedPlatforms = new ArrayList<>();
    private Map<String, List<RouteEntry>> platformRoutes = new LinkedHashMap<>();
    private List<String> finalRoutes = new ArrayList<>();

    public SessionCache(String sessionId) {
        this.sessionId = sessionId;
    }

    // ── Tier 1: platform selection ──

    public void setSelectedPlatforms(List<String> platforms) {
        this.selectedPlatforms = new ArrayList<>(platforms);
    }

    public List<String> getSelectedPlatforms() {
        return Collections.unmodifiableList(selectedPlatforms);
    }

    // ── Tier 2: route selection per platform ──

    public void setPlatformRoutes(String platform, List<RouteEntry> routes) {
        this.platformRoutes.put(platform, new ArrayList<>(routes));
    }

    public void addFinalRoute(String route) {
        this.finalRoutes.add(route);
    }

    public List<String> getFinalRoutes() {
        return Collections.unmodifiableList(finalRoutes);
    }

    // ── Lifecycle ──

    public String getSessionId() {
        return sessionId;
    }

    public void clear() {
        selectedPlatforms.clear();
        platformRoutes.clear();
        finalRoutes.clear();
    }
}
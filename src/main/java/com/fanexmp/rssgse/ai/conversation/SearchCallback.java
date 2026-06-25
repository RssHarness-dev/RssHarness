package com.fanexmp.rssgse.ai.conversation;

import java.util.List;

/**
 * Three-level streaming callback for the search pipeline.
 *
 * <ul>
 *   <li><b>Thinking</b> (gray) — LLM reasoning stream before tool/data extraction</li>
 *   <li><b>Tool call / Extraction</b> (cyan) — structured result captured</li>
 *   <li><b>Response</b> (white) — final deliverable: fetch results + LLM summary</li>
 * </ul>
 */
public interface SearchCallback {

    // ── Thinking (gray) ──

    default void onThinking(String label) {}
    default void onToken(String token) {}

    // ── Tool/Extraction (cyan) ──

    default void onToolCall(String toolName, String detail) {}

    // ── Response (white) ──

    /** White streaming token — final LLM summary output (Tier 4). */
    default void onResponseToken(String token) {}

    /** White static text line. */
    default void onResponse(String text) {}

    // ── Fetch progress ──

    /** Called when fetch begins. */
    default void onFetchStart(int total) {}

    /** Called for each completed route (success/fail/cooldown). */
    default void onFetchResult(String route, String status, String info) {}

    /** Called when all fetches complete. */
    default void onFetchDone(int success, int failed, int cooldown) {}

    // ── Token usage ──

    /** Estimated token count for the current LLM call. */
    default void onTokens(int estimated) {}

    // ── Structure ──

    default void onSeparator() {}
    default void onError(String phase, String message) {}
}

package com.fanexmp.rssharness;

import com.fanexmp.rssharness.ai.conversation.ConversationService;
import com.fanexmp.rssharness.ai.conversation.SearchCallback;
import com.fanexmp.rssharness.ai.rag.RouteCatalog;
import com.fanexmp.rssharness.ai.rag.RouteSyncTask;
import com.fanexmp.rssharness.dto.FetchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Scanner;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "rssharness.cli.enabled", havingValue = "true", matchIfMissing = false)
public class CliRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CliRunner.class);

    // ANSI
    private static final String GRAY  = "\033[90m";
    private static final String CYAN  = "\033[36m";
    private static final String WHITE = "\033[97m";
    private static final String RED   = "\033[91m";
    private static final String GREEN = "\033[92m";
    private static final String YELLOW= "\033[93m";
    private static final String RESET = "\033[0m";
    private static final String SEP   = GRAY + "  ──────────────────────────────────────────────" + RESET;

    @Autowired private ConversationService conversationService;
    @Autowired private RouteCatalog routeCatalog;
    @Autowired private RouteSyncTask routeSyncTask;

    private String sessionId;

    @Override
    public void run(String... args) {
        sessionId = UUID.randomUUID().toString();

        System.out.printf("  Route catalog: %d platforms, %d routes.%n",
                routeCatalog.platformCount(), routeCatalog.routeCount());
        System.out.println();
        System.out.println("  Type a question to search, or:");
        System.out.println("    /sync     — Refresh route catalog from RSSHub");
        System.out.println("    /routes   — List platforms");
        System.out.println("    /new      — Start a new session");
        System.out.println("    /help     — Show help");
        System.out.println("    /exit     — Quit");
        System.out.println();

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("RssHarness> ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;

            if (input.equals("/exit") || input.equals("/quit")) {
                System.out.println("  Goodbye.");
                break;
            }
            if (input.equals("/help")) { printHelp(); continue; }
            if (input.equals("/routes")) { printPlatforms(); continue; }
            if (input.startsWith("/routes ")) { printRoutes(input.substring(8).trim()); continue; }
            if (input.equals("/new")) {
                sessionId = UUID.randomUUID().toString();
                System.out.println(GRAY + "  ✨ New session: " + sessionId.substring(0, 8) + "..." + RESET);
                continue;
            }
            if (input.equals("/sync")) {
                System.out.println(GRAY + "  ⏳ Syncing from RSSHub..." + RESET);
                String result = routeSyncTask.sync();
                System.out.println(CYAN + "  " + result + RESET);
                continue;
            }
            // ── Default: treat as search question ──
            handleQuestion(input);
        }
        scanner.close();
    }

    // ──────────────────────────────────────────────
    //  Search pipeline with progress + streaming
    // ──────────────────────────────────────────────

    private void handleQuestion(String question) {
        System.out.println();
        System.out.println(WHITE + "  🔎 " + question + RESET);
        System.out.println(SEP);

        // Buffer fetch results for per-route display
        java.util.ArrayList<String[]> fetchBuffer = new java.util.ArrayList<>();

        SearchCallback cb = new SearchCallback() {
            // ── Thinking (gray) ──
            @Override public void onThinking(String label) {
                System.out.println();
                System.out.println(GRAY + "  💭 " + label + RESET);
                System.out.print(GRAY + "  " + RESET);
                System.out.flush();
            }
            @Override public void onToken(String token) {
                System.out.print(GRAY + token + RESET);
                System.out.flush();
            }

            // ── Tool (cyan) ──
            @Override public void onToolCall(String name, String detail) {
                System.out.println();
                System.out.println(CYAN + "  🔧 " + name + " → " + detail + RESET);
                System.out.flush();
            }

            // ── Fetch: per-route enum display ──
            @Override public void onFetchStart(int total) {
                fetchBuffer.clear();
                System.out.println();
                System.out.println(GRAY + "  📥 Fetching " + total + " routes..." + RESET);
                System.out.flush();
            }
            @Override public void onFetchResult(String route, String status, String info) {
                fetchBuffer.add(new String[]{route, status, info});
            }

            @Override public void onFetchDone(int success, int failed, int cooldown) {
                for (String[] r : fetchBuffer) {
                    String icon = switch (r[1]) {
                        case "SUCCESS"    -> GREEN + "✓ SUCCESS " + RESET;
                        case "INTERVAL"   -> YELLOW + "⏳ COOL   " + RESET;
                        case "FAILED"     -> RED + "✗ FAILED  " + RESET;
                        case "SAVE_FAILED"-> RED + "✗ SAVE_ERR" + RESET;
                        default           -> GRAY + "? " + r[1] + RESET;
                    };
                    System.out.printf("  %s %-45s %s%n", icon, r[0], GRAY + r[2] + RESET);
                }
                System.out.printf("  " + WHITE + "%d/%d success" + RESET, success, success + failed + cooldown);
                if (failed > 0) System.out.printf("  " + RED + "%d failed" + RESET, failed);
                if (cooldown > 0) System.out.printf("  " + YELLOW + "%d cooling" + RESET, cooldown);
                System.out.println();
                System.out.flush();
            }

            // ── Response (white) ──
            @Override public void onResponseToken(String token) {
                System.out.print(WHITE + token + RESET);
                System.out.flush();
            }
            @Override public void onResponse(String text) {
                System.out.println(WHITE + "  " + text + RESET);
                System.out.flush();
            }
            @Override public void onTokens(int estimated) {
                System.out.println(GRAY + "  (~" + estimated + " tokens)" + RESET);
                System.out.flush();
            }
            @Override public void onSeparator() {
                System.out.println();
                System.out.println(SEP);
                System.out.flush();
            }
            @Override public void onError(String phase, String msg) {
                System.out.println(RED + "  ⚠ [" + phase + "] " + msg + RESET);
                System.out.flush();
            }
        };

        try {
            List<FetchResponse> results = conversationService.searchStreaming(sessionId, question, cb);
            if (results.isEmpty()) {
                System.out.println(GRAY + "  💤 Nothing found." + RESET);
            }
        } catch (Exception e) {
            System.out.println(RED + "  💥 " + e.getMessage() + RESET);
            log.error("Search failed", e);
        }
        System.out.println();
    }

    // ──────────────────────────────────────────────
    //  Commands
    // ──────────────────────────────────────────────

    private void printPlatforms() {
        if (routeCatalog.platformCount() == 0) {
            System.out.println("  No platforms available.");
            return;
        }
        System.out.println("  Platforms (" + routeCatalog.platformCount() + "):");
        for (String name : routeCatalog.getAllPlatforms()) {
            int count = routeCatalog.getRoutesForPlatform(name).size();
            System.out.printf("    %-25s (%d routes)%n", name, count);
        }
    }

    private void printRoutes(String platform) {
        var routes = routeCatalog.getRoutesForPlatform(platform);
        if (routes.isEmpty()) {
            System.out.println("  No routes for: " + platform);
            return;
        }
        System.out.println("  " + platform + ":");
        for (var r : routes) {
            System.out.printf("    %-45s — %s%n", r.path(), r.description());
        }
    }

    private void printHelp() {
        System.out.println();
        System.out.println("  RssHarness — RSS-powered Generative Search Agent");
        System.out.println("  " + SEP.replaceAll("\033\\[[0-9;]*m", ""));
        System.out.println("  <question>    Ask a question (AI selects routes, fetches, summarizes)");
        System.out.println("  /sync         Refresh route catalog from RSSHub");
        System.out.println("  /routes       List all platforms");
        System.out.println("  /routes <p>   List routes for a platform");
        System.out.println("  /new          Start a new session");
        System.out.println("  /help         Show this help");
        System.out.println("  /exit         Quit");
        System.out.println();
    }

    private void printBanner() {
        System.out.println();
        System.out.println("  ╔══════════════════════════════════════════════╗");
        System.out.println("  ║       RssHarness — RSS Search Agent            ║");
        System.out.println("  ║    AI-powered, source-traceable search       ║");
        System.out.println("  ╚══════════════════════════════════════════════╝");
        System.out.println();
    }
}
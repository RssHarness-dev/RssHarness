package com.fanexmp.rssagent.ai.tool;

import com.fanexmp.rssagent.ai.dto.AiSelections.RouteParam;
import com.fanexmp.rssagent.ai.rag.RouteCatalog;
import com.fanexmp.rssagent.dto.FetchResponse;
import com.fanexmp.rssagent.dto.FetchStatus;
import com.fanexmp.rssagent.dto.Summary;
import com.fanexmp.rssagent.rss.RssController;
import com.fanexmp.rssagent.storage.dataview.DataViewFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Spring AI tools for the LLM to call autonomously.
 */
@Component
public class RssTools {

    private static final Logger log = LoggerFactory.getLogger(RssTools.class);

    @Autowired private RssController rssController;
    @Autowired private RouteCatalog routeCatalog;
    @Autowired private DataViewFactory dataViewFactory;

    private final AtomicReference<List<FetchResponse>> lastResults = new AtomicReference<>();

    public List<FetchResponse> getLastResults() {
        List<FetchResponse> r = lastResults.getAndSet(null);
        return r != null ? r : List.of();
    }

    @Tool(description = """
            Search the RSSHub PLATFORM catalog by keyword.
            Returns matching platform names with their route counts.
            Use this to find relevant platforms, then call searchRoutes
            Use the exact platform name, or just a portion of it. It supports partial matching but does not support fuzzy or category-based matching.
            to drill down into a specific platform's routes.\
            """)
    public List<String> searchPlatforms(
            @ToolParam(description = "Search keyword, e.g. '科技' or 'news'")
            String keyword
    ) {
        log.info("Tool call: searchPlatforms '{}'", keyword);
        List<String> results = new ArrayList<>();
        for (String platform : routeCatalog.getAllPlatforms()) {
            if (platform.contains(keyword)) {
                int count = routeCatalog.getRoutesForPlatform(platform).size();
                results.add(platform + "  (" + count + " routes)");
            }
        }
        log.info("  found {} platforms", results.size());
        return results.size() > 5 ? results.subList(0, 5) : results;
    }

    @Tool(description = """
            List routes for a platform.  Params ending with ? are OPTIONAL —
            you can drop them entirely.  Params without ? are REQUIRED.
            Example: "/weibo/search/hot/:fulltext?" → call fetchRss with "/weibo/search/hot"
            Example: "/weibo/keyword/:keyword*" → call fetchRss with "/weibo/keyword/AI"\
            """)
    public List<String> listRoutes(
            @ToolParam(description = "Exact platform name from searchPlatforms, e.g. '知乎' or 'GitHub'")
            String platform
    ) {
        log.info("Tool call: listRoutes '{}'", platform);
        var routes = routeCatalog.getRoutesForPlatform(platform);
        if (routes.isEmpty()) {
            log.warn("listRoutes: no routes for platform '{}'", platform);
            return List.of("Platform not found: " + platform);
        }
        log.info("listRoutes: {} has {} routes", platform, routes.size());
        for (var r : routes) {
            log.debug("  {}", r.path());
        }
        List<String> result = new ArrayList<>();
        for (var r : routes) {
            String p = r.path();
            // Show param info: :keyword* (required) / :keyword? (optional)
            if (r.isPathParam()) {
                p += "  (";
                for (int i = 0; i < r.params().size(); i++) {
                    if (i > 0) p += ", ";
                    p += r.params().get(i);
                }
                p += ")";
            }
            result.add(p + (r.description().isBlank() ? "" : " — " + r.description()));
        }
        if (result.size() > 15) {
            List<String> trimmed = new ArrayList<>(result.subList(0, 15));
            trimmed.add("... (" + (routes.size() - 15) + " more routes, narrow your search)");
            return trimmed;
        }
        return result;
    }

    @Tool(description = """
            FETCH real-time RSS content.  MANDATORY — call after listRoutes.
            Drop OPTIONAL params (? suffix) entirely.
            Fill REQUIRED params with real values.
              listRoutes: "/weibo/search/hot/:fulltext?" → fetchRss(["/weibo/search/hot"])
              listRoutes: "/weibo/keyword/:keyword*"     → fetchRss(["/weibo/keyword/AI"])
              listRoutes: "/zhihu/hot"                   → fetchRss(["/zhihu/hot"])\
            """)
    public List<FetchResponse> fetchRss(
            @ToolParam(description = "Exact paths from listRoutes with :params filled, e.g. ['/zhihu/hot', '/weibo/search/AI']")
            List<String> routes
    ) {
        log.info("Tool call: fetchRss with {} routes", routes.size());
        for (String r : routes) log.info("  route: {}", r);
        List<FetchResponse> results = rssController.fetchRss(routes).join();
        lastResults.set(results);
        for (FetchResponse r : results) {
            log.info("  {} → {}", r.getRoute(), r.getStatus());
        }
        return results;
    }

    @Tool(description = """
            Read stored article summaries for routes that were successfully fetched.
            Call this AFTER fetchRss to get the actual content for summarisation.\
            """)
    public String readSummaries(
            @ToolParam(description = "Routes to read summaries for")
            List<String> routes
    ) {
        log.info("Tool call: readSummaries for {} routes", routes.size());
        StringBuilder sb = new StringBuilder();
        for (String route : routes) {
            List<Summary> summaries = dataViewFactory.fromRoute(route).getSummaries();
            if (summaries.isEmpty()) continue;
            sb.append("\n[").append(route).append("] (").append(summaries.size()).append(" articles)\n");
            for (int i = 0; i < Math.min(summaries.size(), 5); i++) {
                Summary s = summaries.get(i);
                sb.append("  ").append(i + 1).append(". ")
                  .append(s.getTitle()).append("\n")
                  .append("     ").append(s.getTag()).append("\n")
                  .append("     ").append(s.getUrl()).append("\n");
            }
        }
        String result = sb.isEmpty() ? "No summaries found. Run fetchRss first." : sb.toString();
        log.info("readSummaries: {} chars", result.length());
        return result;
    }
}
package com.fanexmp.rssharness.ai.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fanexmp.rssharness.storage.DataRoot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

/**
 * Syncs the RSSHub route catalog via {@code /rsshub/routes/zh}.
 * Parses raw RSS XML with DOM + XPath — no Rome {@code getUri()} corruption.
 */
@Component
public class RouteSyncTask {

    private static final Logger log = LoggerFactory.getLogger(RouteSyncTask.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    @Value("${rssharness.routes-url:http://127.0.0.1:1200/rsshub/routes/zh}")
    private String routesUrl;

    @Value("${rssharness.routes-file:./data/routes.json}")
    private Path localRoutesFile;

    @Autowired private RouteCatalog routeCatalog;
    @Autowired private DataRoot dataRoot;

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public String sync() {
        log.info("Manual sync from {}", routesUrl);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(routesUrl))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Accept", "application/rss+xml, application/xml, text/xml")
                    .GET()
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) return "HTTP " + response.statusCode();

            // Parse with DOM — no Rome, no guid corruption
            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder().parse(new ByteArrayInputStream(response.body()));
            var xpath = XPathFactory.newInstance().newXPath();
            NodeList items = (NodeList) xpath.evaluate("//item", doc, XPathConstants.NODESET);

            Map<String, Map<String, Object>> catalog = new LinkedHashMap<>();
            for (int i = 0; i < items.getLength(); i++) {
                try {
                    String title = xpath.evaluate("title/text()", items.item(i));
                    String guid  = xpath.evaluate("guid/text()",  items.item(i));
                    if (guid == null || !guid.startsWith("/") || title == null) continue;
                    guid = guid.trim();
                    // Title: "平台名 - 路由名"
                    String platform, routeName;
                    int dash = title.indexOf(" - ");
                    if (dash > 0) {
                        platform = title.substring(0, dash).trim();
                        routeName = title.substring(dash + 3).trim();
                    } else {
                        platform = title.trim();
                        routeName = title.trim();
                    }

                    Map<String, Object> meta = new LinkedHashMap<>();
                    meta.put("description", routeName);
                    List<String> params = extractParams(guid);
                    if (!params.isEmpty()) meta.put("parameters", params);

                    catalog.computeIfAbsent(platform, k -> new LinkedHashMap<>())
                            .put(guid, meta);
                } catch (Exception e) {
                    log.debug("Skip item {}: {}", i, e.getMessage());
                }
            }

            int totalRoutes = catalog.values().stream().mapToInt(Map::size).sum();
            log.info("Parsed {} platforms, {} routes", catalog.size(), totalRoutes);

            Files.createDirectories(localRoutesFile.getParent());
            mapper.writerWithDefaultPrettyPrinter().writeValue(localRoutesFile.toFile(), catalog);

            routeCatalog.loadFromJson((Map<String, Object>) (Map<?, ?>) catalog);
            dataRoot.setRoutePlatforms(routeCatalog.getAllPlatforms());
            dataRoot.setLastRouteSync(java.time.Instant.now().getEpochSecond());

            return routeCatalog.platformCount() + " platforms, " + routeCatalog.routeCount() + " routes";
        } catch (Exception e) {
            log.error("Route sync failed", e);
            return "Sync failed: " + e.getMessage();
        }
    }

    private static List<String> extractParams(String routePath) {
        List<String> params = new ArrayList<>();
        for (String seg : routePath.split("/")) {
            if (!seg.contains(":")) continue;
            boolean optional = seg.endsWith("?");
            String name = seg.replaceAll("^:", "")
                    .replaceAll("\\{.+$", "")
                    .replaceAll("\\?$", "");
            if (name.isBlank()) continue;
            params.add(optional ? name + "?" : name);
        }
        return params;
    }
}
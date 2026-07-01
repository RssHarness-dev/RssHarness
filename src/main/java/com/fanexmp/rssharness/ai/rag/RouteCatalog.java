package com.fanexmp.rssharness.ai.rag;

import com.fanexmp.rssharness.ai.dto.RouteEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * In-memory route catalog.  Load order: local file > classpath seed.
 * The local file is written by {@code /sync} so updates survive restarts.
 */
public class RouteCatalog {

    private static final Logger log = LoggerFactory.getLogger(RouteCatalog.class);
    private static final String SEED_RESOURCE = "routes.json";

    private final Map<String, RouteEntry> allRoutes = new LinkedHashMap<>();
    private final List<String> platformNames = new ArrayList<>();
    private final ObjectMapper mapper = new ObjectMapper();

    /** Try local file first, fall back to classpath seed. */
    public RouteCatalog() {
        // Path is resolved later via loadFromFile; try classpath first on construction
        loadFromClasspath();
    }

    public void init(Path localFile) {
        if (Files.exists(localFile)) {
            loadFromFile(localFile);
        }
        // else: already loaded from classpath in constructor
    }

    /** Load from a local file (written by /sync). */
    @SuppressWarnings("unchecked")
    public void loadFromFile(Path path) {
        try {
            Map<String, Object> raw = mapper.readValue(path.toFile(),
                    new TypeReference<Map<String, Object>>() {});
            loadFromJson(raw);
            log.info("Loaded catalog from {}: {} platforms, {} routes", path, platformNames.size(), allRoutes.size());
        } catch (Exception e) {
            log.warn("Failed to load catalog from {}", path, e);
        }
    }

    /** Load from the bundled classpath resource. */
    @SuppressWarnings("unchecked")
    public void loadFromClasspath() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(SEED_RESOURCE)) {
            if (in == null) {
                log.warn("Seed resource '{}' not found on classpath", SEED_RESOURCE);
                return;
            }
            Map<String, Object> raw = mapper.readValue(in, new TypeReference<Map<String, Object>>() {});
            loadFromJson(raw);
            log.info("Loaded seed catalog: {} platforms, {} routes", platformNames.size(), allRoutes.size());
        } catch (Exception e) {
            log.error("Failed to load seed catalog from {}", SEED_RESOURCE, e);
        }
    }

    /**
     * Parse the raw /routes JSON into a flat RouteEntry list.
     * Input structure: {"bilibili": {"hot-search": {"description":"...","example":"..."}, ...}, ...}
     */
    @SuppressWarnings("unchecked")
    public void loadFromJson(Map<String, Object> rawJson) {
        allRoutes.clear();
        platformNames.clear();

        for (var platformEntry : rawJson.entrySet()) {
            String namespace = platformEntry.getKey();
            Object routesObj = platformEntry.getValue();

            if (!(routesObj instanceof Map<?, ?> rawRouteMap)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> routeMap = (Map<String, Object>) rawRouteMap;

            int platformRouteCount = 0;
            for (var routeEntry : routeMap.entrySet()) {
                String routePath = routeEntry.getKey();
                Object metaObj = routeEntry.getValue();

                if (!(metaObj instanceof Map<?, ?> meta)) {
                    continue;
                }

                RouteEntry entry = parseRouteEntry(namespace, routePath, (Map<String, Object>) meta);
                if (entry != null) {
                    // routePath may be a full path like "/zhihu/hot" or a sub-path like "hot"
                    String fullPath = routePath.startsWith("/") ? routePath
                            : "/" + namespace + "/" + routePath;
                    allRoutes.put(fullPath, entry);
                    platformRouteCount++;
                }
            }

            if (platformRouteCount > 0) {
                platformNames.add(namespace);
            }
        }

        log.info("Loaded {} routes across {} platforms", allRoutes.size(), platformNames.size());
    }

    private RouteEntry parseRouteEntry(String namespace, String routePath, Map<String, Object> meta) {
        String description = (String) meta.getOrDefault("description", "");
        String example = (String) meta.getOrDefault("example", "");
        Object paramsObj = meta.get("parameters");
        Object configObj = meta.get("configRequired");
        Object puppeteerObj = meta.get("puppeteer");

        List<String> params = new ArrayList<>();
        if (paramsObj instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof String s) {
                    params.add(s);
                }
            }
        }

        boolean requiresConfig = false;
        if (configObj instanceof Boolean b) {
            requiresConfig = b;
        } else if (configObj instanceof String s) {
            requiresConfig = Boolean.parseBoolean(s);
        }

        boolean usesPuppeteer = false;
        if (puppeteerObj instanceof Boolean b) {
            usesPuppeteer = b;
        }

        return new RouteEntry(
                routePath,
                namespace,
                description.isBlank() ? namespace + " route" : description,
                example,
                params,
                requiresConfig,
                usesPuppeteer
        );
    }

    // ── Query methods ──

    public List<String> getAllPlatforms() {
        return Collections.unmodifiableList(platformNames);
    }

    public List<RouteEntry> getRoutesForPlatform(String namespace) {
        return allRoutes.values().stream()
                .filter(e -> e.namespace().equals(namespace))
                .collect(Collectors.toList());
    }

    /** All routes across the given platforms, flattened. */
    public List<RouteEntry> getRoutesForPlatforms(List<String> namespaces) {
        return allRoutes.values().stream()
                .filter(e -> namespaces.contains(e.namespace()))
                .collect(Collectors.toList());
    }

    public List<RouteEntry> getAllRoutes() {
        return List.copyOf(allRoutes.values());
    }

    public int routeCount() {
        return allRoutes.size();
    }

    public int platformCount() {
        return platformNames.size();
    }

    /**
     * Format platform list for LLM (Tier 1).
     */
    public String formatPlatformList() {
        StringBuilder sb = new StringBuilder();
        for (String name : platformNames) {
            int count = getRoutesForPlatform(name).size();
            sb.append("- ").append(name).append(" (").append(count).append(" routes)\n");
        }
        return sb.toString();
    }

    /**
     * Format routes for a specific platform (Tier 2 single-platform fallback).
     */
    public String formatRoutesForPlatform(String namespace) {
        List<RouteEntry> routes = getRoutesForPlatform(namespace);
        return formatRouteEntries(routes);
    }

    /**
     * Format all routes across the given platforms into one prompt block (Tier 2).
     */
    public String formatRoutesForPlatforms(List<String> namespaces) {
        List<RouteEntry> routes = getRoutesForPlatforms(namespaces);
        return formatRouteEntries(routes);
    }

    private String formatRouteEntries(List<RouteEntry> routes) {
        StringBuilder sb = new StringBuilder();
        for (RouteEntry r : routes) {
            sb.append("- ").append(r.path());
            if (r.isPathParam()) {
                sb.append("  [");
                for (int i = 0; i < r.params().size(); i++) {
                    if (i > 0) sb.append(", ");
                    String p = r.params().get(i);
                    if (p.endsWith("?")) {
                        sb.append(p); // optional: "keyword?"
                    } else {
                        sb.append(p).append("*"); // required: "id*"
                    }
                }
                sb.append("]");
            }
            if (!r.description().isBlank()) {
                sb.append("  (").append(r.description()).append(")");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    // ── Route validation ──

    /**
     * Validates an LLM-generated route path against the catalog.
     * Returns the canonical catalog path if matched, or the original if no match found.
     */
    public String validateRoute(String candidate) {
        // 1. Exact match
        if (allRoutes.containsKey(candidate)) {
            return candidate;
        }

        // 2. Match against parameterized templates
        // e.g. LLM gives /tieba/search/AI → matches /tieba/search/:keyword
        String platform = extractPlatform(candidate);
        List<RouteEntry> platformRoutes = getRoutesForPlatform(platform);

        for (RouteEntry r : platformRoutes) {
            if (matchesTemplate(candidate, r.path())) {
                log.debug("Route {} matched template {}", candidate, r.path());
                return candidate; // LLM filled params correctly, path is valid
            }
        }

        // 3. Fuzzy: try to correct common LLM mistakes
        // e.g. /tieba/keyword/AI → should be /tieba/search/:keyword
        for (RouteEntry r : platformRoutes) {
            String corrected = fuzzyCorrect(candidate, r);
            if (corrected != null) {
                log.warn("LLM route {} corrected to {} (template: {})", candidate, corrected, r.path());
                return corrected;
            }
        }

        log.warn("Route {} could not be matched to any catalog route for platform {}", candidate, platform);
        return null; // caller should drop this route
    }

    private String extractPlatform(String path) {
        String trimmed = path.startsWith("/") ? path.substring(1) : path;
        int slash = trimmed.indexOf('/');
        return slash > 0 ? trimmed.substring(0, slash) : trimmed;
    }

    private boolean matchesTemplate(String candidate, String template) {
        // Replace :param patterns with regex wildcards
        String regex = template.replaceAll(":[^/]+", "[^/]+");
        return candidate.matches(regex);
    }

    /**
     * Tries to detect when the LLM used a param name as a literal path segment.
     * e.g. candidate="/tieba/keyword/AI", template="/tieba/search/:keyword"
     * → LLM confused "keyword" (param name) with "search" (path segment).
     * Returns the corrected path or null.
     */
    private String fuzzyCorrect(String candidate, RouteEntry template) {
        String[] candSegs = splitPath(candidate);
        String[] tmplSegs = splitPath(template.path());

        if (candSegs.length != tmplSegs.length) return null;

        String[] result = new String[candSegs.length];
        for (int i = 0; i < candSegs.length; i++) {
            if (candSegs[i].equals(tmplSegs[i])) {
                // Exact match: keep
                result[i] = candSegs[i];
            } else if (tmplSegs[i].startsWith(":")) {
                // Template segment is a param placeholder → LLM's value is the actual fill
                result[i] = candSegs[i];
            } else if (template.params().contains(candSegs[i])) {
                // LLM used the param NAME as a literal path segment (confused)
                // Restore the correct template segment
                log.debug("Fuzzy: replacing param-name '{}' with template segment '{}' in {}",
                        candSegs[i], tmplSegs[i], candidate);
                result[i] = tmplSegs[i];
            } else {
                return null; // irreconcilable difference
            }
        }
        return "/" + String.join("/", result);
    }

    /** Split "/a/b/c" into ["a","b","c"], skipping the leading empty segment. */
    private static String[] splitPath(String path) {
        String[] parts = path.split("/");
        // First element is "" if path starts with /
        if (parts.length > 0 && parts[0].isEmpty()) {
            String[] nonEmpty = new String[parts.length - 1];
            System.arraycopy(parts, 1, nonEmpty, 0, nonEmpty.length);
            return nonEmpty;
        }
        return parts;
    }

    /**
     * Returns the RouteEntry for a given path (exact or template match), or null.
     */
    public RouteEntry findRouteEntry(String path) {
        RouteEntry exact = allRoutes.get(path);
        if (exact != null) return exact;

        String platform = extractPlatform(path);
        return getRoutesForPlatform(platform).stream()
                .filter(r -> matchesTemplate(path, r.path()))
                .findFirst()
                .orElse(null);
    }
}
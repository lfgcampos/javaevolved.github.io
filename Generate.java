///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21+
//DEPS com.fasterxml.jackson.core:jackson-databind:2.18.3

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generate HTML detail pages from JSON snippet files and slug-template.html.
 * JBang equivalent of generate.py — produces identical output.
 */
public class Generate {

    static final String BASE_URL = "https://javaevolved.github.io";
    static final String TEMPLATE_FILE = "slug-template.html";

    static final Map<String, String> CATEGORY_DISPLAY = Map.ofEntries(
            Map.entry("language", "Language"),
            Map.entry("collections", "Collections"),
            Map.entry("strings", "Strings"),
            Map.entry("streams", "Streams"),
            Map.entry("concurrency", "Concurrency"),
            Map.entry("io", "I/O"),
            Map.entry("errors", "Errors"),
            Map.entry("datetime", "Date/Time"),
            Map.entry("security", "Security"),
            Map.entry("tooling", "Tooling")
    );

    static final List<String> CATEGORIES = List.of(
            "language", "collections", "strings", "streams", "concurrency",
            "io", "errors", "datetime", "security", "tooling"
    );

    static final Set<String> EXCLUDED_KEYS = Set.of("_path", "prev", "next", "related");

    static final ObjectMapper mapper = new ObjectMapper();
    static final Pattern TOKEN_PATTERN = Pattern.compile("\\{\\{(\\w+)}}");

    public static void main(String[] args) throws IOException {
        String template = Files.readString(Path.of(TEMPLATE_FILE));
        Map<String, JsonNode> allSnippets = loadAllSnippets();
        System.out.println("Loaded " + allSnippets.size() + " snippets");

        // Generate HTML files
        for (var entry : allSnippets.entrySet()) {
            JsonNode data = entry.getValue();
            String htmlContent = generateHtml(template, data, allSnippets).strip();
            String category = data.get("category").asText();
            String slug = data.get("slug").asText();
            Path outPath = Path.of(category, slug + ".html");
            Files.writeString(outPath, htmlContent);
        }
        System.out.println("Generated " + allSnippets.size() + " HTML files");

        // Rebuild data/snippets.json
        List<Map<String, Object>> snippetsList = new ArrayList<>();
        for (var entry : allSnippets.entrySet()) {
            Map<String, Object> map = mapper.convertValue(entry.getValue(),
                    new TypeReference<LinkedHashMap<String, Object>>() {});
            EXCLUDED_KEYS.forEach(map::remove);
            snippetsList.add(map);
        }

        Files.createDirectories(Path.of("data"));
        ObjectMapper prettyMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        String json = prettyMapper.writeValueAsString(snippetsList) + "\n";
        Files.writeString(Path.of("data", "snippets.json"), json);
        System.out.println("Rebuilt data/snippets.json with " + snippetsList.size() + " entries");

        // Patch index.html with the current snippet count
        int count = allSnippets.size();
        String indexContent = Files.readString(Path.of("index.html"));
        indexContent = indexContent.replace("{{snippetCount}}", String.valueOf(count));
        Files.writeString(Path.of("index.html"), indexContent);
        System.out.println("Patched index.html with snippet count: " + count);
    }

    static Map<String, JsonNode> loadAllSnippets() throws IOException {
        Map<String, JsonNode> snippets = new LinkedHashMap<>();
        for (String cat : CATEGORIES) {
            Path catDir = Path.of(cat);
            if (!Files.isDirectory(catDir)) continue;
            List<Path> jsonFiles = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(catDir, "*.json")) {
                stream.forEach(jsonFiles::add);
            }
            jsonFiles.sort(Path::compareTo);
            for (Path path : jsonFiles) {
                JsonNode data = mapper.readTree(path.toFile());
                String key = data.get("category").asText() + "/" + data.get("slug").asText();
                snippets.put(key, data);
            }
        }
        return snippets;
    }

    static String escape(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;");
    }

    static String jsonEscape(String text) {
        // Produce ASCII-only JSON string content (matching Python json.dumps(ensure_ascii=True)[1:-1])
        try {
            String full = mapper.writeValueAsString(text); // includes surrounding quotes
            String inner = full.substring(1, full.length() - 1);
            // Jackson doesn't escape non-ASCII by default; do it manually
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < inner.length(); i++) {
                char c = inner.charAt(i);
                if (c > 127) {
                    sb.append(String.format("\\u%04x", (int) c));
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }

    static String renderNavArrows(JsonNode data) {
        List<String> parts = new ArrayList<>();
        if (data.has("prev") && !data.get("prev").isNull()) {
            parts.add("<a href=\"/" + data.get("prev").asText() + ".html\" aria-label=\"Previous pattern\">←</a>");
        } else {
            parts.add("<span class=\"nav-arrow-disabled\">←</span>");
        }
        if (data.has("next") && !data.get("next").isNull()) {
            parts.add("<a href=\"/" + data.get("next").asText() + ".html\" aria-label=\"Next pattern\">→</a>");
        } else {
            parts.add("");
        }
        return String.join("\n          ", parts);
    }

    static String renderWhyCards(JsonNode whyList) {
        List<String> cards = new ArrayList<>();
        for (JsonNode w : whyList) {
            cards.add("        <div class=\"why-card\">\n"
                    + "          <div class=\"why-icon\">" + w.get("icon").asText() + "</div>\n"
                    + "          <h3>" + escape(w.get("title").asText()) + "</h3>\n"
                    + "          <p>" + escape(w.get("desc").asText()) + "</p>\n"
                    + "        </div>");
        }
        return String.join("\n", cards);
    }

    static String renderRelatedCard(JsonNode relatedData) {
        String cat = relatedData.get("category").asText();
        String slug = relatedData.get("slug").asText();
        String catDisplay = CATEGORY_DISPLAY.get(cat);
        String path = cat + "/" + slug;
        String difficulty = relatedData.get("difficulty").asText();

        return "        <a href=\"/" + path + ".html\" class=\"tip-card\">\n"
                + "          <div class=\"tip-card-body\">\n"
                + "            <div class=\"tip-card-header\">\n"
                + "              <div class=\"tip-badges\">\n"
                + "                <span class=\"badge " + cat + "\">" + catDisplay + "</span>\n"
                + "                <span class=\"badge " + difficulty + "\">" + difficulty + "</span>\n"
                + "              </div>\n"
                + "            </div>\n"
                + "            <h3>" + escape(relatedData.get("title").asText()) + "</h3>\n"
                + "          </div>\n"
                + "          <div class=\"card-code\">\n"
                + "            <div class=\"card-code-layer old-layer\">\n"
                + "              <div class=\"mini-label\">" + escape(relatedData.get("oldLabel").asText()) + "</div>\n"
                + "              <pre class=\"code-text\">" + escape(relatedData.get("oldCode").asText()) + "</pre>\n"
                + "            </div>\n"
                + "            <div class=\"card-code-layer modern-layer\">\n"
                + "              <div class=\"mini-label\">" + escape(relatedData.get("modernLabel").asText()) + "</div>\n"
                + "              <pre class=\"code-text\">" + escape(relatedData.get("modernCode").asText()) + "</pre>\n"
                + "            </div>\n"
                + "            <span class=\"hover-hint\">Hover to see modern ➜</span>\n"
                + "          </div>\n"
                + "          <div class=\"tip-card-footer\">\n"
                + "            <span class=\"browser-support\"><span class=\"dot\"></span>JDK " + relatedData.get("jdkVersion").asText() + "+</span>\n"
                + "            <span class=\"arrow-link\">→</span>\n"
                + "          </div>\n"
                + "        </a>";
    }

    static String renderRelatedSection(JsonNode relatedPaths, Map<String, JsonNode> allSnippets) {
        List<String> cards = new ArrayList<>();
        if (relatedPaths != null) {
            for (JsonNode pathNode : relatedPaths) {
                String path = pathNode.asText();
                if (allSnippets.containsKey(path)) {
                    cards.add(renderRelatedCard(allSnippets.get(path)));
                }
            }
        }
        return String.join("\n", cards);
    }

    static String renderSocialShare(String slug, String title) {
        String pageUrl = BASE_URL + "/" + slug + ".html";
        String shareText = title + " \u2013 java.evolved";
        String encodedUrl = urlEncode(pageUrl);
        String encodedText = urlEncode(shareText);

        String xUrl = "https://x.com/intent/tweet?url=" + encodedUrl + "&text=" + encodedText;
        String bskyUrl = "https://bsky.app/intent/compose?text=" + encodedText + "%20" + encodedUrl;
        String liUrl = "https://www.linkedin.com/sharing/share-offsite/?url=" + encodedUrl;
        String redditUrl = "https://www.reddit.com/submit?url=" + encodedUrl + "&title=" + encodedText;

        return "  <div class=\"social-share\">\n"
                + "    <span class=\"share-label\">Share</span>\n"
                + "    <a href=\"" + xUrl + "\" target=\"_blank\" rel=\"noopener\" class=\"share-btn share-x\" aria-label=\"Share on X\">𝕏</a>\n"
                + "    <a href=\"" + bskyUrl + "\" target=\"_blank\" rel=\"noopener\" class=\"share-btn share-bsky\" aria-label=\"Share on Bluesky\">🦋</a>\n"
                + "    <a href=\"" + liUrl + "\" target=\"_blank\" rel=\"noopener\" class=\"share-btn share-li\" aria-label=\"Share on LinkedIn\">in</a>\n"
                + "    <a href=\"" + redditUrl + "\" target=\"_blank\" rel=\"noopener\" class=\"share-btn share-reddit\" aria-label=\"Share on Reddit\">⬡</a>\n"
                + "  </div>";
    }

    static String generateHtml(String template, JsonNode data, Map<String, JsonNode> allSnippets) {
        String cat = data.get("category").asText();
        String slug = data.get("slug").asText();
        String catDisplay = CATEGORY_DISPLAY.get(cat);
        String title = data.get("title").asText();

        Map<String, String> replacements = new HashMap<>();
        replacements.put("title", escape(title));
        replacements.put("summary", escape(data.get("summary").asText()));
        replacements.put("slug", slug);
        replacements.put("category", cat);
        replacements.put("categoryDisplay", catDisplay);
        replacements.put("difficulty", data.get("difficulty").asText());
        replacements.put("jdkVersion", data.get("jdkVersion").asText());
        replacements.put("oldLabel", escape(data.get("oldLabel").asText()));
        replacements.put("modernLabel", escape(data.get("modernLabel").asText()));
        replacements.put("oldCode", escape(data.get("oldCode").asText()));
        replacements.put("modernCode", escape(data.get("modernCode").asText()));
        replacements.put("oldApproach", escape(data.get("oldApproach").asText()));
        replacements.put("modernApproach", escape(data.get("modernApproach").asText()));
        replacements.put("explanation", escape(data.get("explanation").asText()));
        replacements.put("support", escape(data.get("support").asText()));
        replacements.put("canonicalUrl", BASE_URL + "/" + cat + "/" + slug + ".html");
        replacements.put("flatUrl", BASE_URL + "/" + slug + ".html");
        replacements.put("titleJson", jsonEscape(title));
        replacements.put("summaryJson", jsonEscape(data.get("summary").asText()));
        replacements.put("categoryDisplayJson", jsonEscape(catDisplay));
        replacements.put("navArrows", renderNavArrows(data));
        replacements.put("whyCards", renderWhyCards(data.get("whyModernWins")));
        replacements.put("relatedCards", renderRelatedSection(data.get("related"), allSnippets));
        replacements.put("socialShare", renderSocialShare(slug, title));

        Matcher m = TOKEN_PATTERN.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String key = m.group(1);
            String replacement = replacements.getOrDefault(key, m.group(0));
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}

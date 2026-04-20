package se.brpsystems.lunch;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

public class LunchScraper {

    private static final int MAX_CHARS = 8000;

    public String scrape(String url) throws Exception {
        var doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (compatible; LunchBot/1.0)")
                .timeout(15_000)
                .get();

        doc.select("script, style, nav, footer, header, iframe, noscript").remove();

        // Walk the DOM preserving block-level boundaries as newlines so the LLM
        // can see weekday headings on their own lines rather than all fused together.
        var sb = new StringBuilder();
        walkNode(doc.body(), sb);

        String text = sb.toString()
                .replaceAll("[ \t]+", " ")
                .replaceAll("(?m)^ +", "")
                .replaceAll("\n{3,}", "\n\n")
                .strip();

        return text.length() > MAX_CHARS ? text.substring(0, MAX_CHARS) : text;
    }

    private void walkNode(Node node, StringBuilder sb) {
        if (node instanceof TextNode tn) {
            String t = tn.text();
            if (!t.isBlank()) sb.append(t);
        } else if (node instanceof Element el) {
            boolean block = el.isBlock() || el.tagName().equals("br");
            if (block && !sb.isEmpty() && sb.charAt(sb.length() - 1) != '\n') {
                sb.append('\n');
            }
            for (Node child : el.childNodes()) {
                walkNode(child, sb);
            }
            if (block) sb.append('\n');
        }
    }
}

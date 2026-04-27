package se.brpsystems.lunch;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class PageGenerator {

    private static final DateTimeFormatter WEEKDAY_FMT =
            DateTimeFormatter.ofPattern("EEEE", new Locale("sv", "SE"));
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("d MMMM yyyy", new Locale("sv", "SE"));

    public String generate(List<LunchResult> results, LocalDate date) {
        return generate(results, date, "");
    }

    public String generate(List<LunchResult> results, LocalDate date, String githubRepo) {
        return generateLocationPage("", results, date, githubRepo);
    }

    public String generateIndex(Set<String> locationNames, LocalDate date, String githubRepo) {
        String weekdayTitle = formatWeekday(date);
        String fullDate = date.format(DATE_FMT);

        var html = new StringBuilder();
        appendPageStart(html, weekdayTitle, fullDate, weekdayTitle);

        html.append("    <div class=\"cards\">\n");
        for (String location : locationNames) {
            String slug = Main.toSlug(location);
            html.append("      <a class=\"location-card\" href=\"./%s/\">%s</a>\n"
                    .formatted(slug, escape(location)));
        }
        html.append("    </div>\n");

        String issueUrl = buildIssueUrl(githubRepo);
        if (!issueUrl.isEmpty()) {
            html.append("    <div style=\"text-align:center\">\n");
            html.append("      <a class=\"suggest\" href=\"%s\" target=\"_blank\" rel=\"noopener\">+ Föreslå restaurang</a>\n".formatted(issueUrl));
            html.append("    </div>\n");
        }

        appendPageEnd(html);
        return html.toString();
    }

    public String generateLocationPage(String location, List<LunchResult> results, LocalDate date, String githubRepo) {
        String weekdayTitle = formatWeekday(date);
        String fullDate = date.format(DATE_FMT);
        String title = location.isEmpty() ? weekdayTitle : escape(location);

        var html = new StringBuilder();
        appendPageStart(html, title, fullDate, weekdayTitle);

        if (!location.isEmpty()) {
            html.append("    <a class=\"back-link\" href=\"../\">← Alla platser</a>\n");
        }

        html.append("    <div class=\"cards\">\n");
        for (LunchResult result : results) {
            appendCard(html, result);
        }
        html.append("    </div>\n");

        String issueUrl = buildIssueUrl(githubRepo);
        if (!issueUrl.isEmpty()) {
            html.append("    <div style=\"text-align:center\">\n");
            html.append("      <a class=\"suggest\" href=\"%s\" target=\"_blank\" rel=\"noopener\">+ Föreslå restaurang</a>\n".formatted(issueUrl));
            html.append("    </div>\n");
        }

        appendPageEnd(html);
        return html.toString();
    }

    private void appendPageStart(StringBuilder html, String title, String fullDate, String weekdayTitle) {
        html.append("""
                <!DOCTYPE html>
                <html lang="sv">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Lunch – %s</title>
                  <link rel="icon" type="image/svg+xml" href="%s">
                  <style>
                    *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
                    body {
                      font-family: system-ui, -apple-system, sans-serif;
                      background: #f4f4f2;
                      color: #111;
                      padding: 1.5rem 1rem 3rem;
                    }
                    .wrap { max-width: 640px; margin: 0 auto; }
                    header { margin-bottom: 1.25rem; }
                    h1 { font-size: 1.6rem; font-weight: 800; letter-spacing: -0.02em; }
                    .sub { color: #aaa; font-size: 0.8rem; margin-top: 0.2rem; text-transform: capitalize; }
                    .back-link {
                      display: inline-block;
                      margin-bottom: 1rem;
                      font-size: 0.82rem;
                      color: #888;
                      text-decoration: none;
                    }
                    .back-link:hover { color: #111; }
                    .cards { display: grid; gap: 0.75rem; }
                    @media (min-width: 520px) { .cards { grid-template-columns: 1fr 1fr; } }
                    .card {
                      background: #fff;
                      border-radius: 12px;
                      overflow: hidden;
                      box-shadow: 0 1px 3px rgba(0,0,0,.08), 0 0 0 1px rgba(0,0,0,.04);
                    }
                    .card-header {
                      padding: 0.6rem 0.9rem;
                      border-bottom: 1px solid #f0f0ee;
                      background: #fafaf9;
                    }
                    .name { font-weight: 700; font-size: 0.85rem; }
                    .name a { color: #111; text-decoration: none; }
                    .name a:hover { text-decoration: underline; }
                    .card-body { padding: 0.65rem 0.9rem; }
                    ul { list-style: none; }
                    li {
                      font-size: 0.875rem;
                      color: #333;
                      padding: 0.3rem 0;
                      border-bottom: 1px solid #f4f4f2;
                      line-height: 1.35;
                    }
                    li:last-child { border-bottom: none; }
                    .empty { font-size: 0.82rem; color: #bbb; font-style: italic; }
                    .error { font-size: 0.82rem; color: #c00; }
                    .location-card {
                      display: flex;
                      align-items: center;
                      justify-content: center;
                      padding: 1.5rem 1rem;
                      background: #fff;
                      border-radius: 12px;
                      box-shadow: 0 1px 3px rgba(0,0,0,.08), 0 0 0 1px rgba(0,0,0,.04);
                      font-weight: 700;
                      font-size: 1rem;
                      color: #111;
                      text-decoration: none;
                      text-align: center;
                      transition: box-shadow .15s, transform .1s;
                    }
                    .location-card:hover {
                      box-shadow: 0 4px 12px rgba(0,0,0,.12), 0 0 0 1px rgba(0,0,0,.06);
                      transform: translateY(-1px);
                    }
                    footer { margin-top: 2rem; font-size: 0.72rem; color: #ccc; text-align: center; }
                    .suggest {
                      display: inline-block;
                      margin-top: 1.25rem;
                      padding: 0.5rem 1rem;
                      background: #fff;
                      border: 1px solid #ddd;
                      border-radius: 8px;
                      font-size: 0.82rem;
                      color: #555;
                      text-decoration: none;
                      transition: border-color .15s, color .15s;
                    }
                    .suggest:hover { border-color: #aaa; color: #111; }
                  </style>
                </head>
                <body>
                  <div class="wrap">
                    <header>
                      <h1>%s</h1>
                      <div class="sub">%s</div>
                    </header>
                """.formatted(weekdayTitle, faviconDataUri(), title, fullDate));
    }

    private void appendPageEnd(StringBuilder html) {
        html.append("""
                    <footer>Uppdaterad automatiskt · GitHub Actions + Ollama</footer>
                  </div>
                </body>
                </html>
                """);
    }

    private void appendCard(StringBuilder html, LunchResult result) {
        html.append("      <div class=\"card\">\n");
        html.append("        <div class=\"card-header\"><div class=\"name\"><a href=\"%s\" target=\"_blank\" rel=\"noopener\">%s</a></div></div>\n"
                .formatted(escape(result.restaurant().url()), escape(result.restaurant().name())));
        html.append("        <div class=\"card-body\">\n");

        if (!result.success()) {
            html.append("          <div class=\"error\">%s</div>\n".formatted(escape(result.error())));
        } else if (isNoMenu(result.menu())) {
            html.append("          <div class=\"empty\">Ingen lunch hittad</div>\n");
        } else {
            html.append("          <ul>\n");
            menuLines(result.menu())
                    .forEach(line -> html.append("            <li>%s</li>\n".formatted(escape(line))));
            html.append("          </ul>\n");
        }

        html.append("        </div>\n");
        html.append("      </div>\n");
    }

    private String formatWeekday(LocalDate date) {
        String weekday = date.format(WEEKDAY_FMT);
        return weekday.substring(0, 1).toUpperCase() + weekday.substring(1);
    }

    private List<String> menuLines(String menu) {
        List<String> lines = Arrays.stream(menu.split("\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toList());
        // Strip stray "ingen lunch" / "no lunch menu" lines when real dish lines exist alongside them
        boolean hasRealContent = lines.stream().anyMatch(l -> {
            String ll = l.toLowerCase();
            return !ll.contains("ingen lunch") && !ll.contains("no lunch menu");
        });
        if (hasRealContent) {
            lines.removeIf(l -> {
                String ll = l.toLowerCase();
                return ll.contains("ingen lunch") || ll.contains("no lunch menu");
            });
        }
        return lines;
    }

    private boolean isNoMenu(String menu) {
        List<String> lines = menuLines(menu);
        // Only "no menu" if there are no real dish lines alongside the phrase
        boolean hasRealContent = lines.stream().anyMatch(l -> {
            String ll = l.toLowerCase();
            return !ll.contains("ingen lunch") && !ll.contains("no lunch menu");
        });
        if (hasRealContent) return false;
        return lines.stream().anyMatch(l -> {
            String ll = l.toLowerCase();
            return ll.contains("ingen lunch") || ll.contains("no lunch menu");
        });
    }

    private String faviconDataUri() {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64">
                  <rect width="64" height="64" rx="14" fill="#ea580c"/>
                  <path d="M20 27 Q17 21 20 16 Q23 11 20 6"
                        fill="none" stroke="white" stroke-width="3" stroke-linecap="round"/>
                  <path d="M32 27 Q29 21 32 16 Q35 11 32 6"
                        fill="none" stroke="white" stroke-width="3" stroke-linecap="round"/>
                  <path d="M44 27 Q41 21 44 16 Q47 11 44 6"
                        fill="none" stroke="white" stroke-width="3" stroke-linecap="round"/>
                  <path d="M9 33 Q9 55 32 55 Q55 55 55 33 Z" fill="white"/>
                  <rect x="7" y="30" width="50" height="5" rx="2.5" fill="white"/>
                </svg>
                """;
        return "data:image/svg+xml;base64," +
                Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
    }

    private String buildIssueUrl(String githubRepo) {
        if (githubRepo == null || githubRepo.isBlank()) return "";
        String title = URLEncoder.encode("Restaurangförslag: ", StandardCharsets.UTF_8);
        String body  = URLEncoder.encode(
                "**Restaurang:** \n**Lunch-URL:** \n**Varför ska den vara med?** \n",
                StandardCharsets.UTF_8);
        return "https://github.com/" + githubRepo + "/issues/new?title=" + title + "&body=" + body;
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}

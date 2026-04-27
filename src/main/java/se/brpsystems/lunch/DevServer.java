package se.brpsystems.lunch;

import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

public class DevServer {

    private static final int PORT = 8080;
    private static final Path OUTPUT_DIR = Path.of("output");

    public static void main(String[] args) throws Exception {
        if (!Files.exists(OUTPUT_DIR.resolve("index.html"))) {
            System.out.println("No output yet. Generate it first:");
            System.out.println("  LUNCH_MOCK=true java -cp target/lunch-1.0-SNAPSHOT-jar-with-dependencies.jar se.brpsystems.lunch.Main");
            System.out.println();
        }

        var server = HttpServer.create(new InetSocketAddress(PORT), 0);

        server.createContext("/", exchange -> {
            try {
                String uriPath = exchange.getRequestURI().getPath();
                if (uriPath.equals("/") || uriPath.endsWith("/")) uriPath = uriPath + "index.html";

                Path file = OUTPUT_DIR.resolve(uriPath.substring(1)).normalize().toAbsolutePath();

                // Prevent path traversal outside output dir
                if (!file.startsWith(OUTPUT_DIR.toAbsolutePath()) || Files.isDirectory(file)) {
                    exchange.sendResponseHeaders(404, -1);
                    exchange.close();
                    return;
                }

                if (!Files.exists(file)) {
                    byte[] body = "404 Not Found".getBytes();
                    exchange.sendResponseHeaders(404, body.length);
                    exchange.getResponseBody().write(body);
                    exchange.close();
                    return;
                }

                byte[] bytes = Files.readAllBytes(file);
                exchange.getResponseHeaders().set("Content-Type", contentType(file));
                exchange.getResponseHeaders().set("Cache-Control", "no-cache");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            } finally {
                exchange.close();
            }
        });

        server.start();
        System.out.println("Dev server: http://localhost:" + PORT);
        System.out.println("Refresh the browser after re-running the generator to see updates.");
        System.out.println("Press Ctrl+C to stop.");
    }

    private static String contentType(Path file) {
        String name = file.getFileName().toString();
        if (name.endsWith(".html")) return "text/html; charset=utf-8";
        if (name.endsWith(".css"))  return "text/css; charset=utf-8";
        if (name.endsWith(".js"))   return "application/javascript";
        if (name.endsWith(".png"))  return "image/png";
        if (name.endsWith(".ico"))  return "image/x-icon";
        return "application/octet-stream";
    }
}

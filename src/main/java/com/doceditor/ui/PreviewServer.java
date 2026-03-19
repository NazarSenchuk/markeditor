package com.doceditor.ui;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class PreviewServer {
    private static final Logger log = LoggerFactory.getLogger(PreviewServer.class);
    private HttpServer server;
    private int port;
    private String currentHtml;
    private final Map<String, byte[]> staticAssets = new HashMap<>();

    public PreviewServer() {
        loadStaticAssets();
    }

    private void loadStaticAssets() {
        String msg = "<html><body style='background:#1e1e2e; color:#cdd6f4; font-family:sans-serif; display:flex; justify-content:center; align-items:center; height:100vh;'><div>Loading...</div></body></html>";
        staticAssets.put("/", msg.getBytes(StandardCharsets.UTF_8));
        loadAsset("/css/preview.css", "/css/preview.css");
    }

    private void loadAsset(String webPath, String resPath) {
        try (InputStream is = getClass().getResourceAsStream(resPath)) {
            if (is != null) staticAssets.put(webPath, is.readAllBytes());
        } catch (IOException e) {
            log.error("Failed to load asset {}", resPath, e);
        }
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
        server.createContext("/", this::handleStatic);
        server.setExecutor(null);
        server.start();
    }

    public void stop() {
        if (server != null) server.stop(0);
    }

    public int getPort() { return port; }

    public void setCurrentHtml(String html) { this.currentHtml = html; }

    private void handleStatic(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        byte[] content = (path.equals("/") || path.isEmpty()) && currentHtml != null
                ? currentHtml.getBytes(StandardCharsets.UTF_8)
                : staticAssets.get(path);

        if (content == null && path.equals("/")) content = staticAssets.get("/");

        if (content != null) {
            exchange.getResponseHeaders().set("Content-Type", getContentType(path));
            exchange.sendResponseHeaders(200, content.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(content); }
        } else {
            exchange.sendResponseHeaders(404, -1);
        }
    }

    private String getContentType(String path) {
        if (path.endsWith(".html") || path.equals("/")) return "text/html";
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".js")) return "application/javascript";
        return "text/plain";
    }
}

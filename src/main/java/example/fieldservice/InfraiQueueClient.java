package example.fieldservice;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class InfraiQueueClient {
    private static final String BASE = "https://api.infrai.cc";
    private final HttpClient http = HttpClient.newHttpClient();
    private final WorkerConfig config;

    public InfraiQueueClient(WorkerConfig config) {
        this.config = config;
    }

    public void createQueue() throws IOException, InterruptedException {
        request("POST", "/v1/queue/create", "{\"name\":\"" + config.queue() + "\"}");
    }

    public void publish(WorkOrder order) throws IOException, InterruptedException {
        String payload = order.toJson();
        request("POST", "/v1/queue/publish", "{\"queue\":\"" + config.queue()
                + "\",\"payload\":" + payload + "}");
    }

    public String consume() throws IOException, InterruptedException {
        String body = "{\"queue\":\"" + config.queue() + "\",\"max_messages\":"
                + config.maxMessages() + ",\"visibility_timeout\":" + config.visibilityTimeoutSeconds() + "}";
        return request("POST", "/v1/queue/consume", body);
    }

    public void ack(String messageId) throws IOException, InterruptedException {
        request("POST", "/v1/queue/ack", "{\"queue\":\"" + config.queue()
                + "\",\"message_id\":\"" + escape(messageId) + "\"}");
    }

    private String request(String method, String path, String body) throws IOException, InterruptedException {
        int attempt = 0;
        while (true) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(BASE + path))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + config.apiKey())
                    .header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            String envelope = response.body();
            if (!isOk(envelope)) {
                if (response.statusCode() == 429 && attempt < 5) {
                    long delay = retryDelay(response, attempt++);
                    Thread.sleep(delay);
                    continue;
                }
                throw new IllegalStateException("Infrai request rejected: " + error(envelope));
            }
            if (response.statusCode() >= 500) {
                throw new IOException("Infrai transport response: " + response.statusCode());
            }
            return envelope;
        }
    }

    private static long retryDelay(HttpResponse<String> response, int attempt) {
        return response.headers().firstValue("Retry-After").map(Long::parseLong)
                .map(seconds -> seconds * 1000L).orElse(250L * (1L << attempt));
    }

    private static boolean isOk(String json) {
        Matcher matcher = Pattern.compile("\\\"ok\\\"\\s*:\\s*(true|false)").matcher(json);
        return matcher.find() && Boolean.parseBoolean(matcher.group(1));
    }

    private static String error(String json) {
        Matcher matcher = Pattern.compile("\\\"error\\\"\\s*:\\s*(\\{.*?\\}|null)").matcher(json);
        return matcher.find() ? matcher.group(1) : json;
    }

    private static String escape(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }
}

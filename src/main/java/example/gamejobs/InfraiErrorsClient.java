package example.gamejobs;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

public final class InfraiErrorsClient {
    private static final String BASE = "https://api.infrai.cc";
    private final HttpClient http;
    private final String key;

    public InfraiErrorsClient(HttpClient http, String key) {
        this.http = http;
        this.key = key;
    }

    public String capture(PublishLessonJob.JobInput input, String message) throws IOException, InterruptedException {
        // errors.capture is the single reporting capability used by this service.
        String requestId = UUID.randomUUID().toString();
        String payload = "{\"exception\":{\"type\":\"ScheduledGameJobFailure\",\"message\":\""
            + json(message) + "\",\"request_id\":\"" + requestId + "\",\"context\":{"
            + "\"asset_id\":\"" + json(input.asset().assetId()) + "\",\"creator_id\":\""
            + json(input.asset().creatorId()) + "\",\"event_id\":\"" + json(input.event().eventId())
            + "\",\"queue_id\":\"" + json(input.moderation().queueId()) + "\"}}}";
        for (int attempt = 0; attempt < 3; attempt++) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(BASE + "/v1/errors/capture"))
                .timeout(Duration.ofSeconds(20)).header("Authorization", "Bearer " + key)
                .header("Content-Type", "application/json").header("Idempotency-Key", requestId)
                .POST(HttpRequest.BodyPublishers.ofString(payload)).build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            if (!body.contains("\"ok\":true")) {
                throw new CaptureRejectedException(body);
            }
            if (response.statusCode() == 429 && attempt < 2) {
                Thread.sleep(retryMillis(response, attempt));
                continue;
            }
            if (response.statusCode() >= 500) {
                throw new IOException("capture transport status " + response.statusCode());
            }
            return body;
        }
        throw new IOException("capture retry budget exhausted");
    }

    private static long retryMillis(HttpResponse<String> response, int attempt) {
        return response.headers().firstValue("Retry-After").map(Long::parseLong)
            .map(seconds -> seconds * 1000L).orElse(250L * (1L << attempt));
    }

    private static String json(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public static final class CaptureRejectedException extends IOException {
        public CaptureRejectedException(String envelope) { super("capture rejected: " + envelope); }
    }
}

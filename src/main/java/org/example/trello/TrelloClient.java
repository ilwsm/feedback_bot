package org.example.trello;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import java.io.IOException;
import java.time.Duration;

public class TrelloClient {
    private static final String BASE = "https://api.trello.com/1";
    private final OkHttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String key;
    private final String token;
    private final String listId;

    public TrelloClient(String key, String token, String listId) {
        this.key = key;
        this.token = token;
        this.listId = listId;
        this.http = new OkHttpClient.Builder()
                .callTimeout(Duration.ofSeconds(30))
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(20))
                .build();
    }

    public String createCard(String name, String desc) throws IOException, InterruptedException {
        RequestBody form = new FormBody.Builder()
                .add("idList", listId)
                .add("name", name)
                .add("desc", desc)
                .add("key", key)
                .add("token", token)
                .build();

        Request req = new Request.Builder()
                .url(BASE + "/cards")
                .post(form)
                .build();

        int tries = 0;
        while (true) {
            try (Response resp = http.newCall(req).execute()) {
                if (resp.isSuccessful()) {
                    assert resp.body() != null;
                    String body = resp.body().string();
                    JsonNode node = mapper.readTree(body);
                    return node.path("shortUrl").asText(null);
                } else if (resp.code() == 429 && tries < 5) {
                    handleRateLimit(resp);
                    tries++;
                } else {
                    String err = resp.body() != null ? resp.body().string() : resp.message();
                    throw new IOException("Failed to create card: " + resp.code() + " -> " + err);
                }
            }
        }
    }

    private void handleRateLimit(Response resp) throws InterruptedException {
        String retryAfter = resp.header("Retry-After");
        long waitMs = 1000;
        if (retryAfter != null) {
            try {
                waitMs = Long.parseLong(retryAfter) * 1000L;
            } catch (NumberFormatException ignored) {}
        }
        Thread.sleep(waitMs);
    }
}
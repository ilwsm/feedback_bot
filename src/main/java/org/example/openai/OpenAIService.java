// OpenAIService.java
package org.example.openai;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;

public class OpenAIService {
    private final OpenAIClient client;

    public OpenAIService(String apiKey) {
        this.client = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }

    public OpenAIClient client() {
        return client;
    }
}

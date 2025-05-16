package org.example;
import okhttp3.*;

import java.io.IOException;
import java.util.List;

public class OpenAIService {
    private final OkHttpClient client = new OkHttpClient();
    private final String apiKey;

    public OpenAIService(String apiKey) {
        this.apiKey = apiKey;
    }

    public String summarize(List<String> messages) {
        String joined = String.join("\n", messages);
        String prompt = "Summarize the following Slack channel discussion:\n\n" + joined;

        String requestBody = "{ \"model\": \"gpt-4\", \"messages\": [{\"role\": \"user\", \"content\": \"" + prompt.replace("\"", "'") + "\"}] }";

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"),
                requestBody
        );

        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .post(body)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.body() != null) {
                return response.body().string();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "Summary failed.";
    }
}


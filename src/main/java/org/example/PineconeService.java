package org.example;
import okhttp3.*;

import java.io.IOException;
import java.util.List;

public class PineconeService {
    private final OkHttpClient client = new OkHttpClient();
    private final String apiKey;
    private final String indexName;
    private final String environment;

    public PineconeService(Config.PineconeConfig config) {
        this.apiKey = config.apiKey;
        this.indexName = config.indexName;
        this.environment = config.environment;
    }

    public void storeMessages(String channelId, List<String> messages) {
        for (int i = 0; i < messages.size(); i++) {
            String payload = "{ \"vectors\": [ { \"id\": \"" + channelId + "-" + i + "\", \"values\": [0.1,0.2,0.3], \"metadata\": {\"text\": \"" + messages.get(i).replace("\"", "'") + "\"} } ] }";

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"),
                    payload
            );

            Request request = new Request.Builder()
                    .url("https://" + indexName + "-" + environment + ".svc.pinecone.io/vectors/upsert")
                    .post(body)
                    .addHeader("Api-Key", apiKey)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    System.err.println("Pinecone insert failed: " + response);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}


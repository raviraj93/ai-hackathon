package org.example;
import okhttp3.*;
import com.fasterxml.jackson.databind.*;

import java.io.IOException;
import java.util.List;

public class McpSlackClient {
    private final OkHttpClient client = new OkHttpClient();
    private final String baseUrl;
    private final ObjectMapper mapper = new ObjectMapper();

    public McpSlackClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public List<IncidentChannel> getIncidentChannels() {
        Request request = new Request.Builder()
                .url(baseUrl + "/slack/channels")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String json = response.body().string();
                return mapper.readValue(json,
                        mapper.getTypeFactory().constructCollectionType(List.class, IncidentChannel.class));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return List.of();
    }

    public List<SlackMessage> getMessagesForChannel(String channelId) {
        Request request = new Request.Builder()
                .url(baseUrl + "/slack/channels/" + channelId + "/messages")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String json = response.body().string();
                return mapper.readValue(json,
                        mapper.getTypeFactory().constructCollectionType(List.class, SlackMessage.class));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return List.of();
    }
}


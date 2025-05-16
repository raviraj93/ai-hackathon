package org.example;

import java.util.List;

public class Main {
    public static void main(String[] args) {

        ConfigLoader loader = new ConfigLoader();
        Config config = loader.getConfig();

        SlackService slack = new SlackService(config.slack.botToken);
        PineconeService pinecone = new PineconeService(config.pinecone);
        OpenAIService openai = new OpenAIService(config.openai.apiKey);

        List<String> incidentChannels = slack.getIncidentChannels();

        for (String channelId : incidentChannels) {
            List<String> messages = slack.getChannelMessages(channelId);
            List<String> users = slack.getChannelMembers(channelId);

            pinecone.storeMessages(channelId, messages);
            String summary = openai.summarize(messages);

            System.out.println("Channel Summary for " + channelId + ":\n" + summary);
        }
    }}
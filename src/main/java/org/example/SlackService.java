package org.example;
import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.conversations.ConversationsListResponse;
import com.slack.api.methods.response.conversations.ConversationsHistoryResponse;
import com.slack.api.methods.response.conversations.ConversationsMembersResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SlackService {
    private final Slack slack = Slack.getInstance();
    private final String token;

    public SlackService(String botToken) {
        this.token = botToken;
    }

    public List<String> getIncidentChannels() {
        List<String> channelIds = new ArrayList<>();
        try {
            ConversationsListResponse response = slack.methods(token).conversationsList(r -> r);
            response.getChannels().forEach(channel -> {
                if (channel.getName().startsWith("incident")) {
                    channelIds.add(channel.getId());
                }
            });
        } catch (IOException | SlackApiException e) {
            e.printStackTrace();
        }
        return channelIds;
    }

    public List<String> getChannelMessages(String channelId) {
        List<String> messages = new ArrayList<>();
        try {
            ConversationsHistoryResponse response = slack.methods(token).conversationsHistory(r -> r.channel(channelId));
            response.getMessages().forEach(msg -> messages.add(msg.getText()));
        } catch (IOException | SlackApiException e) {
            e.printStackTrace();
        }
        return messages;
    }

    public List<String> getChannelMembers(String channelId) {
        List<String> users = new ArrayList<>();
        try {
            ConversationsMembersResponse response = slack.methods(token).conversationsMembers(r -> r.channel(channelId));
            users.addAll(response.getMembers());
        } catch (IOException | SlackApiException e) {
            e.printStackTrace();
        }
        return users;
    }
}



package org.example;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.spec.McpSchema;

import java.time.Duration;

public class McpSlackClientApp {
    public static void main(String[] args) throws Exception {
        // Configure MCP client for Slack server
        ServerParameters params = ServerParameters.builder("docker")
                .args("run", "--rm",
                        "-e", "SLACK_BOT_TOKEN=xoxb-your-token",
                        "-e", "SLACK_SIGNING_SECRET=your-signing-secret",
                        "-e", "SLACK_APP_TOKEN=xapp-your-app-token",
                        "-e", "SLACK_TEAM_ID=T12345678",
                        "mcp-slack:latest")
                .build();

        try (McpSyncClient client = McpClient.sync(new StdioClientTransport(params))
                .requestTimeout(Duration.ofSeconds(30))
                .build()) {

            client.initialize();

            // 1. List all Slack channels
            McpSchema.ListResourcesResult channels = client.listResources();

            System.out.println("## Available Channels");
            channels.resources().forEach(channel -> {
                System.out.printf("- %s (%s)%n",
                        channel.name(),
                        channel.uri().replace("slack://channel/", ""));
            });

            // 2. Get messages for first channel
            if (!channels.resources().isEmpty()) {
                McpSchema.Resource firstChannel = channels.resources().get(0);

                McpSchema.ReadResourceResult messages = client.readResource(firstChannel);

                System.out.printf("%n## Recent Messages in %s%n", firstChannel.name());
                messages.contents().forEach(msg ->
                        System.out.println(msg.toString()));
            }
        }
    }
}

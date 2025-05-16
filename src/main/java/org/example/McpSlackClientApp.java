package org.example;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

public class McpSlackClientApp {
    public static void main(String[] args) throws Exception {
        // Build ServerParameters for stdio transport using Docker
        ServerParameters params = ServerParameters.builder("docker")
                .args("run", "--rm",
                        "-e", "SLACK_BOT_TOKEN=xoxb-your-token",
                        "-e", "SLACK_SIGNING_SECRET=your-signing-secret",
                        "-e", "SLACK_APP_TOKEN=xapp-your-app-token",
                        "-e", "SLACK_TEAM_ID=T12345678",
                        "mcp-slack:latest")
                .build();

        // Create transport using stdio client transport
        McpClientTransport transport = new StdioClientTransport(params);

        // Build and initialize MCP sync client
        McpSyncClient client = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(20))
                .build();

        client.initialize();

        // List ALL resources from the Slack MCP server
        McpSchema.ListResourcesResult resourcesResult = client.listResources();
        List<McpSchema.Resource> resources = resourcesResult.resources();

        for (McpSchema.Resource res : resources) {
            String uri = res.uri();
            // Slack channel URIs usually look like: slack://channel/{id}
            if (uri.startsWith("slack://channel/") && res.name() != null && res.name().startsWith("incident")) {
                System.out.println("\nChannel: " + res.name() + " (" + uri + ")");

                // Read messages for this channel
                McpSchema.ReadResourceResult readResult = client.readResource(res);
                List<Object> messages = Arrays.asList(readResult.contents().toArray());

                // Save raw JSON of messages
                saveMessagesToFile(res.name(), messages);

                // Print first few messages (if objects have text field)
                messages.stream().limit(10).forEach(System.out::println);
            }
        }

        // Close connection gracefully
        client.closeGracefully();
    }

    private static void saveMessagesToFile(String channelName, List<Object> messages) {
        String filename = channelName.replaceAll("[^a-zA-Z0-9_-]", "_") + "_messages.json";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (Object msg : messages) {
                writer.write(msg.toString());
                writer.newLine();
            }
            System.out.println("Saved messages to " + filename);
        } catch (IOException e) {
            System.err.println("Failed to write messages to file: " + filename);
            e.printStackTrace();
        }
    }
}

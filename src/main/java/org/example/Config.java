package org.example;

public class Config {
    public SlackConfig slack;
    public PineconeConfig pinecone;
    public OpenAIConfig openai;

    public static class SlackConfig {
        public String botToken;
    }

    public static class PineconeConfig {
        public String apiKey;
        public String environment;
        public String indexName;
    }

    public static class OpenAIConfig {
        public String apiKey;
    }
}

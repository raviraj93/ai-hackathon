package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.InputStream;

public class ConfigLoader {
    private final Config config;

    public ConfigLoader() {
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            InputStream is = getClass().getClassLoader().getResourceAsStream("config.yml");
            config = mapper.readValue(is, Config.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load config.yml", e);
        }
    }

    public Config getConfig() {
        return config;
    }
}

package com.miniclaw.config;

import org.yaml.snakeyaml.Yaml;
import java.io.InputStream;
import java.util.Map;

public class ConfigManager {
    private static ConfigManager instance;
    private Map<String, Object> configMap;

    private ConfigManager() {
        Yaml yaml = new Yaml();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("application.yml")) {
            if (inputStream != null) {
                configMap = yaml.load(inputStream);
            } else {
                throw new RuntimeException("application.yml not found in classpath");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    @SuppressWarnings("unchecked")
    public String getProperty(String path) {
        String[] keys = path.split("\\.");
        Map<String, Object> currentMap = configMap;
        for (int i = 0; i < keys.length - 1; i++) {
            currentMap = (Map<String, Object>) currentMap.get(keys[i]);
            if (currentMap == null) return null;
        }
        return String.valueOf(currentMap.get(keys[keys.length - 1]));
    }
}

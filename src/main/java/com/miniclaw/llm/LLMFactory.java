package com.miniclaw.llm;

import com.miniclaw.config.ConfigManager;

public class LLMFactory {
    public static LLMProvider createProvider() {
        String providerName = ConfigManager.getInstance().getProperty("llm.provider");
        if ("deepseek".equalsIgnoreCase(providerName)) {
            return new DeepSeekProvider();
        } else if ("ollama".equalsIgnoreCase(providerName)) {
            return new OllamaProvider();
        }
        // Fallback or other providers
        return new OllamaProvider();
    }
}

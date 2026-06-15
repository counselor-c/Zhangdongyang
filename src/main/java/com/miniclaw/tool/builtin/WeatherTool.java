package com.miniclaw.tool.builtin;

import com.miniclaw.tool.Tool;

public class WeatherTool implements Tool {

    @Override
    public String getName() {
        return "WeatherTool";
    }

    @Override
    public String getDescription() {
        return "Query the weather of a specific city.";
    }

    @Override
    public String getParametersSchema() {
        return "{\n" +
               "  \"type\": \"object\",\n" +
               "  \"properties\": {\n" +
               "    \"city\": {\n" +
               "      \"type\": \"string\",\n" +
               "      \"description\": \"The name of the city\"\n" +
               "    }\n" +
               "  },\n" +
               "  \"required\": [\"city\"]\n" +
               "}";
    }

    @Override
    public String execute(String arguments) {
        // Mock data
        return "{\"city\": \"Beijing\", \"weather\": \"Sunny\", \"temperature\": \"25°C\"}";
    }
}

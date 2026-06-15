package com.miniclaw.tool.builtin;

import com.miniclaw.tool.Tool;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.FileWriter;
import java.io.IOException;

public class WriteFileTool implements Tool {

    @Override
    public String getName() {
        return "WriteFileTool";
    }

    @Override
    public String getDescription() {
        return "Write content to a specific text file.";
    }

    @Override
    public String getParametersSchema() {
        return "{\n" +
               "  \"type\": \"object\",\n" +
               "  \"properties\": {\n" +
               "    \"filename\": {\n" +
               "      \"type\": \"string\",\n" +
               "      \"description\": \"The name of the file to write to\"\n" +
               "    },\n" +
               "    \"content\": {\n" +
               "      \"type\": \"string\",\n" +
               "      \"description\": \"The content to write into the file\"\n" +
               "    }\n" +
               "  },\n" +
               "  \"required\": [\"filename\", \"content\"]\n" +
               "}";
    }

    @Override
    public String execute(String arguments) {
        try {
            Gson gson = new Gson();
            JsonObject args = gson.fromJson(arguments, JsonObject.class);
            String filename = args.get("filename").getAsString();
            String content = args.get("content").getAsString();

            try (FileWriter writer = new FileWriter(filename)) {
                writer.write(content);
            }
            return "{\"status\": \"success\", \"message\": \"File written successfully\"}";
        } catch (Exception e) {
            return "{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}";
        }
    }
}

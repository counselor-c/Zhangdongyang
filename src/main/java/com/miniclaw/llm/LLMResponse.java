package com.miniclaw.llm;

public class LLMResponse {
    private String content;
    private boolean isToolCall;
    private String toolName;
    private String toolArguments;

    public LLMResponse(String content) {
        this.content = content;
        this.isToolCall = false;
    }

    public LLMResponse(String toolName, String toolArguments) {
        this.isToolCall = true;
        this.toolName = toolName;
        this.toolArguments = toolArguments;
    }

    public String getContent() { return content; }
    public boolean isToolCall() { return isToolCall; }
    public String getToolName() { return toolName; }
    public String getToolArguments() { return toolArguments; }
}

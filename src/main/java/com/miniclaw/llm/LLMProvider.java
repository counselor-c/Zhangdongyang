package com.miniclaw.llm;

import com.miniclaw.context.Message;
import com.miniclaw.tool.Tool;

import java.util.List;
import java.util.function.Consumer;

public interface LLMProvider {
    // Basic chat abstraction
    String chat(String prompt);

    // Chat with tool support and SSE streaming output
    LLMResponse chatWithTools(List<Message> context, List<Tool> availableTools, Consumer<String> streamCallback);
}

package com.miniclaw.task;

import com.miniclaw.context.ConversationContext;
import com.miniclaw.context.Message;
import com.miniclaw.llm.LLMFactory;
import com.miniclaw.llm.LLMProvider;
import com.miniclaw.llm.LLMResponse;
import com.miniclaw.tool.Tool;
import com.miniclaw.tool.ToolRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class TaskManager {
    private static final TaskManager instance = new TaskManager();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Map<String, Task> tasks = new ConcurrentHashMap<>();
    private final ConversationContext context = new ConversationContext();
    private final LLMProvider llmProvider = LLMFactory.createProvider();

    private TaskManager() {
        context.addMessage(new Message("system", "你是 DeepSeek，由深度求索公司开发的 AI 助手。请始终以 DeepSeek 的身份回答问题，不要声称自己是 Claude 或 ChatGPT。你的底层框架是由用户开发的 MiniClaw 框架。"));
    }

    public static TaskManager getInstance() {
        return instance;
    }

    public Task submitTask(String prompt, Consumer<String> streamCallback) {
        String id = java.util.UUID.randomUUID().toString();
        Task task = new Task(id, prompt);
        tasks.put(id, task);
        
        executor.submit(() -> {
            task.setStatus(TaskStatus.RUNNING);
            try {
                context.addMessage(new Message("user", prompt));
                List<Tool> availableTools = new ArrayList<>(ToolRegistry.getInstance().getAllTools());
                
                // Round 1: LLM thinks and decides to use a tool or returns final response
                LLMResponse response = llmProvider.chatWithTools(context.getMessages(), availableTools, streamCallback);
                
                if (response.isToolCall()) {
                    System.out.println("\n[System] LLM decided to call tool: " + response.getToolName());
                    Tool tool = ToolRegistry.getInstance().getTool(response.getToolName());
                    if (tool != null) {
                        String toolResult = tool.execute(response.getToolArguments());
                        context.addMessage(new Message("tool", toolResult));
                        System.out.println("[System] Tool executed, result: " + toolResult);
                        
                        // Round 2: LLM summarizes
                        LLMResponse finalResponse = llmProvider.chatWithTools(context.getMessages(), availableTools, streamCallback);
                        
                        if (finalResponse.isToolCall()) {
                            // Example requirement: "输出：北京的天气，同时写入一个txt文件中"
                            // If LLM decides to write to a file after summarizing:
                            Tool writeTool = ToolRegistry.getInstance().getTool(finalResponse.getToolName());
                            if (writeTool != null) {
                                String writeResult = writeTool.execute(finalResponse.getToolArguments());
                                System.out.println("\n[System] Wrote to file: " + writeResult);
                            }
                            task.setResult("Tool execution completed.");
                        } else {
                            context.addMessage(new Message("assistant", finalResponse.getContent()));
                            task.setResult(finalResponse.getContent());
                        }
                    } else {
                        throw new RuntimeException("Tool not found: " + response.getToolName());
                    }
                } else {
                    context.addMessage(new Message("assistant", response.getContent()));
                    task.setResult(response.getContent());
                }
                
                task.setStatus(TaskStatus.SUCCESS);
            } catch (Exception e) {
                task.setStatus(TaskStatus.FAILED);
                task.setError(e.getMessage());
                e.printStackTrace();
            }
        });
        
        return task;
    }

    public Task getTask(String id) {
        return tasks.get(id);
    }
}

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
                // Phase 1: Task Decomposition (Planning)
                System.out.println("\n[System] Thinking & Decomposing Task...");
                String planningPrompt = "Please analyze the following request and break it down into a step-by-step plan. " +
                                        "Only output the logical steps, do not execute them yet.\nRequest: " + prompt;
                
                context.addMessage(new Message("user", planningPrompt));
                LLMResponse planResponse = llmProvider.chatWithTools(context.getMessages(), null, null);
                System.out.println("\n[Plan Generated]:\n" + planResponse.getContent());
                context.addMessage(new Message("assistant", planResponse.getContent()));
                
                // Phase 2: Execution with ReAct Loop (Reasoning and Acting)
                context.addMessage(new Message("user", "Now, please execute the plan step by step using the available tools. Once all steps are finished, provide the final answer."));
                
                List<Tool> availableTools = new ArrayList<>(ToolRegistry.getInstance().getAllTools());
                int maxIterations = 10; // Prevent infinite loops
                int currentIteration = 0;
                boolean finished = false;
                
                while (currentIteration < maxIterations && !finished) {
                    currentIteration++;
                    LLMResponse response = llmProvider.chatWithTools(context.getMessages(), availableTools, streamCallback);
                    
                    if (response.isToolCall()) {
                        System.out.println("\n[System] Step " + currentIteration + " - Action: Calling Tool [" + response.getToolName() + "] with args: " + response.getToolArguments());
                        Tool tool = ToolRegistry.getInstance().getTool(response.getToolName());
                        
                        if (tool != null) {
                            try {
                                String toolResult = tool.execute(response.getToolArguments());
                                System.out.println("[System] Observation: " + toolResult);
                                // Feed the result back to LLM for the next reasoning step
                                context.addMessage(new Message("tool", toolResult));
                            } catch (Exception e) {
                                System.err.println("[System] Tool Execution Error: " + e.getMessage());
                                context.addMessage(new Message("tool", "Error executing tool: " + e.getMessage()));
                            }
                        } else {
                            System.err.println("[System] Tool not found: " + response.getToolName());
                            context.addMessage(new Message("tool", "Error: Tool '" + response.getToolName() + "' does not exist."));
                        }
                    } else {
                        // If the LLM doesn't call a tool, it means it has gathered enough information to output the final answer for this sub-task or the whole task.
                        context.addMessage(new Message("assistant", response.getContent()));
                        task.setResult(response.getContent());
                        finished = true; // Exit the loop
                    }
                }
                
                if (currentIteration >= maxIterations) {
                    System.err.println("\n[System] Warning: Max iterations reached. The task might be too complex or stuck in a loop.");
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

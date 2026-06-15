package com.miniclaw.tool;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ToolRegistry {
    private static final ToolRegistry instance = new ToolRegistry();
    private final Map<String, Tool> tools = new ConcurrentHashMap<>();

    private ToolRegistry() {}

    public static ToolRegistry getInstance() {
        return instance;
    }

    public void register(Tool tool) {
        tools.put(tool.getName(), tool);
        System.out.println("Registered tool: " + tool.getName());
    }

    public Tool getTool(String name) {
        return tools.get(name);
    }

    public Collection<Tool> getAllTools() {
        return tools.values();
    }
}

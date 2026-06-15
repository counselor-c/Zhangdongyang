package com.miniclaw.tool;

public interface Tool {
    String getName();
    String getDescription();
    String getParametersSchema(); // JSON Schema
    String execute(String arguments);
}

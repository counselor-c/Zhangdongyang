package com.miniclaw.task;

public class Task {
    private String id;
    private String prompt;
    private TaskStatus status;
    private String result;
    private String error;

    public Task(String id, String prompt) {
        this.id = id;
        this.prompt = prompt;
        this.status = TaskStatus.PENDING;
    }

    public String getId() { return id; }
    public String getPrompt() { return prompt; }

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}

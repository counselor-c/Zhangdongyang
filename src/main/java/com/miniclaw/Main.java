package com.miniclaw;

import com.miniclaw.task.Task;
import com.miniclaw.task.TaskManager;
import com.miniclaw.task.TaskStatus;
import com.miniclaw.tool.ToolScanner;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        System.out.println("Initializing MiniClaw Framework...");
        
        // 1. Dynamic tool discovery
        System.out.println("Scanning for tools...");
        ToolScanner.scanAndRegister("com.miniclaw.tool.builtin");
        
        System.out.println("\nMiniClaw AI Assistant is ready. Type 'exit' to quit.");
        
        Scanner scanner = new Scanner(System.in);
        TaskManager taskManager = TaskManager.getInstance();
        
        while (true) {
            System.out.print("\nUser > ");
            String input = scanner.nextLine().trim();
            
            if ("exit".equalsIgnoreCase(input)) {
                System.out.println("Goodbye!");
                System.exit(0);
            }
            if (input.isEmpty()) {
                continue;
            }
            
            System.out.print("Assistant > ");
            
            // Submit task
            Task task = taskManager.submitTask(input, token -> {
                System.out.print(token);
                System.out.flush();
            });
            
            // Wait for task to finish in CLI demo
            while (task.getStatus() == TaskStatus.PENDING || task.getStatus() == TaskStatus.RUNNING) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            
            System.out.println(); // Newline after stream
            
            if (task.getStatus() == TaskStatus.FAILED) {
                System.err.println("\n[Error] Task failed: " + task.getError());
            }
        }
    }
}

package com.miniclaw.context;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class ConversationContext {
    private static final int MAX_HISTORY = 10;
    private final LinkedList<Message> messages = new LinkedList<>();
    private final String backupFilePath = "conversation_history.txt";

    public void addMessage(Message message) {
        messages.add(message);
        if (messages.size() > MAX_HISTORY) {
            Message oldest = messages.removeFirst();
            writeToFile(oldest);
        }
    }

    public List<Message> getMessages() {
        return messages;
    }

    private void writeToFile(Message msg) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(backupFilePath, true))) {
            writer.write(String.format("[%s]: %s\n", msg.getRole(), msg.getContent()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

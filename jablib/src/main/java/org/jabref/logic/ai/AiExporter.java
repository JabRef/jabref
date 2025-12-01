package org.jabref.logic.ai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AiExporter {

    private final BibEntry entry;

    public AiExporter(BibEntry entry) {
        this.entry = entry;
    }

    public String buildMarkdownExport(String contentTitle, String contentBody) {
        StringBuilder sb = new StringBuilder();
        sb.append("## Bibtex\n\n```bibtex\n");
        sb.append("@").append(entry.getType().getName()).append("{").append(entry.getCitationKey().orElse("")).append(",\n");
        for (Field field : entry.getFields()) {
            String value = entry.getField(field).orElse("");
            sb.append("  ").append(field.getName()).append(" = {").append(value).append("},\n");
        }
        sb.append("}\n```\n\n");
        sb.append("## ").append(contentTitle).append("\n\n");
        sb.append(contentBody);

        return sb.toString();
    }

    public String buildMarkdownForChat(List<ChatMessage> messages) {
        StringBuilder conversation = new StringBuilder();
        for (ChatMessage msg : messages) {
            String role = "";
            String content = "";

            if (msg instanceof UserMessage userMessage) {
                role = "User";
                content = userMessage.singleText();
            } else if (msg instanceof AiMessage aiMessage) {
                role = "AI";
                content = aiMessage.text();
            } else {
                continue;
            }
            conversation.append("**").append(role).append(":**\n\n");
            conversation.append(content).append("\n\n");
        }
        return buildMarkdownExport("Conversation", conversation.toString());
    }

    public String buildJsonExport(String provider, String model, String timestamp, List<ChatMessage> messages) {
        Map<String, Object> root = new HashMap<>();

        root.put("latest_provider", provider);
        root.put("latest_model", model);
        root.put("timestamp", timestamp);

        Map<String, String> entryMap = new HashMap<>();
        for (Field field : entry.getFields()) {
            entryMap.put(field.getName(), entry.getField(field).orElse(""));
        }
        root.put("entry", entryMap);

        StringBuilder bibtex = new StringBuilder();
        bibtex.append("@").append(entry.getType().getName()).append("{").append(entry.getCitationKey().orElse("")).append(",\n");
        for (Field field : entry.getFields()) {
            bibtex.append("  ").append(field.getName()).append(" = {").append(entry.getField(field).orElse("")).append("},\n");
        }
        bibtex.append("}");
        root.put("entry_bibtex", bibtex.toString());

        List<Map<String, String>> conversationList = new ArrayList<>();
        for (ChatMessage msg : messages) {
            String role;
            String content;

            if (msg instanceof UserMessage userMessage) {
                role = "user";
                content = userMessage.singleText();
            } else if (msg instanceof AiMessage aiMessage) {
                role = "assistant";
                content = aiMessage.text();
            } else {
                continue;
            }

            Map<String, String> msgMap = new HashMap<>();
            msgMap.put("role", role);
            msgMap.put("content", content);
            conversationList.add(msgMap);
        }
        root.put("conversation", conversationList);

        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        return gson.toJson(root);
    }
}

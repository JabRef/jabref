package org.jabref.logic.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;

public class AiExporter {

    private final BibEntry entry;
    private final BibEntryTypesManager entryTypesManager;
    private final FieldPreferences fieldPreferences;

    public AiExporter(BibEntry entry, BibEntryTypesManager entryTypesManager, FieldPreferences fieldPreferences) {
        this.entry = entry;
        this.entryTypesManager = entryTypesManager;
        this.fieldPreferences = fieldPreferences;
    }

    public String buildMarkdownExport(String contentTitle, String contentBody) {
        StringBuilder sb = new StringBuilder();
        String bibtex = entry.getStringRepresentation(entry, BibDatabaseMode.BIBTEX, entryTypesManager, fieldPreferences);
        sb.append("## Bibtex\n\n```bibtex\n");
        sb.append(bibtex);
        sb.append("\n```\n\n");
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
                // System messages and tool execution results are internal details
                // and are hidden to keep the conversation readable for the user.
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

        String bibtex = entry.getStringRepresentation(entry, BibDatabaseMode.BIBTEX, entryTypesManager, fieldPreferences);
        root.put("entry_bibtex", bibtex);

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
                // I ignored SystemMessage, ToolExecutionResultMessage, ErrorMessage and ErrorMessage as they are not part of the conversation exchange.
                continue;
            }

            conversationList.add(Map.of("role", role, "content", content));
        }
        root.put("conversation", conversationList);

        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        return gson.toJson(root);
    }
}

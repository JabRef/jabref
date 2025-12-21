package org.jabref.logic.ai;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.jabref.logic.ai.util.ErrorMessage;
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

    private final List<BibEntry> entries;
    private final BibEntryTypesManager entryTypesManager;
    private final FieldPreferences fieldPreferences;

    public AiExporter(List<BibEntry> entries, BibEntryTypesManager entryTypesManager, FieldPreferences fieldPreferences) {
        this.entries = entries;
        this.entryTypesManager = entryTypesManager;
        this.fieldPreferences = fieldPreferences;
    }

    public AiExporter(BibEntry entry, BibEntryTypesManager entryTypesManager, FieldPreferences fieldPreferences) {
        this(List.of(entry), entryTypesManager, fieldPreferences);
    }

    public String buildMarkdownExport(String heading, String contentTitle, String contentBody) {
        StringJoiner stringJoiner = new StringJoiner("\n");
        stringJoiner.add("# " + heading);
        stringJoiner.add("");
        stringJoiner.add("## BibTeX");
        stringJoiner.add("");
        stringJoiner.add("```bibtex");

        StringJoiner bibtexJoiner = new StringJoiner("\n");
        for (BibEntry entry : entries) {
            String bibtex = entry.getStringRepresentation(entry, BibDatabaseMode.BIBTEX, entryTypesManager, fieldPreferences).trim();
            if (!bibtex.isEmpty()) {
                bibtexJoiner.add(bibtex);
                bibtexJoiner.add("");
            }
        }
        stringJoiner.add(bibtexJoiner.toString().trim());

        stringJoiner.add("```");
        stringJoiner.add("");
        stringJoiner.add("## " + contentTitle);
        stringJoiner.add("");
        stringJoiner.add(contentBody.trim());

        return stringJoiner + "\n";
    }

    public String buildMarkdownForChat(List<ChatMessage> messages) {
        StringJoiner conversation = new StringJoiner("\n");
        for (ChatMessage msg : messages) {
            String role = "";
            String content = "";
            if (msg instanceof UserMessage userMessage) {
                role = "User";
                content = userMessage.singleText();
            } else if (msg instanceof AiMessage aiMessage) {
                role = "AI";
                content = aiMessage.text();
            } else if (msg instanceof ErrorMessage errorMessage) {
                role = "Error";
                content = errorMessage.getText();
            } else {
                // ignored SystemMessage, ToolExecutionResultMessage as they are not part of the conversation exchange.
                continue;
            }
            conversation.add("**" + role + ":**");
            conversation.add("");
            conversation.add(content);
            conversation.add("");
        }
        return buildMarkdownExport("AI chat", "Conversation", conversation.toString());
    }

    public String buildJsonExport(String provider, String model, String timestamp, List<ChatMessage> messages) {
        Map<String, Object> root = new HashMap<>();

        root.put("latest_provider", provider);
        root.put("latest_model", model);
        root.put("export_timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        List<Map<String, Object>> entriesList = new ArrayList<>();
        for (BibEntry entry : entries) {
            Map<String, Object> entryData = new HashMap<>();

            Map<String, String> fields = new HashMap<>();
            for (Field field : entry.getFields()) {
                fields.put(field.getName(), entry.getField(field).orElse(""));
            }
            entryData.put("fields", fields);
            entryData.put("bibtex", entry.getStringRepresentation(entry, BibDatabaseMode.BIBTEX, entryTypesManager, fieldPreferences));

            entriesList.add(entryData);
        }
        root.put("entries", entriesList);

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
            } else if (msg instanceof ErrorMessage errorMessage) {
                role = "error";
                content = errorMessage.getText();
            } else {
                // ignored SystemMessage, ToolExecutionResultMessage as they are not part of the conversation exchange.
                continue;
            }

            conversationList.add(Map.of("role", role, "content", content));
        }
        root.put("conversation", conversationList);

        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        return gson.toJson(root);
    }
}

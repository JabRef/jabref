package org.jabref.logic.ai.chatting.exporters;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jabref.logic.bibtex.BibEntryWriter;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.bibtex.FieldWriter;
import org.jabref.logic.exporter.BibWriter;
import org.jabref.model.ai.AiMetadata;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/// Exports an AI chat conversation to JSON format.
///
/// The JSON output includes export metadata (provider, model, timestamp), BibTeX entry data,
/// and the conversation messages with role/content pairs.
///
/// The JSON schema of the export is handcrafted because there is no well-established export format.
public class AiChatJsonExporter implements AiChatExporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiChatJsonExporter.class);

    private static final JsonMapper OBJECT_MAPPER = JsonMapper
            .builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    private final BibEntryTypesManager entryTypesManager;
    private final FieldPreferences fieldPreferences;

    public AiChatJsonExporter(BibEntryTypesManager entryTypesManager, FieldPreferences fieldPreferences) {
        this.entryTypesManager = entryTypesManager;
        this.fieldPreferences = fieldPreferences;
    }

    @Override
    public String export(AiMetadata metadata, List<BibEntry> entries, BibDatabaseMode mode, List<ChatMessage> messages) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("latest_provider", metadata.aiProvider().toString());
        root.put("latest_model", metadata.model());
        root.put("export_timestamp", metadata.timestamp().toString());

        List<Map<String, Object>> entriesList = new ArrayList<>();
        for (BibEntry entry : entries) {
            Map<String, Object> entryData = new LinkedHashMap<>();

            Map<String, String> fields = new LinkedHashMap<>();
            for (Field field : entry.getFields()) {
                fields.put(field.getName(), entry.getField(field).orElse(""));
            }
            entryData.put("fields", fields);
            entryData.put("bibtex", entryToBibtex(entry, mode));

            entriesList.add(entryData);
        }
        root.put("entries", entriesList);

        List<Map<String, String>> conversationList = new ArrayList<>();
        for (ChatMessage msg : messages) {
            String role = switch (msg.role()) {
                case USER ->
                        "user";
                case AI ->
                        "assistant";
                case ERROR ->
                        "error";
                case SYSTEM ->
                        null; // System messages are not part of the conversation exchange
            };

            if (role == null) {
                continue;
            }

            Map<String, String> message = new LinkedHashMap<>();
            message.put("role", role);
            message.put("content", msg.content());
            message.put("timestamp", msg.timestamp().toString());
            conversationList.add(message);
        }
        root.put("conversation", conversationList);

        try {
            return OBJECT_MAPPER.writeValueAsString(root);
        } catch (JacksonException e) {
            // Should not happen for a plain Map<String, Object> with String values
            throw new RuntimeException("Failed to serialize chat export to JSON", e);
        }
    }

    private String entryToBibtex(BibEntry entry, BibDatabaseMode mode) {
        try {
            StringWriter stringWriter = new StringWriter();
            BibWriter bibWriter = new BibWriter(stringWriter, "\n");
            FieldWriter fieldWriter = new FieldWriter(fieldPreferences);
            BibEntryWriter bibEntryWriter = new BibEntryWriter(fieldWriter, entryTypesManager);
            bibEntryWriter.write(entry, bibWriter, mode, true);
            return stringWriter.toString();
        } catch (IOException e) {
            LOGGER.error("Could not write entry to BibTeX", e);
            return "";
        }
    }
}

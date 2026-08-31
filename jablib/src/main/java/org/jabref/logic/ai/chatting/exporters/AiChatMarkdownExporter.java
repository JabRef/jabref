package org.jabref.logic.ai.chatting.exporters;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import org.jabref.logic.ai.templates.AiTemplateRenderer;
import org.jabref.logic.bibtex.BibEntryWriter;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.bibtex.FieldWriter;
import org.jabref.logic.exporter.BibWriter;
import org.jabref.model.ai.AiMetadata;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Exports an AI chat conversation to Markdown format.
///
/// The Markdown output is produced by rendering a configurable Velocity template.
/// The template has access to the following variables:
///
/// - `$metadata` - {@link AiMetadata} with provider, model, and timestamp.
/// - `$bibtex` - pre-rendered BibTeX string of all associated entries.
/// - `$messages` - filtered list of {@link ChatMessage} objects (SYSTEM messages excluded).
///
public class AiChatMarkdownExporter implements AiChatExporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiChatMarkdownExporter.class);

    private final BibEntryTypesManager entryTypesManager;
    private final FieldPreferences fieldPreferences;
    private final String markdownExportTemplate;

    public AiChatMarkdownExporter(BibEntryTypesManager entryTypesManager, FieldPreferences fieldPreferences, String markdownExportTemplate) {
        this.entryTypesManager = entryTypesManager;
        this.fieldPreferences = fieldPreferences;
        this.markdownExportTemplate = markdownExportTemplate;
    }

    @Override
    public String export(AiMetadata metadata, List<BibEntry> entries, BibDatabaseMode mode, List<ChatMessage> messages) {
        String bibtex = concatenateBibtexEntries(entries, mode);

        List<ChatMessage> filteredMessages = messages.stream()
                                                     .filter(msg -> msg.role() != ChatMessage.Role.SYSTEM)
                                                     .toList();

        return AiTemplateRenderer.renderMarkdownChatExport(markdownExportTemplate, metadata, bibtex, filteredMessages);
    }

    private String concatenateBibtexEntries(List<BibEntry> entries, BibDatabaseMode mode) {
        StringBuilder sb = new StringBuilder();
        for (BibEntry entry : entries) {
            String bibtex = entryToBibtex(entry, mode).trim();
            if (!bibtex.isEmpty()) {
                if (!sb.isEmpty()) {
                    sb.append('\n');
                }
                sb.append(bibtex);
            }
        }
        return sb.toString();
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

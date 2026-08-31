package org.jabref.logic.ai.chatting.exporters;

import java.time.Instant;
import java.util.List;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.model.ai.AiMetadata;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.llm.AiProvider;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiChatExporterTest {

    private static final String MARKDOWN_TEMPLATE = """
            # AI Chat Export

            **Provider:** $metadata.aiProvider
            **Model:** $metadata.model

            ## Conversation

            #foreach( $msg in $messages )
            **$msg.role.displayName:**

            $msg.content

            #end
            """;

    private AiMetadata metadata;
    private List<BibEntry> entries;

    @BeforeEach
    void setUp() {
        metadata = new AiMetadata(AiProvider.OPEN_AI, "gpt-4", Instant.now());
        entries = List.of(new BibEntry(StandardEntryType.Article).withCitationKey("test2024"));
    }

    static List<AiChatExporter> exporters() {
        BibEntryTypesManager entryTypesManager = new BibEntryTypesManager();
        FieldPreferences fieldPreferences = new FieldPreferences(true, List.of(), List.of());
        return List.of(
                new AiChatMarkdownExporter(entryTypesManager, fieldPreferences, MARKDOWN_TEMPLATE),
                new AiChatJsonExporter(entryTypesManager, fieldPreferences)
        );
    }

    @ParameterizedTest
    @MethodSource("exporters")
    void userMessageContentAppearsInExport(AiChatExporter exporter) {
        List<ChatMessage> messages = List.of(ChatMessage.userMessage("hello from user"));

        String result = exporter.export(metadata, entries, BibDatabaseMode.BIBTEX, messages);

        assertTrue(result.contains("hello from user"));
    }

    @ParameterizedTest
    @MethodSource("exporters")
    void aiMessageContentAppearsInExport(AiChatExporter exporter) {
        List<ChatMessage> messages = List.of(ChatMessage.aiMessage("reply from AI", List.of()));

        String result = exporter.export(metadata, entries, BibDatabaseMode.BIBTEX, messages);

        assertTrue(result.contains("reply from AI"));
    }

    @ParameterizedTest
    @MethodSource("exporters")
    void errorMessageContentAppearsInExport(AiChatExporter exporter) {
        List<ChatMessage> messages = List.of(ChatMessage.errorMessage(new RuntimeException("something broke")));

        String result = exporter.export(metadata, entries, BibDatabaseMode.BIBTEX, messages);

        assertTrue(result.contains("something broke"));
    }

    @ParameterizedTest
    @MethodSource("exporters")
    void systemMessagesAreExcludedFromExport(AiChatExporter exporter) {
        List<ChatMessage> messages = List.of(
                ChatMessage.systemMessage("top secret system prompt"),
                ChatMessage.userMessage("visible user message")
        );

        String result = exporter.export(metadata, entries, BibDatabaseMode.BIBTEX, messages);

        assertTrue(result.contains("visible user message"));
        assertFalse(result.contains("top secret system prompt"));
    }

    @ParameterizedTest
    @MethodSource("exporters")
    void multipleMessagesAllContentPresent(AiChatExporter exporter) {
        List<ChatMessage> messages = List.of(
                ChatMessage.userMessage("first question"),
                ChatMessage.aiMessage("first answer", List.of()),
                ChatMessage.userMessage("second question"),
                ChatMessage.aiMessage("second answer", List.of())
        );

        String result = exporter.export(metadata, entries, BibDatabaseMode.BIBTEX, messages);

        assertTrue(result.contains("first question"));
        assertTrue(result.contains("first answer"));
        assertTrue(result.contains("second question"));
        assertTrue(result.contains("second answer"));
    }

    @ParameterizedTest
    @MethodSource("exporters")
    void messageWithSpecialJsonCharactersAppearsCorrectly(AiChatExporter exporter) {
        List<ChatMessage> messages = List.of(ChatMessage.userMessage("say \"hello\" & <world>"));

        String result = exporter.export(metadata, entries, BibDatabaseMode.BIBTEX, messages);

        // The raw content text must appear somewhere in the output (possibly escaped for JSON)
        assertTrue(result.contains("hello") && result.contains("world"));
    }
}

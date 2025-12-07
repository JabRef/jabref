package org.jabref.logic.importer.plaincitation;

import java.util.Optional;

import org.jabref.logic.ai.templates.AiTemplatesService;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LlmPlainCitationParserTest {

    @Test
    void parsePlainCitation() {
        // Given
        String input = "E. G. Santana Jr., G. Benjamin, M. Araujo, and H. Santos, \"Which Prompting Technique Should I Use? An Empirical Investigation of Prompting Techniques for Software Engineering Tasks\", arXiv:2506.05614, Jun. 2025.";

        // Mocks
        AiTemplatesService aiTemplatesService = mock(AiTemplatesService.class);
        when(aiTemplatesService.makeCitationParsingSystemMessage()).thenReturn("system");
        when(aiTemplatesService.makeCitationParsingUserMessage(input)).thenReturn("user: " + input);

        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);

        ChatModel chatModel = mock(ChatModel.class);
        ChatResponse chatResponse = mock(ChatResponse.class);
        AiMessage aiMessage = mock(AiMessage.class);

        String bibtex = """
                @article{key,
                  title={Which Prompting Technique Should I Use? An Empirical Investigation of Prompting Techniques for Software Engineering Tasks},
                  author={Santana Jr., E. G. and Benjamin, G. and Araujo, M. and Santos, H.},
                  journal={arXiv},
                  volume={2506.05614},
                  year={2025}
                }
                """;

        when(aiMessage.text()).thenReturn(bibtex);
        when(chatResponse.aiMessage()).thenReturn(aiMessage);
        when(chatModel.chat(anyList())).thenReturn(chatResponse);

        LlmPlainCitationParser parser = new LlmPlainCitationParser(aiTemplatesService, importFormatPreferences, chatModel);

        // When
        Optional<BibEntry> result;
        try {
            result = parser.parsePlainCitation(input);
        } catch (FetcherException e) {
            throw new RuntimeException(e);
        }

        // Then
        BibEntry entry = result.orElseThrow();
        // arXiv id should be moved from volume to eprint by EprintCleanup
        assertEquals("2506.05614", entry.getField(StandardField.EPRINT).orElse(""));
        assertEquals("", entry.getField(StandardField.VOLUME).orElse(""));
    }
}

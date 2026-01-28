package org.jabref.logic.importer.plaincitation;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.ai.templates.AiTemplatesService;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

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
    void parsePlainCitation() throws FetcherException {
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
                @article{Santana,
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
        Optional<BibEntry> result = parser.parsePlainCitation(input);

        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Santana")
                .withField(StandardField.TITLE, "Which Prompting Technique Should I Use? An Empirical Investigation of Prompting Techniques for Software Engineering Tasks")
                .withField(StandardField.AUTHOR, "Santana Jr., E. G. and Benjamin, G. and Araujo, M. and Santos, H.")
                .withField(StandardField.EPRINTTYPE, "arxiv")
                .withField(StandardField.EPRINT, "2506.05614")
                .withField(StandardField.YEAR, "2025");

        // Then
        assertEquals(Optional.of(expected), result);
    }

    @Test
    void parseMultiplePlainCitations() throws FetcherException {
        // Given
        String input = """
                E. G. Santana Jr., G. Benjamin, M. Araujo, and H. Santos, \"Which Prompting Technique Should I Use? An Empirical Investigation of Prompting Techniques for Software Engineering Tasks\", arXiv:2506.05614, Jun. 2025.

                Z. Rasheed, M. A. Sami, M. Waseem, K.-K. Kemell, X. Wang, A. Nguyen, K. Systä, and P. Abrahamsson, "AI-powered Code Review with LLMs: Early Results", arXiv:2404.18496, Apr. 2024.
                """;

        // Mocks
        AiTemplatesService aiTemplatesService = mock(AiTemplatesService.class);
        when(aiTemplatesService.makeCitationParsingSystemMessage()).thenReturn("system");
        when(aiTemplatesService.makeCitationParsingUserMessage(input)).thenReturn("user: " + input);

        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);

        ChatModel chatModel = mock(ChatModel.class);
        ChatResponse chatResponse = mock(ChatResponse.class);
        AiMessage aiMessage = mock(AiMessage.class);

        String bibtex = """
                @article{Santana,
                  title={Which Prompting Technique Should I Use? An Empirical Investigation of Prompting Techniques for Software Engineering Tasks},
                  author={Santana Jr., E. G. and Benjamin, G. and Araujo, M. and Santos, H.},
                  journal={arXiv},
                  volume={2506.05614},
                  year={2025}
                }

                @Article{Rasheed,
                  author     = {Z. Rasheed and M. A. Sami and M. Waseem and K.-K. Kemell and X. Wang and A. Nguyen and K. Systä and P. Abrahamsson},
                  year       = {2025},
                  title      = {AI-powered Code Review with LLMs: Early Results},
                  eprint     = {2404.18496},
                  eprinttype = {arxiv},
                }
                """;

        when(aiMessage.text()).thenReturn(bibtex);
        when(chatResponse.aiMessage()).thenReturn(aiMessage);
        when(chatModel.chat(anyList())).thenReturn(chatResponse);

        LlmPlainCitationParser parser = new LlmPlainCitationParser(aiTemplatesService, importFormatPreferences, chatModel);

        // When
        List<BibEntry> result = parser.parseMultiplePlainCitations(input);

        BibEntry santana = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Santana")
                .withField(StandardField.TITLE, "Which Prompting Technique Should I Use? An Empirical Investigation of Prompting Techniques for Software Engineering Tasks")
                .withField(StandardField.AUTHOR, "Santana Jr., E. G. and Benjamin, G. and Araujo, M. and Santos, H.")
                .withField(StandardField.EPRINTTYPE, "arxiv")
                .withField(StandardField.EPRINT, "2506.05614")
                .withField(StandardField.YEAR, "2025");
        BibEntry rasheed = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Rasheed")
                .withField(StandardField.TITLE, "AI-powered Code Review with LLMs: Early Results")
                .withField(StandardField.AUTHOR, "Z. Rasheed and M. A. Sami and M. Waseem and K.-K. Kemell and X. Wang and A. Nguyen and K. Systä and P. Abrahamsson")
                .withField(StandardField.EPRINTTYPE, "arxiv")
                .withField(StandardField.EPRINT, "2404.18496")
                .withField(StandardField.YEAR, "2025");

        // Then
        assertEquals(List.of(santana, rasheed), result);
    }
}

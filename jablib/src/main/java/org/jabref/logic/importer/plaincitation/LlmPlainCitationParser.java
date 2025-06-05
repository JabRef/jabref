package org.jabref.logic.importer.plaincitation;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.ai.templates.AiTemplatesService;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.model.entry.BibEntry;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;

public class LlmPlainCitationParser implements PlainCitationParser {
    private final AiTemplatesService aiTemplatesService;
    private final ImportFormatPreferences importFormatPreferences;
    private final ChatModel llm;

    public LlmPlainCitationParser(AiTemplatesService aiTemplatesService, ImportFormatPreferences importFormatPreferences, ChatModel llm) {
        this.aiTemplatesService = aiTemplatesService;
        this.importFormatPreferences = importFormatPreferences;
        this.llm = llm;
    }

    @Override
    public Optional<BibEntry> parsePlainCitation(String text) throws FetcherException {
        try {
            return BibtexParser.singleFromString(getBibtexStringFromLlm(text), importFormatPreferences);
        } catch (ParseException e) {
            throw new FetcherException("Could not parse BibTeX returned from LLM", e);
        }
    }

    private String getBibtexStringFromLlm(String searchQuery) {
        String systemMessage = aiTemplatesService.makeCitationParsingSystemMessage();
        String userMessage = aiTemplatesService.makeCitationParsingUserMessage(searchQuery);

        return llm.chat(
                List.of(
                        new SystemMessage(systemMessage),
                        new UserMessage(userMessage)
                )
        ).aiMessage().text();
    }
}

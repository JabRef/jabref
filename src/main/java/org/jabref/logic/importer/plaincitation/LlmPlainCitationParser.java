package org.jabref.logic.importer.plaincitation;

import java.util.Map;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.model.entry.BibEntry;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.PromptTemplate;

public class LlmPlainCitationParser implements PlainCitationParser {
    private static final String SYSTEM_MESSAGE = "You are a bot to convert a plain text citation to a BibTeX entry. The user you talk to understands only BibTeX code, so provide it plainly without any wrappings.";
    private static final PromptTemplate USER_MESSAGE_TEMPLATE = PromptTemplate.from("Please convert this plain text citation to a BibTeX entry:\n{{citation}}\nIn your output, please provide only BibTex code as your message.");

    private final ImportFormatPreferences importFormatPreferences;
    private final ChatLanguageModel llm;

    public LlmPlainCitationParser(ImportFormatPreferences importFormatPreferences, ChatLanguageModel llm) {
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
        return llm.generate(new SystemMessage(SYSTEM_MESSAGE),
                new UserMessage(
                        USER_MESSAGE_TEMPLATE.apply(Map.of("citation", searchQuery)).toString()
                )).content().text();
    }
}

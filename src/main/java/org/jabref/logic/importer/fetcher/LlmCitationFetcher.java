package org.jabref.logic.importer.fetcher;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.model.entry.BibEntry;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.PromptTemplate;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;

public class LlmCitationFetcher implements SearchBasedFetcher {
    private static final String SYSTEM_MESSAGE = "You are a bot to convert a plain text citation to a BibTeX entry. The user you talk to understands only BibTeX code, so provide it plainly without any wrappings.";
    private static final PromptTemplate USER_MESSAGE_TEMPLATE = PromptTemplate.from("Please convert this plain text citation to a BibTeX entry:\n{{citation}}\nIn your output, please provide only BibTex code as your message.");

    private final ImportFormatPreferences importFormatPreferences;
    private final ChatLanguageModel llm;

    public LlmCitationFetcher(ImportFormatPreferences importFormatPreferences, ChatLanguageModel llm) {
        this.importFormatPreferences = importFormatPreferences;
        this.llm = llm;
    }

    @Override
    public List<BibEntry> performSearch(String searchQuery) throws FetcherException {
        return parseBibtexUsingLlm(searchQuery).map(List::of).orElse(List.of());
    }

    private Optional<BibEntry> parseBibtexUsingLlm(String searchQuery) throws FetcherException {
        try {
            return BibtexParser.singleFromString(getBibtexStringFromLlm(searchQuery), importFormatPreferences);
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

    @Override
    public String getName() {
        return "LLM";
    }

    /**
     * Not used
     */
    @Override
    public List<BibEntry> performSearch(QueryNode luceneQuery) throws FetcherException {
        return Collections.emptyList();
    }
}

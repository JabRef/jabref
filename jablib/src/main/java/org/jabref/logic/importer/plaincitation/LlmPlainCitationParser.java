package org.jabref.logic.importer.plaincitation;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.ai.templates.AiTemplatesService;
import org.jabref.logic.cleanup.EprintCleanup;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.importer.fileformat.pdf.PdfImporterWithPlainCitationParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class LlmPlainCitationParser extends PdfImporterWithPlainCitationParser implements PlainCitationParser {
    private final AiTemplatesService aiTemplatesService;
    private final ImportFormatPreferences importFormatPreferences;
    private final ChatModel llm;
    private final EprintCleanup eprintCleanup = new EprintCleanup();

    public LlmPlainCitationParser(AiTemplatesService aiTemplatesService, ImportFormatPreferences importFormatPreferences, ChatModel llm) {
        this.aiTemplatesService = aiTemplatesService;
        this.importFormatPreferences = importFormatPreferences;
        this.llm = llm;
    }

    @Override
    public String getId() {
        return "llm";
    }

    @Override
    public String getName() {
        return "LLM";
    }

    @Override
    public String getDescription() {
        return Localization.lang("LLM");
    }

    @Override
    public Optional<BibEntry> parsePlainCitation(String text) throws FetcherException {
        try {
            return BibtexParser.singleFromString(getBibtexStringFromLlm(text), importFormatPreferences)
                               .map(entry -> {
                                   eprintCleanup.cleanup(entry);
                                   return entry;
                               });
        } catch (ParseException e) {
            throw new FetcherException("Could not parse BibTeX returned from LLM", e);
        }
    }

    @Override
    public List<BibEntry> parseMultiplePlainCitations(String text) throws FetcherException {
        String systemMessage = aiTemplatesService.makeCitationParsingSystemMessage();
        String userMessage = aiTemplatesService.makeCitationParsingUserMessage(text);

        String llmResult = llm.chat(
                List.of(
                        new SystemMessage(systemMessage),
                        new UserMessage(userMessage)
                )
        ).aiMessage().text();

        BibtexParser parser = new BibtexParser(importFormatPreferences);
        List<BibEntry> entries;
        try {
            entries = parser.parseEntries(llmResult);
        } catch (ParseException e) {
            throw new FetcherException("Could not parse BibTeX returned from LLM", e);
        }
        entries.forEach(entry -> eprintCleanup.cleanup(entry));
        return entries;
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

package org.jabref.logic.importer.plaincitation;

import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fileformat.pdf.RuleBasedBibliographyPdfImporter;
import org.jabref.logic.importer.util.GrobidPreferences;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class PlainCitationParserFactory {

    /// Creates a parser for any choice that does not require AI dependencies.
    /// For [PlainCitationParserChoice#LLM] use [#getLlmPlainCitationParser] or [#getPlainCitationParser(PlainCitationParserChoice, CitationKeyPatternPreferences, GrobidPreferences, ImportFormatPreferences, AiService)].
    public static PlainCitationParser getPlainCitationParser(PlainCitationParserChoice parserChoice,
                                                             CitationKeyPatternPreferences citationKeyPatternPreferences,
                                                             GrobidPreferences grobidPreferences,
                                                             ImportFormatPreferences importFormatPreferences) {
        return switch (parserChoice) {
            case PlainCitationParserChoice.RULE_BASED_GENERAL ->
                    new RuleBasedPlainCitationParser();
            case PlainCitationParserChoice.RULE_BASED_IEEE ->
                    new RuleBasedBibliographyPdfImporter(citationKeyPatternPreferences);
            case PlainCitationParserChoice.GROBID ->
                    new GrobidPlainCitationParser(grobidPreferences, importFormatPreferences);
            case PlainCitationParserChoice.LLM ->
                    throw new IllegalArgumentException("LLM parser requires AI dependencies; call getLlmPlainCitationParser instead");
        };
    }

    public static PlainCitationParser getPlainCitationParser(PlainCitationParserChoice parserChoice,
                                                             CitationKeyPatternPreferences citationKeyPatternPreferences,
                                                             GrobidPreferences grobidPreferences,
                                                             ImportFormatPreferences importFormatPreferences,
                                                             AiService aiService) {
        if (parserChoice == PlainCitationParserChoice.LLM) {
            return aiService.getLlmPlainCitationParser(importFormatPreferences)
                            .orElseGet(() -> getPlainCitationParser(PlainCitationParserChoice.RULE_BASED_GENERAL,
                                    citationKeyPatternPreferences, grobidPreferences, importFormatPreferences));
        }
        return getPlainCitationParser(parserChoice, citationKeyPatternPreferences, grobidPreferences, importFormatPreferences);
    }

    public static PlainCitationParser getLlmPlainCitationParser(ImportFormatPreferences importFormatPreferences,
                                                                AiPreferences aiPreferences,
                                                                ChatModel chatModel) {
        return new LlmPlainCitationParser(importFormatPreferences, aiPreferences.getCitationParsingSystemMessageTemplate(), chatModel);
    }
}

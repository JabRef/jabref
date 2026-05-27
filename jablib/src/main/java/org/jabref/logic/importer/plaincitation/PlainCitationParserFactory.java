package org.jabref.logic.importer.plaincitation;

import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fileformat.pdf.RuleBasedBibliographyPdfImporter;
import org.jabref.logic.importer.util.GrobidPreferences;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class PlainCitationParserFactory {
    public static PlainCitationParser getPlainCitationParser(PlainCitationParserChoice parserChoice,
                                                             CitationKeyPatternPreferences citationKeyPatternPreferences,
                                                             GrobidPreferences grobidPreferences,
                                                             ImportFormatPreferences importFormatPreferences,
                                                             AiPreferences aiPreferences,
                                                             ChatModel chatModel) {
        return switch (parserChoice) {
            case PlainCitationParserChoice.RULE_BASED_GENERAL ->
                    new RuleBasedPlainCitationParser();
            case PlainCitationParserChoice.RULE_BASED_IEEE ->
                    new RuleBasedBibliographyPdfImporter(citationKeyPatternPreferences);
            case PlainCitationParserChoice.GROBID ->
                    new GrobidPlainCitationParser(grobidPreferences, importFormatPreferences);
            case PlainCitationParserChoice.LLM ->
                    new LlmPlainCitationParser(importFormatPreferences, aiPreferences.getCitationParsingSystemMessageTemplate(), chatModel);
        };
    }
}

package org.jabref.logic.importer.plaincitation;

import org.jabref.logic.ai.AiService;
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
                                                              AiService aiService) {
        return switch (parserChoice) {
            case PlainCitationParserChoice.RULE_BASED_GENERAL ->
                    new RuleBasedPlainCitationParser();
            case PlainCitationParserChoice.RULE_BASED_IEEE ->
                    new RuleBasedBibliographyPdfImporter(citationKeyPatternPreferences);
            case PlainCitationParserChoice.GROBID ->
                    new GrobidPlainCitationParser(grobidPreferences, importFormatPreferences);
            case PlainCitationParserChoice.LLM ->
                    new LlmPlainCitationParser(aiService.getTemplatesService(), importFormatPreferences, aiService.getChatLanguageModel());
        };
    }
}

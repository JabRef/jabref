package org.jabref.logic.importer.fileformat.pdf;

import java.nio.file.Path;

import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.ai.chatting.util.ChatModelFactory;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.plaincitation.LlmPlainCitationParser;
import org.jabref.logic.importer.util.GrobidPreferences;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.NotificationService;

/// Wrapper around the different options to extract citations from a PDF
///
/// We deliberately opt for passing whole [CliPreferences] to ease calling this helper methods
public class CitationsFromPdf {

    public static ParserResult extractCitationsUsingRuleBasedAlgorithm(CliPreferences preferences, Path path) {
        RuleBasedBibliographyPdfImporter importer = new RuleBasedBibliographyPdfImporter(preferences.getCitationKeyPatternPreferences());
        return importer.importDatabase(path);
    }

    public static ParserResult extractCitationsUsingGrobid(CliPreferences preferences, Path path) {
        PdfGrobidImporter importer = new PdfGrobidImporter(preferences.getImportFormatPreferences());
        return importer.importDatabase(path);
    }

    /// Same as [#extractCitationsUsingGrobid(CliPreferences, Path)], but overrides the configured Grobid server URL for this single call.
    public static ParserResult extractCitationsUsingGrobid(CliPreferences preferences, Path path, String grobidUrlOverride) {
        PdfGrobidImporter importer = new PdfGrobidImporter(withOverriddenGrobidUrl(preferences.getImportFormatPreferences(), grobidUrlOverride));
        return importer.importDatabase(path);
    }

    static ImportFormatPreferences withOverriddenGrobidUrl(ImportFormatPreferences importFormatPreferences, String grobidUrlOverride) {
        GrobidPreferences overriddenGrobidPreferences = new GrobidPreferences(
                true,
                importFormatPreferences.grobidPreferences().isGrobidUseAsked(),
                grobidUrlOverride);
        return new ImportFormatPreferences(
                importFormatPreferences.bibEntryPreferences(),
                importFormatPreferences.citationKeyPatternPreferences(),
                importFormatPreferences.fieldPreferences(),
                importFormatPreferences.xmpPreferences(),
                importFormatPreferences.doiPreferences(),
                overriddenGrobidPreferences,
                importFormatPreferences.filePreferences());
    }

    public static ParserResult extractCitationsUsingLLM(CliPreferences preferences, NotificationService notificationService, Path path) {
        try (ChatModel chatModel = ChatModelFactory.create(preferences.getAiPreferences())) {
            LlmPlainCitationParser importer = new LlmPlainCitationParser(
                    preferences.getImportFormatPreferences(),
                    preferences.getAiPreferences().getCitationParsingSystemMessageTemplate(),
                    chatModel
            );

            return importer.importDatabase(path);
        }
    }
}

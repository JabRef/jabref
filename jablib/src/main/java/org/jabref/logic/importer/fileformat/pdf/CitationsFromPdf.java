package org.jabref.logic.importer.fileformat.pdf;

import java.nio.file.Path;

import org.jabref.logic.ai.AiService;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.plaincitation.LlmPlainCitationParser;
import org.jabref.logic.preferences.JabRefCliPreferences;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.logic.util.NotificationService;

/// Wrapper around the different options to extract citations from a PDF
///
/// We deliberately opt for passing whole [JabRefCliPreferences] to ease calling this helper methods
public class CitationsFromPdf {

    /// As NoticationService, one can pass `LOGGER::info`
    public static ParserResult extractCitations(Class<? extends BibliographyFromPdfImporter> importer, JabRefCliPreferences preferences, NotificationService notificationService, Path path) {
        return switch (importer.getSimpleName()) {
            case "RuleBasedBibliographyPdfImporter" ->
                    extractCitationsUsingTextMatching(preferences, path);

            case "PdfGrobidImporter" ->
                    extractCitationsUsingGrobid(preferences, path);

            case "LlmPlainCitationParser" ->
                    extractCitationsUsingLLM(preferences, notificationService, path);

            default ->
                    throw new IllegalArgumentException("Unsupported importer: " + importer.getName());
        };
    }

    public static ParserResult extractCitationsUsingTextMatching(JabRefCliPreferences preferences, Path path) {
        RuleBasedBibliographyPdfImporter importer = new RuleBasedBibliographyPdfImporter(preferences.getCitationKeyPatternPreferences());
        return importer.importDatabase(path);
    }

    public static ParserResult extractCitationsUsingGrobid(JabRefCliPreferences preferences, Path path) {
        PdfGrobidImporter importer = new PdfGrobidImporter(preferences.getImportFormatPreferences());
        return importer.importDatabase(path);
    }

    /// As NoticationService, one can pass `LOGGER::info`
    public static ParserResult extractCitationsUsingLLM(JabRefCliPreferences preferences, NotificationService notificationService, Path path) {
        try (AiService aiService = new AiService(
                preferences.getAiPreferences(),
                preferences.getFilePreferences(),
                preferences.getCitationKeyPatternPreferences(),
                notificationService,
                new CurrentThreadTaskExecutor())) {
            LlmPlainCitationParser importer = new LlmPlainCitationParser(aiService.getTemplatesService(), preferences.getImportFormatPreferences(), aiService.getChatLanguageModel());
            return importer.importDatabase(path);
        }
    }
}

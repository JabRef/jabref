package org.jabref.logic.importer.fileformat.pdf;

import java.nio.file.Path;

import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.ai.chatting.util.ChatModelFactory;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.plaincitation.LlmPlainCitationParser;
import org.jabref.logic.preferences.JabRefCliPreferences;
import org.jabref.logic.util.NotificationService;

/// Wrapper around the different options to extract citations from a PDF
///
/// We deliberately opt for passing whole [JabRefCliPreferences] to ease calling this helper methods
public class CitationsFromPdf {

    public static ParserResult extractCitationsUsingRuleBasedAlgorithm(JabRefCliPreferences preferences, Path path) {
        RuleBasedBibliographyPdfImporter importer = new RuleBasedBibliographyPdfImporter(preferences.getCitationKeyPatternPreferences());
        return importer.importDatabase(path);
    }

    public static ParserResult extractCitationsUsingGrobid(JabRefCliPreferences preferences, Path path) {
        PdfGrobidImporter importer = new PdfGrobidImporter(preferences.getImportFormatPreferences());
        return importer.importDatabase(path);
    }

    public static ParserResult extractCitationsUsingLLM(JabRefCliPreferences preferences, NotificationService notificationService, Path path) {
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

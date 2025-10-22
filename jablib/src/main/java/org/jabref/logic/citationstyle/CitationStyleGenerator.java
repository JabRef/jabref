package org.jabref.logic.citationstyle;

import java.io.IOException;
import java.util.List;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

import org.jbibtex.TokenMgrException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Facade to unify the access to the citation style engine. Use these methods if you need rendered BibTeX item(s) in a
 * given journal style. This class uses {@link CSLAdapter} to create output.
 */
public class CitationStyleGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CitationStyleGenerator.class);
    private static final CSLAdapter CSL_ADAPTER = new CSLAdapter();

    private CitationStyleGenerator() {
    }

    /**
     * Generates a citation based on a given list of entries, .csl style source content and output format with a given {@link BibDatabaseContext}.
     *
     * @implNote The citation is generated using an external library which may take some time, debatable if it is better to call it from outside the main Thread.
     */
    public static String generateCitation(List<BibEntry> bibEntries, String style, CitationStyleOutputFormat outputFormat, BibDatabaseContext databaseContext, BibEntryTypesManager entryTypesManager) {
        try {
            return CSL_ADAPTER.makeCitation(bibEntries, style, outputFormat, databaseContext, entryTypesManager).getText();
        } catch (IOException e) {
            LOGGER.error("Could not generate BibEntry citation", e);
            return Localization.lang("Cannot generate citation based on selected citation style.");
        }
    }

    /**
     * Generates a bibliography list in HTML format based on a given list of entries and .csl style source content with a default {@link BibDatabaseContext}.
     *
     * @implNote The bibliography is generated using an external library which may take some time, debatable if it is better to call it from outside the main Thread.
     */
    protected static String generateBibliography(List<BibEntry> bibEntries, String style, BibEntryTypesManager entryTypesManager) {
        BibDatabaseContext context = new BibDatabaseContext.Builder()
                .database(new BibDatabase(bibEntries))
                .build();
        context.setMode(BibDatabaseMode.BIBLATEX);
        return generateBibliography(bibEntries, style, CitationStyleOutputFormat.HTML, context, entryTypesManager).getFirst();
    }

    /**
     * Generates a bibliography list based on a given list of entries, .csl style source content and output format with a given {@link BibDatabaseContext}.
     *
     * @implNote The bibliographies are generated using an external library which may take some time, debatable if it is better to call it from outside the main Thread.
     */
    public static List<String> generateBibliography(List<BibEntry> bibEntries, String style, CitationStyleOutputFormat outputFormat, BibDatabaseContext databaseContext, BibEntryTypesManager entryTypesManager) {
        try {
            return CSL_ADAPTER.makeBibliography(bibEntries, style, outputFormat, databaseContext, entryTypesManager);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Could not generate BibEntry bibliography. The CSL engine could not create a bibliography output for your item.", e);
            return List.of(Localization.lang("Cannot generate bibliography based on selected citation style."));
        } catch (IOException | ArrayIndexOutOfBoundsException e) {
            LOGGER.error("Could not generate BibEntry bibliography", e);
            return List.of(Localization.lang("Cannot generate bibliography based on selected citation style."));
        } catch (TokenMgrException e) {
            LOGGER.error("Bad character inside BibEntry", e);
            // sadly one cannot easily retrieve the bad char from the TokenMgrError
            return List.of(Localization.lang("Cannot generate bibliography based on selected citation style.") +
                    outputFormat.getLineSeparator() +
                    Localization.lang("Bad character inside entry") +
                    outputFormat.getLineSeparator() +
                    e.getLocalizedMessage());
        }
    }
}

package org.jabref.logic.openoffice.oocsltext;

import java.util.List;

import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.openoffice.CSLBibliographyMark;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.openoffice.uno.CreationException;
import org.jabref.model.openoffice.uno.NoDocumentException;

import com.sun.star.text.XTextDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CSLUpdateBibliography {

    private static final Logger LOGGER = LoggerFactory.getLogger(CSLUpdateBibliography.class);

    private final OpenOfficePreferences openOfficePreferences;

    public CSLUpdateBibliography() {
        this(OpenOfficePreferences.getDefault());
    }

    public CSLUpdateBibliography(OpenOfficePreferences openOfficePreferences) {
        this.openOfficePreferences = openOfficePreferences;
    }

    /// Rebuilds the bibliography using CSL.
    public void rebuildCSLBibliography(XTextDocument doc,
                                       CSLCitationOOAdapter cslCitationOOAdapter,
                                       List<BibEntry> entries,
                                       CitationStyle citationStyle,
                                       BibDatabaseContext bibDatabaseContext,
                                       BibEntryTypesManager bibEntryTypesManager)
            throws com.sun.star.uno.Exception, NoDocumentException, CreationException {
        LOGGER.debug("Starting to rebuild CSL bibliography");

        CSLBibliographyMark.rebuildCSLBibliography(
                doc,
                cslCitationOOAdapter,
                entries,
                citationStyle,
                bibDatabaseContext,
                bibEntryTypesManager,
                openOfficePreferences.getReferenceMarkFormat());

        LOGGER.debug("Finished rebuilding CSL bibliography");
    }
}

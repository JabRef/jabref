package org.jabref.logic.openoffice.oocsltext;

import java.util.List;

import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.openoffice.JabRefBibliographyMark;
import org.jabref.logic.openoffice.ZoteroBibliographyMark;
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

    /// Rebuilds the bibliography using CSL.
    public void rebuildCSLBibliography(XTextDocument doc,
                                       CSLCitationOOAdapter cslCitationOOAdapter,
                                       List<BibEntry> entries,
                                       CitationStyle citationStyle,
                                       BibDatabaseContext bibDatabaseContext,
                                       BibEntryTypesManager bibEntryTypesManager)
            throws com.sun.star.uno.Exception, NoDocumentException, CreationException {
        LOGGER.debug("Starting to rebuild CSL bibliography");

        // TODO: Implement preference. For now, only allow Zotero Style
        boolean useZoteroBibliography = true;
        if (useZoteroBibliography) {
            new ZoteroBibliographyMark().rebuildCSLBibliography(doc, cslCitationOOAdapter, entries, citationStyle, bibDatabaseContext, bibEntryTypesManager);
        } else {
            new JabRefBibliographyMark().rebuildCSLBibliography(doc, cslCitationOOAdapter, entries, citationStyle, bibDatabaseContext, bibEntryTypesManager);
        }

        LOGGER.debug("Finished rebuilding CSL bibliography");
    }
}

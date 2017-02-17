package org.jabref.gui.importer.fetcher;

import java.util.Map;

import org.jabref.gui.importer.FetcherPreviewDialog;
import org.jabref.logic.importer.ImportInspector;
import org.jabref.logic.importer.OutputPrinter;

/**
 *
 */
public interface PreviewEntryFetcher extends EntryFetcher {

    boolean processQueryGetPreview(String query, FetcherPreviewDialog preview,
                                   OutputPrinter status);

    void getEntries(Map<String, Boolean> selection, ImportInspector inspector);

    /**
     * The number of entries a user can select for download without getting a warning message.
     * @return the warning limit
     */
    int getWarningLimit();

    /**
     * The preferred table row height for the previews.
     * @return the preferred height
     */
    int getPreferredPreviewHeight();

}

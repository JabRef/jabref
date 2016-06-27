package net.sf.jabref.importer.fetcher;

import java.util.Map;

import net.sf.jabref.importer.ImportInspector;

public interface PreviewEntryFetcher extends EntryFetcher {

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

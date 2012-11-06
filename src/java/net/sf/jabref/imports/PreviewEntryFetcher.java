package net.sf.jabref.imports;

import net.sf.jabref.OutputPrinter;
import net.sf.jabref.gui.FetcherPreviewDialog;

import javax.swing.*;
import java.util.Map;

/**
 *
 */
public interface PreviewEntryFetcher extends EntryFetcher {

    public boolean processQueryGetPreview(String query, FetcherPreviewDialog preview,
                                                      OutputPrinter status);

    public void getEntries(Map<String, Boolean> selection, ImportInspector inspector);

    /**
     * The number of entries a user can select for download without getting a warning message.
     * @return the warning limit
     */
    public int getWarningLimit();

    /**
     * The preferred table row height for the previews.
     * @return the preferred height
     */
    public int getPreferredPreviewHeight();

}

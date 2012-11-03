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

}

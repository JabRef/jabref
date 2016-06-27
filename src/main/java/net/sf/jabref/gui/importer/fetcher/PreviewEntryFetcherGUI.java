package net.sf.jabref.gui.importer.fetcher;

import net.sf.jabref.gui.FetcherPreviewDialog;
import net.sf.jabref.importer.OutputPrinter;
import net.sf.jabref.importer.fetcher.PreviewEntryFetcher;

public interface PreviewEntryFetcherGUI extends PreviewEntryFetcher, EntryFetcherGUI {

    boolean processQueryGetPreview(String query, FetcherPreviewDialog preview, OutputPrinter status);

}

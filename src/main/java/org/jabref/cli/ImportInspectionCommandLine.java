package org.jabref.cli;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.jabref.gui.importer.fetcher.EntryFetcher;
import org.jabref.logic.importer.ImportInspector;
import org.jabref.logic.importer.OutputPrinter;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;

public class ImportInspectionCommandLine implements ImportInspector {

    private final List<BibEntry> entries = new LinkedList<>();

    private final OutputPrinter status = new SystemOutputPrinter();

    @Override
    public void addEntry(BibEntry entry) {
        entries.add(entry);
    }

    @Override
    public void setProgress(int current, int max) {
        status.setStatus(Localization.lang("Progress: %0 of %1", String.valueOf(current), String
                .valueOf(max)));
    }

    public Collection<BibEntry> query(String query, EntryFetcher fetcher) {
        entries.clear();
        if (fetcher.processQuery(query, ImportInspectionCommandLine.this, status)) {
            return entries;
        }
        return Collections.emptyList();
    }
}

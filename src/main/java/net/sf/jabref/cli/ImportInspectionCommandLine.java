package net.sf.jabref.cli;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import net.sf.jabref.gui.importer.fetcher.EntryFetcher;
import net.sf.jabref.logic.importer.ImportInspector;
import net.sf.jabref.logic.importer.OutputPrinter;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;

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

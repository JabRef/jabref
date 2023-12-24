package org.jabref.gui.entryeditor.citationrelationtab;

import java.util.List;

import org.jabref.model.entry.BibEntry;

public interface RelatedEntriesFetcher {
   List<BibEntry> fetch(BibEntry entry);
}

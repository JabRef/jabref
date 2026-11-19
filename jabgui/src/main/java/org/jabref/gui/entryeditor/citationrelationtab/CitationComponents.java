package org.jabref.gui.entryeditor.citationrelationtab;

import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SplitMenuButton;

import org.jabref.logic.importer.fetcher.citation.CitationFetcher;
import org.jabref.model.entry.BibEntry;

import org.controlsfx.control.CheckListView;

public record CitationComponents(
        BibEntry entry,
        CheckListView<CitationRelationItem> listView,
        Button abortButton,
        Button refreshButton,
        CitationFetcher.SearchType searchType,
        SplitMenuButton importButton,
        MenuItem reviewMenuItem,
        ProgressIndicator progress) {
}

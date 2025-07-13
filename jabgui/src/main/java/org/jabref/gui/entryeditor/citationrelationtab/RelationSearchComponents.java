package org.jabref.gui.entryeditor.citationrelationtab;

import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;

import org.jabref.logic.importer.fetcher.citation.CitationFetcher;
import org.jabref.model.entry.BibEntry;

import org.controlsfx.control.CheckListView;

public record RelationSearchComponents(
        BibEntry entry,
        CheckListView<CitationRelationItem> listView,
        CheckListView<CitationRelationItem> otherListView,
        Button abortButton,
        Button refreshButton,
        CitationFetcher.SearchType searchType,
        Button importButton,
        ProgressIndicator progress) {
}

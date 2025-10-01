package org.jabref.gui.util;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

import org.jabref.logic.importer.SearchBasedFetcher;

public class ResearchTrackPaneViewModel {
    private final ListProperty<SearchBasedFetcher> fetchers = new SimpleListProperty<>(FXCollections.observableArrayList());

    private final StringProperty researchTitle = new SimpleStringProperty();
    private final StringProperty researchUrl = new SimpleStringProperty();
    private final StringProperty researchNotes = new SimpleStringProperty();

    private final
}

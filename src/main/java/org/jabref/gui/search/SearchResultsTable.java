package org.jabref.gui.search;

import javax.swing.undo.UndoManager;

import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableView;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.maintable.BibEntryTableViewModel;
import org.jabref.gui.maintable.MainTable;
import org.jabref.gui.maintable.MainTableColumnFactory;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.PreferencesService;

public class SearchResultsTable extends TableView<BibEntryTableViewModel> {

    public SearchResultsTable(SearchResultsTableDataModel model,
                              BibDatabaseContext database,
                              PreferencesService preferencesService,
                              UndoManager undoManager,
                              DialogService dialogService,
                              StateManager stateManager,
                              ExternalFileTypes externalFileTypes) {
        super();

        this.getColumns().addAll(new MainTableColumnFactory(
                database,
                preferencesService,
                externalFileTypes,
                undoManager,
                dialogService,
                stateManager,
                false).createColumns());

        this.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        this.setItems(model.getEntriesFilteredAndSorted());
        // Enable sorting
        model.getEntriesFilteredAndSorted().comparatorProperty().bind(this.comparatorProperty());

        this.getStylesheets().add(MainTable.class.getResource("MainTable.css").toExternalForm());
        database.getDatabase().registerListener(this);
    }
}


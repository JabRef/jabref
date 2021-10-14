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
import org.jabref.gui.maintable.MainTablePreferences;
import org.jabref.gui.maintable.SmartConstrainedResizePolicy;
import org.jabref.gui.maintable.columns.MainTableColumn;
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

        MainTablePreferences mainTablePreferences = preferencesService.getMainTablePreferences();

        this.getColumns().addAll(new MainTableColumnFactory(
                database,
                preferencesService,
                externalFileTypes,
                undoManager,
                dialogService,
                stateManager).createColumns());

        this.getSortOrder().clear();
        mainTablePreferences.getColumnPreferences().getColumnSortOrder().forEach(columnModel ->
                this.getColumns().stream()
                    .map(column -> (MainTableColumn<?>) column)
                    .filter(column -> column.getModel().equals(columnModel))
                    .findFirst()
                    .ifPresent(column -> this.getSortOrder().add(column)));

        if (mainTablePreferences.getResizeColumnsToFit()) {
            this.setColumnResizePolicy(new SmartConstrainedResizePolicy());
        }

        this.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        this.setItems(model.getEntriesFilteredAndSorted());
        // Enable sorting
        model.getEntriesFilteredAndSorted().comparatorProperty().bind(this.comparatorProperty());

        this.getStylesheets().add(MainTable.class.getResource("MainTable.css").toExternalForm());
        database.getDatabase().registerListener(this);
    }
}


package org.jabref.gui.search;

import java.util.List;

import javax.swing.undo.UndoManager;

import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import org.jabref.architecture.AllowedToUseClassGetResource;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.maintable.BibEntryTableViewModel;
import org.jabref.gui.maintable.MainTable;
import org.jabref.gui.maintable.MainTableColumnFactory;
import org.jabref.gui.maintable.MainTablePreferences;
import org.jabref.gui.maintable.PersistenceVisualStateTable;
import org.jabref.gui.maintable.SmartConstrainedResizePolicy;
import org.jabref.gui.maintable.columns.LibraryColumn;
import org.jabref.gui.maintable.columns.MainTableColumn;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;

@AllowedToUseClassGetResource("JavaFX internally handles the passed URLs properly.")
public class SearchResultsTable extends TableView<BibEntryTableViewModel> {

    public SearchResultsTable(SearchResultsTableDataModel model,
                              BibDatabaseContext database,
                              GuiPreferences preferences,
                              UndoManager undoManager,
                              DialogService dialogService,
                              StateManager stateManager,
                              TaskExecutor taskExecutor) {
        super();

        MainTablePreferences mainTablePreferences = preferences.getMainTablePreferences();

        List<TableColumn<BibEntryTableViewModel, ?>> allCols = new MainTableColumnFactory(
                database,
                preferences,
                preferences.getSearchDialogColumnPreferences(),
                undoManager,
                dialogService,
                stateManager,
                taskExecutor).createColumns();

        if (allCols.stream().noneMatch(LibraryColumn.class::isInstance)) {
            allCols.addFirst(new LibraryColumn());
        }
        this.getColumns().addAll(allCols);

        this.getSortOrder().clear();
        preferences.getSearchDialogColumnPreferences().getColumnSortOrder().forEach(columnModel ->
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

        // Store visual state
        new PersistenceVisualStateTable(this, preferences.getSearchDialogColumnPreferences()).addListeners();

        database.getDatabase().registerListener(this);
    }
}


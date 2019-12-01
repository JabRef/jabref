package org.jabref.gui.maintable;

import java.util.stream.Collectors;

import javafx.collections.ListChangeListener;
import javafx.scene.control.TableColumn;

import org.jabref.preferences.JabRefPreferences;

/**
 * Keep track of changes made to the columns, like reordering or resizing.
 *
 */
public class PersistenceVisualStateTable {

    private final MainTable mainTable;
    private final JabRefPreferences preferences;

    public PersistenceVisualStateTable(final MainTable mainTable, JabRefPreferences preferences) {
        this.mainTable = mainTable;
        this.preferences = preferences;

        mainTable.getColumns().addListener(this::onColumnsChanged);
        mainTable.getSortOrder().addListener(this::onColumnsChanged);
        mainTable.getColumns().forEach(col -> col.widthProperty().addListener(obs -> updateColumnPreferences()));
        mainTable.getColumns().forEach(col -> col.sortTypeProperty().addListener(obs -> updateColumnPreferences()));
    }

    private void onColumnsChanged(ListChangeListener.Change<? extends TableColumn<BibEntryTableViewModel, ?>> change) {
        boolean changed = false;
        while (change.next()) {
            changed = true;
        }

        if (changed) {
            updateColumnPreferences();
        }
    }

    /**
     * Store shown columns, their width and their sortType in preferences.
     */
    private void updateColumnPreferences() {
        preferences.storeColumnPreferences(new ColumnPreferences(
                mainTable.getColumns().stream()
                        .map(column -> ((MainTableColumn<?>) column).getModel())
                        .collect(Collectors.toList()),
                mainTable.getSortOrder().stream()
                        .map(column -> ((MainTableColumn<?>) column).getModel())
                        .collect(Collectors.toList())
        ));
    }
}

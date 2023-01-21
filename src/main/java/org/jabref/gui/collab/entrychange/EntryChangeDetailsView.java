package org.jabref.gui.collab.entrychange;

import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.collab.DatabaseChangeDetailsView;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.preferences.PreferencesService;

import com.tobiasdiez.easybind.EasyBind;

public final class EntryChangeDetailsView extends DatabaseChangeDetailsView {
    private final PreviewWithSourceTab oldPreviewWithSourcesTab = new PreviewWithSourceTab();
    private final PreviewWithSourceTab newPreviewWithSourcesTab = new PreviewWithSourceTab();

    public EntryChangeDetailsView(BibEntry oldEntry, BibEntry newEntry, BibDatabaseContext databaseContext, DialogService dialogService, StateManager stateManager, ThemeManager themeManager, PreferencesService preferencesService, BibEntryTypesManager entryTypesManager) {
        TabPane oldEntryTabPane = oldPreviewWithSourcesTab.getPreviewWithSourceTab(oldEntry, databaseContext, dialogService, stateManager, themeManager, preferencesService, entryTypesManager);
        TabPane newEntryTabPane = newPreviewWithSourcesTab.getPreviewWithSourceTab(newEntry, databaseContext, dialogService, stateManager, themeManager, preferencesService, entryTypesManager);

        EasyBind.subscribe(oldEntryTabPane.getSelectionModel().selectedIndexProperty(), selectedIndex -> {
            newEntryTabPane.getSelectionModel().select(selectedIndex.intValue());
        });

        EasyBind.subscribe(newEntryTabPane.getSelectionModel().selectedIndexProperty(), selectedIndex -> {
            if (oldEntryTabPane.getSelectionModel().getSelectedIndex() != selectedIndex.intValue()) {
                oldEntryTabPane.getSelectionModel().select(selectedIndex.intValue());
            }
        });

        SplitPane split = new SplitPane(oldEntryTabPane, newEntryTabPane);
        split.setOrientation(Orientation.HORIZONTAL);

        setLeftAnchor(split, 8d);
        setTopAnchor(split, 8d);
        setRightAnchor(split, 8d);
        setBottomAnchor(split, 8d);
        this.getChildren().add(split);
    }
}

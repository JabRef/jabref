package org.jabref.gui.collab.entrychange;

import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.collab.DatabaseChangeDetailsView;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preview.PreviewViewer;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

import com.tobiasdiez.easybind.EasyBind;

public final class EntryChangeDetailsView extends DatabaseChangeDetailsView {

    public EntryChangeDetailsView(BibEntry oldEntry,
                                  BibEntry newEntry,
                                  BibDatabaseContext databaseContext,
                                  DialogService dialogService,
                                  GuiPreferences preferences,
                                  BibEntryTypesManager entryTypesManager,
                                  PreviewViewer previewViewer,
                                  TaskExecutor taskExecutor) {
        Label inJabRef = new Label(Localization.lang("In JabRef"));
        inJabRef.getStyleClass().add("lib-change-header");
        Label onDisk = new Label(Localization.lang("On disk"));
        onDisk.getStyleClass().add("lib-change-header");

        // we need a copy here as we otherwise would set the same entry twice
        PreviewViewer previewClone = new PreviewViewer(dialogService, preferences, taskExecutor);
        previewClone.setDatabaseContext(databaseContext);

        // PreviewViewer is a plain ScrollPane now, so its own scroll bars can be synchronized directly
        previewClone.vvalueProperty().bindBidirectional(previewViewer.vvalueProperty());
        // TODO: Also sync scrolling for BibTeX view.

        PreviewWithSourceTab oldPreviewWithSourcesTab = new PreviewWithSourceTab();
        TabPane oldEntryTabPane = oldPreviewWithSourcesTab.getPreviewWithSourceTab(oldEntry, databaseContext, preferences, entryTypesManager, previewClone, Localization.lang("Entry Preview"));
        PreviewWithSourceTab newPreviewWithSourcesTab = new PreviewWithSourceTab();
        TabPane newEntryTabPane = newPreviewWithSourcesTab.getPreviewWithSourceTab(newEntry, databaseContext, preferences, entryTypesManager, previewViewer, Localization.lang("Entry Preview"));

        EasyBind.subscribe(
                oldEntryTabPane.getSelectionModel().selectedIndexProperty(),
                selectedIndex -> newEntryTabPane.getSelectionModel().select(selectedIndex.intValue()));

        EasyBind.subscribe(newEntryTabPane.getSelectionModel().selectedIndexProperty(), selectedIndex -> {
            if (oldEntryTabPane.getSelectionModel().getSelectedIndex() != selectedIndex.intValue()) {
                oldEntryTabPane.getSelectionModel().select(selectedIndex.intValue());
            }
        });

        VBox containerOld = new VBox(inJabRef, oldEntryTabPane);
        VBox containerNew = new VBox(onDisk, newEntryTabPane);

        SplitPane split = new SplitPane(containerOld, containerNew);
        split.setOrientation(Orientation.HORIZONTAL);

        this.setAllAnchorsAndAttachChild(split);
    }
}

package org.jabref.gui.externalfiles;

import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import org.jabref.gui.StateManager;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.FileNodeViewModel;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;

public class UnlinkedFilesCellFactory extends CheckBoxTreeCell<FileNodeViewModel> {
    private final ComboBox<BibEntry> relatedEntries = new ComboBox<>();
    private final Button jumpToEntryButton = new Button();
    private final HBox cellContent = new HBox();
    private final HBox leftSide = new HBox();
    private final HBox jumpIcon = new HBox();
    private final Button doNotLinkButton = new Button();

    private final StateManager stateManager;
    private final UnlinkedFilesDialogViewModel viewModel;

    public UnlinkedFilesCellFactory(StateManager stateManager,
                                    UnlinkedFilesDialogViewModel viewModel) {
        this.stateManager = stateManager;
        this.viewModel = viewModel;
        cellContent.setSpacing(10);
        leftSide.setSpacing(5);
        jumpIcon.setSpacing(5);

        new ViewModelListCellFactory<BibEntry>()
                .withText(entry -> entry.getCitationKey().orElse(Localization.lang("new")))
                .install(relatedEntries);
        HBox.setHgrow(leftSide, Priority.ALWAYS);
        relatedEntries.setPrefWidth(200);
        relatedEntries.setTooltip(new Tooltip(Localization.lang("Select an existing entry to link the file to")));

        jumpToEntryButton.setGraphic(IconTheme.JabRefIcons.SEARCH.getGraphicNode());
        jumpToEntryButton.getStyleClass().add("icon-button");
        jumpToEntryButton.setTooltip(new Tooltip(Localization.lang("Jump to entry")));
        jumpToEntryButton.setOnAction(_ -> {
            BibEntry selectedEntry = relatedEntries.getValue();
            // If the user didn't select an entry from the dropdown but still clicks the jump button
            if (selectedEntry != null) {
                stateManager.activeTabProperty().get().ifPresent(tab -> {
                    tab.clearAndSelect(selectedEntry);
                    tab.showAndEdit(selectedEntry);
                });
            }
        });

        doNotLinkButton.setText(Localization.lang("Do not link"));
        doNotLinkButton.getStyleClass().add("icon-button");
        doNotLinkButton.setTooltip(new Tooltip(Localization.lang("Import the file in a new entry")));
        doNotLinkButton.setOnAction(_ -> {
            relatedEntries.getSelectionModel().clearSelection();
            relatedEntries.setPromptText(Localization.lang("Select entry to link"));
            viewModel.setSelectedEntryForFile(getTreeItem().getValue().getPath(), null);
        });

        relatedEntries.valueProperty().addListener((_, _, newEntry) -> {
            viewModel.setSelectedEntryForFile(
                    getTreeItem().getValue().getPath(),
                    newEntry
            );
        });
    }

    @Override
    public void updateItem(FileNodeViewModel item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setText(null);
            setGraphic(null);
            return;
        }
        leftSide.getChildren().clear();
        CheckBox checkBox = (CheckBox) getGraphic();
        Label fileNameLabel = new Label(item.getDisplayText());
        leftSide.getChildren().addAll(checkBox, fileNameLabel);
        cellContent.getChildren().clear();
        cellContent.getChildren().add(leftSide);

        if (!item.getPath().toFile().isFile()) {
            setGraphic(cellContent);
            setText(null);
            return;
        }
        ObservableList<BibEntry> fileRelatedEntries =
                viewModel.getRelatedEntriesForFiles(item.getPath());

        if (!fileRelatedEntries.isEmpty()) {
            relatedEntries.setPromptText(Localization.lang("Select entry to link"));
            relatedEntries.setItems(fileRelatedEntries);
            jumpIcon.getChildren().clear();
            jumpIcon.getChildren().addAll(relatedEntries, jumpToEntryButton, doNotLinkButton);
            cellContent.getChildren().add(jumpIcon);
        }
        setGraphic(cellContent);
        // The text needs to be set to null,
        // otherwise the CheckBoxTreeCell will show the file name twice
        // (once from the label in the graphic and once as text of the cell).
        setText(null);
    }
}

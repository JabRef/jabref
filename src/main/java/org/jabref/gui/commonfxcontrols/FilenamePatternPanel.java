package org.jabref.gui.commonfxcontrols;

import java.util.Collection;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyEvent;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.logic.filenameformatpatterns.AbstractFilenameFormatPatterns;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.types.EntryType;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

public class FilenamePatternPanel extends TableView<FilenamePatternItemModel> {

    @FXML public TableColumn<FilenamePatternItemModel, EntryType> entryTypeColumn;
    @FXML public TableColumn<FilenamePatternItemModel, String> patternColumn;
    @FXML public TableColumn<FilenamePatternItemModel, EntryType> actionsColumn;

    @Inject private CliPreferences preferences;

    private FilenamePatternPanelViewModel viewModel;

    private long lastKeyPressTime;
    private String tableSearchTerm;

    public FilenamePatternPanel() {
        super();

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void initialize() {
        viewModel = new FilenamePatternPanelViewModel(preferences.getFilePreferences());

        this.setEditable(true);

        entryTypeColumn.setSortable(true);
        entryTypeColumn.setReorderable(false);
        entryTypeColumn.setCellValueFactory(cellData -> cellData.getValue().entryType());
        new ValueTableCellFactory<FilenamePatternItemModel, EntryType>()
                .withText(EntryType::getDisplayName)
                .install(entryTypeColumn);
        this.setOnSort(event ->
                viewModel.patternListProperty().sort(FilenamePatternPanelViewModel.defaultOnTopComparator));

        patternColumn.setSortable(true);
        patternColumn.setReorderable(false);
        patternColumn.setCellValueFactory(cellData -> cellData.getValue().pattern());
        patternColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        patternColumn.setEditable(true);
        patternColumn.setOnEditCommit(
                (TableColumn.CellEditEvent<FilenamePatternItemModel, String> event) ->
                        event.getRowValue().setPattern(event.getNewValue()));

        actionsColumn.setSortable(false);
        actionsColumn.setReorderable(false);
        actionsColumn.setCellValueFactory(cellData -> cellData.getValue().entryType());
        new ValueTableCellFactory<FilenamePatternItemModel, EntryType>()
                .withGraphic(entryType -> IconTheme.JabRefIcons.REFRESH.getGraphicNode())
                .withTooltip(entryType ->
                        Localization.lang("Reset %s to default value").formatted(entryType.getDisplayName()))
                .withOnMouseClickedEvent(item -> evt ->
                        viewModel.setItemToDefaultPattern(this.getFocusModel().getFocusedItem()))
                .install(actionsColumn);

        this.setRowFactory(item -> new HighlightTableRow());
        this.setOnKeyTyped(this::jumpToSearchKey);
        this.itemsProperty().bindBidirectional(viewModel.patternListProperty());
    }

    public void setValues(Collection<BibEntryType> entryTypeList, AbstractFilenameFormatPatterns keyPattern) {
        viewModel.setValues(entryTypeList, keyPattern);
    }

    public void resetAll() {
        viewModel.resetAll();
    }

    public ListProperty<FilenamePatternItemModel> patternListProperty() {
        return viewModel.patternListProperty();
    }

    public ObjectProperty<FilenamePatternItemModel> defaultKeyPatternProperty() {
        return viewModel.defaultKeyPatternProperty();
    }

    private void jumpToSearchKey(KeyEvent keypressed) {
        if (keypressed.getCharacter() == null) {
            return;
        }

        if (System.currentTimeMillis() - lastKeyPressTime < 1000) {
            tableSearchTerm += keypressed.getCharacter().toLowerCase();
        } else {
            tableSearchTerm = keypressed.getCharacter().toLowerCase();
        }

        lastKeyPressTime = System.currentTimeMillis();

        this.getItems().stream().filter(item -> item.getEntryType().getName().toLowerCase().startsWith(tableSearchTerm))
            .findFirst().ifPresent(this::scrollTo);
    }

    private static class HighlightTableRow extends TableRow<FilenamePatternItemModel> {
        @Override
        public void updateItem(FilenamePatternItemModel item, boolean empty) {
            super.updateItem(item, empty);
            if (item == null || item.getEntryType() == null) {
                setStyle("");
            } else if (isSelected()) {
                setStyle("-fx-background-color: -fx-selection-bar");
            } else if (item.getEntryType().getName().equals(FilenamePatternPanelViewModel.ENTRY_TYPE_DEFAULT_NAME)) {
                setStyle("-fx-background-color: -fx-default-button");
            } else {
                setStyle("");
            }
        }
    }
}

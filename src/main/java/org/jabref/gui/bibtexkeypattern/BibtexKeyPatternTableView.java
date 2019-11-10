package org.jabref.gui.bibtexkeypattern;

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
import org.jabref.logic.l10n.Localization;
import org.jabref.model.bibtexkeypattern.AbstractBibtexKeyPattern;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.types.EntryType;
import org.jabref.preferences.JabRefPreferences;

import com.airhacks.afterburner.views.ViewLoader;

public class BibtexKeyPatternTableView extends TableView<BibtexKeyPatternTableItemModel> {

    @FXML public TableColumn<BibtexKeyPatternTableItemModel, EntryType> entryTypeColumn;
    @FXML public TableColumn<BibtexKeyPatternTableItemModel, String> patternColumn;
    @FXML public TableColumn<BibtexKeyPatternTableItemModel, EntryType> actionsColumn;

    private BibtexKeyPatternTableViewModel viewModel;

    private long lastKeyPressTime;
    private String tableSearchTerm;

    public BibtexKeyPatternTableView(JabRefPreferences preferences, Collection<BibEntryType> entryTypeList, AbstractBibtexKeyPattern keyPattern) {
        super();

        viewModel = new BibtexKeyPatternTableViewModel(preferences, entryTypeList, keyPattern);

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void initialize() {
        this.setEditable(true);

        entryTypeColumn.setSortable(true);
        entryTypeColumn.setReorderable(false);
        entryTypeColumn.setCellValueFactory(cellData -> cellData.getValue().entryType());
        new ValueTableCellFactory<BibtexKeyPatternTableItemModel, EntryType>()
                .withText(EntryType::getDisplayName)
                .install(entryTypeColumn);
        this.setOnSort(event ->
                viewModel.patternListProperty().sort(BibtexKeyPatternTableViewModel.defaultOnTopComparator));

        patternColumn.setSortable(true);
        patternColumn.setReorderable(false);
        patternColumn.setCellValueFactory(cellData -> cellData.getValue().pattern());
        patternColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        patternColumn.setEditable(true);
        patternColumn.setOnEditCommit(
                (TableColumn.CellEditEvent<BibtexKeyPatternTableItemModel, String> event) ->
                        event.getRowValue().setPattern(event.getNewValue()));

        actionsColumn.setSortable(false);
        actionsColumn.setReorderable(false);
        actionsColumn.setCellValueFactory(cellData -> cellData.getValue().entryType());
        new ValueTableCellFactory<BibtexKeyPatternTableItemModel, EntryType>()
                .withGraphic(entryType -> IconTheme.JabRefIcons.REFRESH.getGraphicNode())
                .withTooltip(entryType ->
                        String.format(Localization.lang("Reset %s to default value"), entryType.getDisplayName()))
                .withOnMouseClickedEvent(item -> evt ->
                        viewModel.setItemToDefaultPattern(this.getFocusModel().getFocusedItem()))
                .install(actionsColumn);

        this.setRowFactory(item -> new HighlightTableRow());
        this.setOnKeyTyped(this::jumpToSearchKey);
        this.itemsProperty().bindBidirectional(viewModel.patternListProperty());
    }

    public void setValues() { viewModel.setValues(); }

    public void resetAll() { viewModel.resetAll(); }

    public ListProperty<BibtexKeyPatternTableItemModel> patternListProperty() { return viewModel.patternListProperty(); }

    public ObjectProperty<BibtexKeyPatternTableItemModel> defaultKeyPatternProperty() { return viewModel.defaultKeyPatternProperty(); }

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

    private static class HighlightTableRow extends TableRow<BibtexKeyPatternTableItemModel> {
        @Override
        public void updateItem(BibtexKeyPatternTableItemModel item, boolean empty) {
            super.updateItem(item, empty);
            if (item == null || item.getEntryType() == null) {
                setStyle("");
            } else if (isSelected()) {
                setStyle("-fx-background-color: -fx-selection-bar");
            } else if (item.getEntryType().getName().equals(BibtexKeyPatternTableViewModel.ENTRY_TYPE_DEFAULT_NAME)) {
                setStyle("-fx-background-color: -fx-default-button");
            } else {
                setStyle("");
            }
        }
    }
}

package org.jabref.gui.linkedfile;

import java.util.Collection;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyEvent;

import org.jabref.gui.commonfxcontrols.PatternsItemModel;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.logic.citationkeypattern.Pattern;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.linkedfile.AbstractLinkedFileNamePatterns;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.types.EntryType;

import com.airhacks.afterburner.views.ViewLoader;

public class LinkedFileNamePatternsPanel extends TableView<PatternsItemModel> {

    @FXML public TableColumn<PatternsItemModel, EntryType> entryTypeColumn;
    @FXML public TableColumn<PatternsItemModel, String> patternColumn;
    @FXML public TableColumn<PatternsItemModel, EntryType> actionsColumn;

    private LinkedFileNamePatternsPanelViewModel viewModel;

    private long lastKeyPressTime;
    private String tableSearchTerm;
    private final ObservableList<String> patterns;

    public LinkedFileNamePatternsPanel() {
        super();
        this.patterns = FXCollections.observableArrayList(
                Pattern.getAllPatterns().stream()
                       .map(Pattern::stringRepresentation)
                       .toList()
        );

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void initialize() {
        viewModel = new LinkedFileNamePatternsPanelViewModel();

        this.setEditable(true);

        entryTypeColumn.setSortable(true);
        entryTypeColumn.setReorderable(false);
        entryTypeColumn.setCellValueFactory(cellData -> cellData.getValue().entryType());
        new ValueTableCellFactory<PatternsItemModel, EntryType>()
                .withText(EntryType::getDisplayName)
                .install(entryTypeColumn);
        this.setOnSort(event ->
                viewModel.patternListProperty().sort(LinkedFileNamePatternsPanelViewModel.defaultOnTopComparator));

        patternColumn.setSortable(true);
        patternColumn.setReorderable(false);
        patternColumn.setCellValueFactory(cellData -> cellData.getValue().pattern());
        patternColumn.setCellFactory(a -> new LinkedFileNamePatternsSuggestionCell(patterns));
        patternColumn.setEditable(true);
        patternColumn.setOnEditCommit(
                (TableColumn.CellEditEvent<PatternsItemModel, String> event) ->
                        event.getRowValue().setPattern(event.getNewValue()));

        actionsColumn.setSortable(false);
        actionsColumn.setReorderable(false);
        actionsColumn.setCellValueFactory(cellData -> cellData.getValue().entryType());
        new ValueTableCellFactory<PatternsItemModel, EntryType>()
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

    public void setValues(Collection<BibEntryType> entryTypeList, AbstractLinkedFileNamePatterns keyPattern) {
        viewModel.setValues(entryTypeList, keyPattern);
    }

    public void resetAll() {
        viewModel.resetAll();
    }

    public ListProperty<PatternsItemModel> patternListProperty() {
        return viewModel.patternListProperty();
    }

    public ObjectProperty<PatternsItemModel> defaultNamePatternProperty() {
        return viewModel.defaultNamePatternProperty();
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

    private static class HighlightTableRow extends TableRow<PatternsItemModel> {
        @Override
        public void updateItem(PatternsItemModel item, boolean empty) {
            super.updateItem(item, empty);
            if (item == null || item.getEntryType() == null) {
                setStyle("");
            } else if (isSelected()) {
                setStyle("-fx-background-color: -fx-selection-bar");
            } else if (LinkedFileNamePatternsPanelViewModel.ENTRY_TYPE_DEFAULT_NAME.equals(item.getEntryType().getName())) {
                setStyle("-fx-background-color: -fx-default-button");
            } else {
                setStyle("");
            }
        }
    }
}

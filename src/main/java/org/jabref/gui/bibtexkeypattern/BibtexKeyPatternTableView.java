package org.jabref.gui.bibtexkeypattern;

import java.util.Collection;

import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;

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
                .withTooltip(entryType -> Localization.lang("Reset %s to default value", entryType.getDisplayName()))
                .withOnMouseClickedEvent(item -> evt ->
                        viewModel.setItemToDefaultPattern(this.getFocusModel().getFocusedItem()))
                .install(actionsColumn);

        this.itemsProperty().bindBidirectional(viewModel.patternListProperty());
    }

    public void setValues() { viewModel.setValues(); }

    public AbstractBibtexKeyPattern getKeyPattern() { return viewModel.getKeyPattern(); }

    public void setDefaultPattern(String pattern) { viewModel.setDefaultPattern(pattern); }

    public String getDefaultPattern() { return viewModel.getDefaultPattern(); }
}

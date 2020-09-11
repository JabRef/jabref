package org.jabref.gui.edit;

import java.util.List;

import javax.inject.Inject;

import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.TextFieldTableCell;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;

public class ManageKeywordsDialog extends BaseDialog<Void> {
    private final List<BibEntry> entries;
    @FXML private TableColumn<String, String> keywordsTableMainColumn;
    @FXML private TableColumn<String, Boolean> keywordsTableEditColumn;
    @FXML private TableColumn<String, Boolean> keywordsTableDeleteColumn;
    @FXML private TableView<String> keywordsTable;
    @FXML private ToggleGroup displayType;
    @Inject private PreferencesService preferences;
    private ManageKeywordsViewModel viewModel;

    public ManageKeywordsDialog(List<BibEntry> entries) {
        this.entries = entries;
        this.setTitle(Localization.lang("Manage keywords"));

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        setResultConverter(button -> {
            if (button == ButtonType.APPLY) {
                viewModel.saveChanges();
            }
            return null;
        });
    }

    @FXML
    public void initialize() {
        viewModel = new ManageKeywordsViewModel(preferences, entries);

        viewModel.displayTypeProperty().bind(
                EasyBind.map(displayType.selectedToggleProperty(), toggle -> {
                    if (toggle != null) {
                        return (ManageKeywordsDisplayType) toggle.getUserData();
                    } else {
                        return ManageKeywordsDisplayType.CONTAINED_IN_ALL_ENTRIES;
                    }
                })
        );

        keywordsTable.setItems(viewModel.getKeywords());
        keywordsTableMainColumn.setCellValueFactory(data -> BindingsHelper.constantOf(data.getValue()));
        keywordsTableMainColumn.setOnEditCommit(event -> {
            // Poor mans reverse databinding (necessary because we use a constant value above)
            viewModel.getKeywords().set(event.getTablePosition().getRow(), event.getNewValue());
        });
        keywordsTableMainColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        keywordsTableEditColumn.setCellValueFactory(data -> BindingsHelper.constantOf(true));
        keywordsTableDeleteColumn.setCellValueFactory(data -> BindingsHelper.constantOf(true));
        new ValueTableCellFactory<String, Boolean>()
                .withGraphic(none -> IconTheme.JabRefIcons.EDIT.getGraphicNode())
                .withOnMouseClickedEvent(none -> event -> keywordsTable.edit(keywordsTable.getFocusModel().getFocusedIndex(), keywordsTableMainColumn))
                .install(keywordsTableEditColumn);
        new ValueTableCellFactory<String, Boolean>()
                .withGraphic(none -> IconTheme.JabRefIcons.REMOVE.getGraphicNode())
                .withOnMouseClickedEvent((keyword, none) -> event -> viewModel.removeKeyword(keyword))
                .install(keywordsTableDeleteColumn);
    }
}

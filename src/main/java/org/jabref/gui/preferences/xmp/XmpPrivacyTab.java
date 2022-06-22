package org.jabref.gui.preferences.xmp;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.FieldsUtil;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;

import com.airhacks.afterburner.views.ViewLoader;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;

public class XmpPrivacyTab extends AbstractPreferenceTabView<XmpPrivacyTabViewModel> implements PreferencesTab {

    @FXML private CheckBox enableXmpFilter;
    @FXML private TableView<Field> filterList;
    @FXML private TableColumn<Field, Field> fieldColumn;
    @FXML private TableColumn<Field, Field> actionsColumn;
    @FXML private ComboBox<Field> addFieldName;
    @FXML private Button addField;

    private final ControlsFxVisualizer validationVisualizer = new ControlsFxVisualizer();

    public XmpPrivacyTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() {
        return Localization.lang("XMP metadata");
    }

    public void initialize() {
        this.viewModel = new XmpPrivacyTabViewModel(dialogService, preferencesService.getXmpPreferences());

        enableXmpFilter.selectedProperty().bindBidirectional(viewModel.xmpFilterEnabledProperty());
        filterList.disableProperty().bind(viewModel.xmpFilterEnabledProperty().not());
        addFieldName.disableProperty().bind(viewModel.xmpFilterEnabledProperty().not());
        addField.disableProperty().bind(viewModel.xmpFilterEnabledProperty().not());

        fieldColumn.setSortable(true);
        fieldColumn.setReorderable(false);
        fieldColumn.setCellValueFactory(cellData -> BindingsHelper.constantOf(cellData.getValue()));
        new ValueTableCellFactory<Field, Field>()
                .withText(FieldsUtil::getNameWithType)
                .install(fieldColumn);

        actionsColumn.setSortable(false);
        actionsColumn.setReorderable(false);
        actionsColumn.setCellValueFactory(cellData -> BindingsHelper.constantOf(cellData.getValue()));
        new ValueTableCellFactory<Field, Field>()
                .withGraphic(item -> IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode())
                .withTooltip(item -> Localization.lang("Remove") + " " + item.getName())
                .withOnMouseClickedEvent(
                        item -> evt -> viewModel.removeFilter(filterList.getFocusModel().getFocusedItem()))
                .install(actionsColumn);

        filterList.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.DELETE) {
                viewModel.removeFilter(filterList.getSelectionModel().getSelectedItem());
                event.consume();
            }
        });

        filterList.itemsProperty().bind(viewModel.filterListProperty());

        addFieldName.setEditable(true);
        new ViewModelListCellFactory<Field>()
                .withText(FieldsUtil::getNameWithType)
                .install(addFieldName);
        addFieldName.itemsProperty().bind(viewModel.availableFieldsProperty());
        addFieldName.valueProperty().bindBidirectional(viewModel.addFieldNameProperty());
        addFieldName.setConverter(FieldsUtil.fieldStringConverter);
        addFieldName.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                viewModel.addField();
                event.consume();
            }
        });

        validationVisualizer.setDecoration(new IconValidationDecorator());
        Platform.runLater(() -> validationVisualizer.initVisualization(viewModel.xmpFilterListValidationStatus(), filterList));
    }

    public void addField() {
        viewModel.addField();
    }
}

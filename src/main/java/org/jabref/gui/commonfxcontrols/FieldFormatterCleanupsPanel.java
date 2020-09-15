package org.jabref.gui.commonfxcontrols;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.FieldsUtil;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;

import com.airhacks.afterburner.views.ViewLoader;

public class FieldFormatterCleanupsPanel extends VBox {

    @FXML private CheckBox cleanupsEnabled;
    @FXML private TableView<FieldFormatterCleanup> cleanupsList;
    @FXML private TableColumn<FieldFormatterCleanup, Field> fieldColumn;
    @FXML private TableColumn<FieldFormatterCleanup, Formatter> formatterColumn;
    @FXML private TableColumn<FieldFormatterCleanup, Field> actionsColumn;
    @FXML private ComboBox<Field> addableFields;
    @FXML private ComboBox<Formatter> addableFormatters;

    private FieldFormatterCleanupsPanelViewModel viewModel;

    public FieldFormatterCleanupsPanel() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void initialize() {
        this.viewModel = new FieldFormatterCleanupsPanelViewModel();

        setupTable();
        setupCombos();
        setupBindings();
    }

    private void setupTable() {
        cleanupsList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        // ToDo: To be editable the list needs a view model wrapper for FieldFormatterCleanup

        fieldColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getField()));
        new ValueTableCellFactory<FieldFormatterCleanup, Field>()
                .withText(Field::getDisplayName)
                .install(fieldColumn);

        formatterColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getFormatter()));
        new ValueTableCellFactory<FieldFormatterCleanup, Formatter>()
                .withText(Formatter::getName)
                .install(formatterColumn);

        actionsColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getField()));
        new ValueTableCellFactory<FieldFormatterCleanup, Field>()
                .withGraphic(field -> IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode())
                .withTooltip(field -> Localization.lang("Remove formatter for %0", field.getDisplayName()))
                .withOnMouseClickedEvent(item -> event -> viewModel.removeCleanup(cleanupsList.getSelectionModel().getSelectedItem()))
                .install(actionsColumn);

        viewModel.selectedCleanupProperty().setValue(cleanupsList.getSelectionModel());

        cleanupsList.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DELETE) {
                viewModel.removeCleanup(cleanupsList.getSelectionModel().getSelectedItem());
            }
        });
    }

    private void setupCombos() {
        new ViewModelListCellFactory<Field>()
                .withText(Field::getDisplayName)
                .install(addableFields);
        addableFields.setConverter(FieldsUtil.fieldStringConverter);
        addableFields.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.TAB || event.getCode() == KeyCode.ENTER) {
                addableFormatters.requestFocus();
                event.consume();
            }
        });

        new ViewModelListCellFactory<Formatter>()
                .withText(Formatter::getName)
                .withStringTooltip(Formatter::getDescription)
                .install(addableFormatters);
        addableFormatters.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                viewModel.addCleanup();
                event.consume();
            }
        });
    }

    private void setupBindings() {
        BindingsHelper.bindBidirectional((ObservableValue<Boolean>) cleanupsEnabled.selectedProperty(),
                viewModel.cleanupsDisableProperty(),
                disabled -> cleanupsEnabled.selectedProperty().setValue(!disabled),
                selected -> viewModel.cleanupsDisableProperty().setValue(!selected));

        cleanupsList.itemsProperty().bind(viewModel.cleanupsListProperty());
        addableFields.itemsProperty().bind(viewModel.availableFieldsProperty());
        addableFields.valueProperty().bindBidirectional(viewModel.selectedFieldProperty());
        addableFormatters.itemsProperty().bind(viewModel.availableFormattersProperty());
        addableFormatters.valueProperty().bindBidirectional(viewModel.selectedFormatterProperty());
    }

    @FXML
    private void resetToRecommended() {
        viewModel.resetToRecommended();
    }

    @FXML
    private void clearAll() {
        viewModel.clearAll();
    }

    @FXML
    private void addCleanup() {
        viewModel.addCleanup();
    }

    public BooleanProperty cleanupsDisableProperty() {
        return viewModel.cleanupsDisableProperty();
    }

    public ListProperty<FieldFormatterCleanup> cleanupsProperty() {
        return viewModel.cleanupsListProperty();
    }
}

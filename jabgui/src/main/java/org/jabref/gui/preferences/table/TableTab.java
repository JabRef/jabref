package org.jabref.gui.preferences.table;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.maintable.MainTableColumnModel;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;

import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;

public class TableTab extends AbstractPreferenceTabView<TableTabViewModel> {

    private final ControlsFxVisualizer validationVisualizer = new ControlsFxVisualizer();

    private final TableView<MainTableColumnModel> columnsList = new TableView<>();

    public TableTab() {
        this.viewModel = new TableTabViewModel(
                dialogService,
                preferences.getSpecialFieldsPreferences(),
                preferences.getNameDisplayPreferences(),
                preferences.getMainTablePreferences(),
                preferences.getExternalApplicationsPreferences());
        buildView();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Entry table");
    }

    private void buildView() {
        getChildren().add(form()

                .section(Localization.lang("Columns"), columns -> columns
                        .custom(buildColumnsRegion())
                        .checkbox(Localization.lang("Enable special fields"), viewModel.specialFieldsEnabledProperty(),
                                specialFields -> specialFields.help(StandardActions.HELP_SPECIAL_FIELDS, HelpFile.SPECIAL_FIELDS))
                        .checkbox(Localization.lang("Show extra columns"), viewModel.extraFileColumnsEnabledProperty())
                        .checkbox(Localization.lang("Fit table horizontally on screen"), viewModel.autoResizeColumnsProperty()))

                .section(Localization.lang("Format of author and editor names"), nameFormat -> nameFormat
                        .columns(sideBySide -> sideBySide
                                .group(order -> order
                                        .label(Localization.lang("Order"))
                                        .group(choices -> choices
                                                .radioGroup(choice -> choice
                                                        .radio(Localization.lang("Natbib style"), viewModel.namesNatbibProperty())
                                                        .radio(Localization.lang("Show names unchanged"), viewModel.nameAsIsProperty())
                                                        .radio(Localization.lang("Show 'Firstname Lastname'"), viewModel.nameFirstLastProperty())
                                                        .radio(Localization.lang("Show 'Lastname, Firstname'"), viewModel.nameLastFirstProperty())),
                                                indent -> indent.styleClass("prefIndent").spacing(4.0)))
                                .group(abbreviation -> abbreviation
                                        .label(Localization.lang("Abbreviations"))
                                        .group(choices -> choices
                                                .radioGroup(choice -> choice
                                                        .radio(Localization.lang("Do not abbreviate names"), viewModel.abbreviationDisabledProperty())
                                                        .radio(Localization.lang("Abbreviate names"), viewModel.abbreviationEnabledProperty())
                                                        .radio(Localization.lang("Show last names only"), viewModel.abbreviationLastNameOnlyProperty())),
                                                indent -> indent.styleClass("prefIndent")
                                                                .spacing(4.0)
                                                                // Natbib and "unchanged" render names verbatim,
                                                                // so abbreviation does not apply.
                                                                .disableWhen(viewModel.namesNatbibProperty().or(viewModel.nameAsIsProperty()))))))

                .build());
    }

    /// The column list with its reorder buttons alongside, and the "add column" combo underneath.
    private Node buildColumnsRegion() {
        setupColumnsList();

        ComboBox<MainTableColumnModel> addColumnName = buildAddColumnCombo();
        HBox.setHgrow(addColumnName, Priority.ALWAYS);
        HBox addRow = new HBox(4.0,
                addColumnName,
                ControlHelper.narrowIconButton(IconTheme.JabRefIcons.ADD_NOBOX, Localization.lang("Add custom column"), viewModel::insertColumnInList));

        VBox listWithAddRow = new VBox(4.0, columnsList, addRow);
        HBox.setHgrow(listWithAddRow, Priority.ALWAYS);

        VBox reorderButtons = new VBox(10.0,
                ControlHelper.narrowIconButton(IconTheme.JabRefIcons.LIST_MOVE_UP, Localization.lang("Sort column one step upwards"), viewModel::moveColumnUp),
                ControlHelper.narrowIconButton(IconTheme.JabRefIcons.LIST_MOVE_DOWN, Localization.lang("Sort column one step downwards"), viewModel::moveColumnDown),
                ControlHelper.narrowIconButton(IconTheme.JabRefIcons.REFRESH, Localization.lang("Update to current column order"), viewModel::fillColumnList));
        reorderButtons.setAlignment(Pos.CENTER);

        return new HBox(4.0, listWithAddRow, reorderButtons);
    }

    private void setupColumnsList() {
        columnsList.setPrefHeight(300.0);
        columnsList.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<MainTableColumnModel, String> nameColumn = new TableColumn<>(Localization.lang("Name"));
        nameColumn.setSortable(false);
        nameColumn.setReorderable(false);
        nameColumn.setPrefWidth(160.0);
        nameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        new ValueTableCellFactory<MainTableColumnModel, String>()
                .withText(name -> name)
                .install(nameColumn);

        TableColumn<MainTableColumnModel, String> actionsColumn = new TableColumn<>();
        actionsColumn.setSortable(false);
        actionsColumn.setReorderable(false);
        actionsColumn.setMinWidth(40.0);
        actionsColumn.setMaxWidth(40.0);
        actionsColumn.setResizable(false);
        actionsColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        new ValueTableCellFactory<MainTableColumnModel, String>()
                .withGraphic(_ -> IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode())
                .withTooltip(name -> Localization.lang("Remove column") + " " + name)
                .withOnMouseClickedEvent(_ -> _ ->
                        viewModel.removeColumn(columnsList.getFocusModel().getFocusedItem()))
                .install(actionsColumn);

        columnsList.getColumns().add(nameColumn);
        columnsList.getColumns().add(actionsColumn);
        columnsList.itemsProperty().bind(viewModel.columnsListProperty());
        viewModel.selectedColumnModelProperty().setValue(columnsList.getSelectionModel());

        columnsList.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.DELETE) {
                viewModel.removeColumn(columnsList.getSelectionModel().getSelectedItem());
                event.consume();
            }
        });

        validationVisualizer.setDecoration(new IconValidationDecorator());
        Platform.runLater(() -> validationVisualizer.initVisualization(viewModel.columnsListValidationStatus(), columnsList));
    }

    private ComboBox<MainTableColumnModel> buildAddColumnCombo() {
        ComboBox<MainTableColumnModel> combo = new ComboBox<>();
        combo.setEditable(true);
        combo.setMaxWidth(Double.MAX_VALUE);
        new ViewModelListCellFactory<MainTableColumnModel>()
                .withText(MainTableColumnModel::getDisplayName)
                .install(combo);
        combo.itemsProperty().bind(viewModel.availableColumnsProperty());
        combo.valueProperty().bindBidirectional(viewModel.addColumnProperty());
        combo.setConverter(TableTabViewModel.columnNameStringConverter);
        combo.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                viewModel.insertColumnInList();
                event.consume();
            }
        });
        return combo;
    }
}

package org.jabref.gui.preferences.nameformatter;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;

public class NameFormatterTab extends AbstractPreferenceTabView<NameFormatterTabViewModel> {

    public NameFormatterTab() {
        this.viewModel = new NameFormatterTabViewModel(preferences.getNameFormatterPreferences());
        buildView();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Name formatter");
    }

    @Override
    public String getTitle() {
        return Localization.lang("Special name formatters");
    }

    private void buildView() {
        getChildren().add(form()
                .custom(buildFormatterEditor())
                .build());
    }

    /// The formatter table with its "add formatter" input row, and a slim button column
    /// (help on top, add at the bottom) to its right.
    private Node buildFormatterEditor() {
        TableView<NameFormatterItemModel> formatterList = buildFormatterTable();

        TextField addFormatterName = new TextField();
        addFormatterName.setPromptText(Localization.lang("Formatter name"));
        HBox.setHgrow(addFormatterName, Priority.ALWAYS);
        addFormatterName.textProperty().bindBidirectional(viewModel.addFormatterNameProperty());

        TextField addFormatterString = new TextField();
        addFormatterString.setPromptText(Localization.lang("Format string"));
        HBox.setHgrow(addFormatterString, Priority.ALWAYS);
        addFormatterString.textProperty().bindBidirectional(viewModel.addFormatterStringProperty());

        addFormatterName.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                addFormatterString.requestFocus();
                addFormatterString.selectAll();
                event.consume();
            }
        });
        addFormatterString.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                viewModel.addFormatter();
                addFormatterName.requestFocus();
                event.consume();
            }
        });

        VBox tableAndInput = new VBox(4.0, formatterList, new HBox(4.0, addFormatterName, addFormatterString));
        tableAndInput.setPrefHeight(300.0);
        HBox.setHgrow(tableAndInput, Priority.ALWAYS);

        Button help = new Button();
        help.setPrefSize(25.0, 25.0);
        new ActionFactory().configureIconButton(
                StandardActions.HELP_NAME_FORMATTER,
                new HelpAction(HelpFile.CUSTOM_EXPORTS_NAME_FORMATTER, dialogService, preferences.getExternalApplicationsPreferences()),
                help);

        Button add = new Button();
        add.getStyleClass().addAll("icon-button", "narrow");
        add.setPrefSize(25.0, 25.0);
        add.setGraphic(IconTheme.JabRefIcons.ADD_NOBOX.getGraphicNode());
        add.setTooltip(new Tooltip(Localization.lang("Add formatter to list")));
        add.setOnAction(_ -> viewModel.addFormatter());

        VBox spacer = new VBox();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        VBox buttons = new VBox(help, spacer, add);

        return new HBox(4.0, tableAndInput, buttons);
    }

    private TableView<NameFormatterItemModel> buildFormatterTable() {
        TableView<NameFormatterItemModel> table = new TableView<>();
        table.setPrefHeight(300.0);
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<NameFormatterItemModel, String> nameColumn = new TableColumn<>(Localization.lang("Formatter name"));
        nameColumn.setMinWidth(50.0);
        nameColumn.setPrefWidth(200.0);
        nameColumn.setReorderable(false);
        nameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        nameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        nameColumn.setEditable(true);
        nameColumn.setOnEditCommit(event -> event.getRowValue().setName(event.getNewValue()));

        TableColumn<NameFormatterItemModel, String> formatColumn = new TableColumn<>(Localization.lang("Format string"));
        formatColumn.setMinWidth(50.0);
        formatColumn.setPrefWidth(368.0);
        formatColumn.setReorderable(false);
        formatColumn.setCellValueFactory(cellData -> cellData.getValue().formatProperty());
        formatColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        formatColumn.setEditable(true);
        formatColumn.setOnEditCommit(event -> event.getRowValue().setFormat(event.getNewValue()));

        TableColumn<NameFormatterItemModel, String> actionsColumn = new TableColumn<>();
        actionsColumn.setMinWidth(30.0);
        actionsColumn.setMaxWidth(30.0);
        actionsColumn.setPrefWidth(30.0);
        actionsColumn.setEditable(false);
        actionsColumn.setResizable(false);
        actionsColumn.setSortable(false);
        actionsColumn.setReorderable(false);
        actionsColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        new ValueTableCellFactory<NameFormatterItemModel, String>()
                .withGraphic(_ -> IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode())
                .withTooltip(name -> Localization.lang("Remove formatter '%0'", name))
                .withOnMouseClickedEvent(_ -> _ ->
                        viewModel.removeFormatter(table.getFocusModel().getFocusedItem()))
                .install(actionsColumn);

        table.getColumns().add(nameColumn);
        table.getColumns().add(formatColumn);
        table.getColumns().add(actionsColumn);

        table.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.DELETE) {
                viewModel.removeFormatter(table.getSelectionModel().getSelectedItem());
                event.consume();
            }
        });

        table.itemsProperty().bindBidirectional(viewModel.formatterListProperty());
        return table;
    }
}

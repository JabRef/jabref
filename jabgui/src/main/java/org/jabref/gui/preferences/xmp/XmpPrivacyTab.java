package org.jabref.gui.preferences.xmp;

import javax.swing.undo.UndoManager;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIconView;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.FieldsUtil;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;

import com.airhacks.afterburner.injection.Injector;

public class XmpPrivacyTab extends AbstractPreferenceTabView<XmpPrivacyTabViewModel> {

    private final UndoManager undoManager = Injector.instantiateModelOrService(UndoManager.class);

    private TableView<Field> filterList;

    public XmpPrivacyTab() {
        this.viewModel = new XmpPrivacyTabViewModel(dialogService, preferences.getXmpPreferences());
        buildView();
    }

    @Override
    public String getTabName() {
        return Localization.lang("XMP metadata");
    }

    @Override
    public String getTitle() {
        return Localization.lang("XMP export privacy settings");
    }

    private void buildView() {
        getChildren().add(form()
                .checkbox(Localization.lang("Do not write the following fields to XMP Metadata"), viewModel.xmpFilterEnabledProperty())
                .custom(buildFilterRegion(), region -> region
                        .validate(viewModel.xmpFilterListValidationStatus(), filterList))
                .build());
    }

    private Node buildFilterRegion() {
        filterList = new TableView<>();
        filterList.setPrefHeight(300.0);
        filterList.setPrefWidth(200.0);
        filterList.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        filterList.disableProperty().bind(viewModel.xmpFilterEnabledProperty().not());
        filterList.itemsProperty().bind(viewModel.filterListProperty());

        TableColumn<Field, Field> fieldColumn = new TableColumn<>(Localization.lang("Field name"));
        fieldColumn.setMinWidth(100.0);
        fieldColumn.setSortable(true);
        fieldColumn.setReorderable(false);
        fieldColumn.setCellValueFactory(cellData -> BindingsHelper.constantOf(cellData.getValue()));
        new ValueTableCellFactory<Field, Field>()
                .withText(item -> FieldsUtil.getNameWithType(item, preferences, undoManager))
                .install(fieldColumn);

        TableColumn<Field, Field> actionsColumn = new TableColumn<>();
        actionsColumn.setMinWidth(40.0);
        actionsColumn.setPrefWidth(40.0);
        actionsColumn.setMaxWidth(39.0);
        actionsColumn.setResizable(false);
        actionsColumn.setSortable(false);
        actionsColumn.setReorderable(false);
        actionsColumn.getStyleClass().add("actions-column");
        actionsColumn.setCellValueFactory(cellData -> BindingsHelper.constantOf(cellData.getValue()));
        new ValueTableCellFactory<Field, Field>()
                .withGraphic(item -> IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode())
                .withTooltip(item -> Localization.lang("Remove") + " " + item.getName())
                .withOnMouseClickedEvent(item -> _ -> viewModel.removeFilter(filterList.getFocusModel().getFocusedItem()))
                .install(actionsColumn);

        filterList.getColumns().add(fieldColumn);
        filterList.getColumns().add(actionsColumn);
        filterList.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.DELETE) {
                viewModel.removeFilter(filterList.getSelectionModel().getSelectedItem());
                event.consume();
            }
        });

        ComboBox<Field> addFieldName = new ComboBox<>();
        addFieldName.setPrefWidth(200.0);
        addFieldName.setEditable(true);
        addFieldName.disableProperty().bind(viewModel.xmpFilterEnabledProperty().not());
        new ViewModelListCellFactory<Field>()
                .withText(item -> FieldsUtil.getNameWithType(item, preferences, undoManager))
                .install(addFieldName);
        addFieldName.itemsProperty().bind(viewModel.availableFieldsProperty());
        addFieldName.valueProperty().bindBidirectional(viewModel.addFieldNameProperty());
        addFieldName.setConverter(FieldsUtil.FIELD_STRING_CONVERTER);
        addFieldName.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                viewModel.addField();
                event.consume();
            }
        });
        VBox.setVgrow(addFieldName, Priority.ALWAYS);

        VBox listColumn = new VBox(4.0, filterList, addFieldName);

        Button addField = new Button();
        addField.setGraphic(new JabRefIconView(IconTheme.JabRefIcons.ADD_NOBOX));
        addField.getStyleClass().addAll("icon-button", "narrow");
        addField.setPrefSize(25.0, 25.0);
        addField.setTooltip(new Tooltip(Localization.lang("Add field to filter list")));
        addField.disableProperty().bind(viewModel.xmpFilterEnabledProperty().not());
        addField.setOnAction(_ -> viewModel.addField());

        HBox region = new HBox(4.0, listColumn, addField);
        region.setAlignment(Pos.BOTTOM_LEFT);
        return region;
    }
}

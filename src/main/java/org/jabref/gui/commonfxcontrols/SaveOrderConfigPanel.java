package org.jabref.gui.commonfxcontrols;

import java.util.List;
import java.util.stream.Collectors;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIconView;
import org.jabref.gui.util.FieldsUtil;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;

import com.airhacks.afterburner.views.ViewLoader;

public class SaveOrderConfigPanel extends VBox {

    @FXML private RadioButton exportInSpecifiedOrder;
    @FXML private RadioButton exportInTableOrder;
    @FXML private RadioButton exportInOriginalOrder;
    @FXML private GridPane sortCriterionList;

    private SaveOrderConfigPanelViewModel viewModel;

    public SaveOrderConfigPanel() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void initialize() {
        viewModel = new SaveOrderConfigPanelViewModel();

        exportInOriginalOrder.selectedProperty().bindBidirectional(viewModel.saveInOriginalProperty());
        exportInTableOrder.selectedProperty().bindBidirectional(viewModel.saveInTableOrderProperty());
        exportInSpecifiedOrder.selectedProperty().bindBidirectional(viewModel.saveInSpecifiedOrderProperty());

        viewModel.sortCriteriaProperty().addListener((ListChangeListener<SortCriterionViewModel>) c -> {
            while (c.next()) {
                if (c.wasReplaced()) {
                    refreshBindings(viewModel.sortCriteriaProperty().get(c.getFrom()),c.getFrom());

                    /* for (SortCriterionViewModel vm : c.getList()) {
                        refreshBindings(vm, c.getList().indexOf(vm));
                    } */
                } else if (c.wasAdded()) {
                    for (SortCriterionViewModel vm : c.getAddedSubList()) {
                        int row = c.getFrom() + c.getAddedSubList().indexOf(vm);
                        createCriterionRow(vm, row);
                    }
                } else if (c.wasRemoved()) {
                    for (SortCriterionViewModel vm : c.getRemoved()) {
                        clearCriterionRow(c.getFrom());
                    }
                }
            }
        });
    }

    private void refreshBindings(SortCriterionViewModel criterionViewModel, int row) {
        for (Node node : sortCriterionList.getChildren()) {
            if (GridPane.getRowIndex(node) == row) {
                if (GridPane.getColumnIndex(node) == 1) {
                    @SuppressWarnings("unchecked") ComboBox<Field> comboBox = (ComboBox<Field>) node;
                    if (comboBox.valueProperty().isBound()) {
                        comboBox.valueProperty().unbind();
                    }
                    comboBox.valueProperty().bindBidirectional(criterionViewModel.fieldProperty());
                } else if (GridPane.getColumnIndex(node) == 2) {
                    CheckBox checkBox = (CheckBox) node;
                    if (checkBox.selectedProperty().isBound()) {
                        checkBox.selectedProperty().unbind();
                    }
                    checkBox.selectedProperty().bindBidirectional(criterionViewModel.descendingProperty());
                } else if (GridPane.getColumnIndex(node) == 3) {
                    HBox hBox = (HBox) node;
                    hBox.getChildren().clear();
                    hBox.getChildren().addAll(createRowButtons(criterionViewModel));
                }
            }
        }
    }

    private void createCriterionRow(SortCriterionViewModel criterionViewModel, int row) {
        sortCriterionList.getChildren().stream()
                         .filter(item -> GridPane.getRowIndex(item) >= row)
                         .forEach(item -> {
                             GridPane.setRowIndex(item, GridPane.getRowIndex(item) + 1);
                             if (item instanceof Label label) {
                                 label.setText(String.valueOf(GridPane.getRowIndex(item) + 1));
                             }
                         });

        Label label = new Label(String.valueOf(row + 1));
        sortCriterionList.add(label, 0, row);

        ComboBox<Field> field = new ComboBox<>(viewModel.sortableFieldsProperty());
        field.setMaxWidth(Double.MAX_VALUE);
        new ViewModelListCellFactory<Field>()
                .withText(FieldsUtil::getNameWithType)
                .install(field);
        field.setConverter(FieldsUtil.fieldStringConverter);
        field.itemsProperty().bindBidirectional(viewModel.sortableFieldsProperty());
        field.valueProperty().bindBidirectional(criterionViewModel.fieldProperty());
        sortCriterionList.add(field, 1, row);
        GridPane.getHgrow(field);

        CheckBox descending = new CheckBox(Localization.lang("Descending"));
        descending.selectedProperty().bindBidirectional(criterionViewModel.descendingProperty());
        sortCriterionList.add(descending, 2, row);

        HBox hBox = new HBox();
        hBox.getChildren().addAll(createRowButtons(criterionViewModel));
        sortCriterionList.add(hBox, 3, row);
    }

    private List<Node> createRowButtons(SortCriterionViewModel criterionViewModel) {
        Button remove = new Button("", new JabRefIconView(IconTheme.JabRefIcons.REMOVE_NOBOX));
        remove.getStyleClass().addAll("icon-button", "narrow");
        remove.setPrefHeight(20.0);
        remove.setPrefWidth(20.0);
        remove.setOnAction(event -> removeCriterion(criterionViewModel));

        Button moveUp = new Button("", new JabRefIconView(IconTheme.JabRefIcons.LIST_MOVE_UP));
        moveUp.getStyleClass().addAll("icon-button", "narrow");
        moveUp.setPrefHeight(20.0);
        moveUp.setPrefWidth(20.0);
        moveUp.setOnAction(event -> moveCriterionUp(criterionViewModel));

        Button moveDown = new Button("", new JabRefIconView(IconTheme.JabRefIcons.LIST_MOVE_DOWN));
        moveDown.getStyleClass().addAll("icon-button", "narrow");
        moveDown.setPrefHeight(20.0);
        moveDown.setPrefWidth(20.0);
        moveDown.setOnAction(event -> moveCriterionDown(criterionViewModel));

        return List.of(moveUp, moveDown, remove);
    }

    private void clearCriterionRow(int row) {
        List<Node> criterionRow = sortCriterionList.getChildren().stream()
                                                   .filter(item -> GridPane.getRowIndex(item) == row)
                                                   .collect(Collectors.toList());
        sortCriterionList.getChildren().removeAll(criterionRow);

        sortCriterionList.getChildren().stream()
                         .filter(item -> GridPane.getRowIndex(item) > row)
                         .forEach(item -> {
                             GridPane.setRowIndex(item, GridPane.getRowIndex(item) - 1);
                             if (item instanceof Label label) {
                                 label.setText(String.valueOf(GridPane.getRowIndex(item) + 1));
                             }
                         });
    }

    @FXML
    public void addCriterion() {
        viewModel.addCriterion();
    }

    @FXML
    public void moveCriterionUp(SortCriterionViewModel criterionViewModel) {
        viewModel.moveCriterionUp(criterionViewModel);
    }

    @FXML
    public void moveCriterionDown(SortCriterionViewModel criterionViewModel) {
        viewModel.moveCriterionDown(criterionViewModel);
    }

    @FXML
    public void removeCriterion(SortCriterionViewModel criterionViewModel) {
        viewModel.removeCriterion(criterionViewModel);
    }

    public BooleanProperty saveInOriginalProperty() {
        return viewModel.saveInOriginalProperty();
    }

    public BooleanProperty saveInTableOrderProperty() {
        return viewModel.saveInTableOrderProperty();
    }

    public BooleanProperty saveInSpecifiedOrderProperty() {
        return viewModel.saveInSpecifiedOrderProperty();
    }

    public ListProperty<Field> sortableFieldsProperty() {
        return viewModel.sortableFieldsProperty();
    }

    public ListProperty<SortCriterionViewModel> sortCriteriaProperty() {
        return viewModel.sortCriteriaProperty();
    }
}

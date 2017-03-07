package org.jabref.gui.groups;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.inject.Inject;

import javafx.beans.property.ObjectProperty;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Control;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionModel;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;

import org.jabref.gui.AbstractController;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.RecursiveTreeItem;
import org.jabref.gui.util.ViewModelTreeTableCellFactory;
import org.jabref.logic.l10n.Localization;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.control.textfield.TextFields;
import org.fxmisc.easybind.EasyBind;

public class GroupTreeController extends AbstractController<GroupTreeViewModel> {

    private static final Log LOGGER = LogFactory.getLog(GroupTreeController.class);

    @FXML private TreeTableView<GroupNodeViewModel> groupTree;
    @FXML private TreeTableColumn<GroupNodeViewModel,GroupNodeViewModel> mainColumn;
    @FXML private TreeTableColumn<GroupNodeViewModel,GroupNodeViewModel> numberColumn;
    @FXML private TreeTableColumn<GroupNodeViewModel,GroupNodeViewModel> disclosureNodeColumn;
    @FXML private CustomTextField searchField;

    @Inject private StateManager stateManager;
    @Inject private DialogService dialogService;

    @FXML
    public void initialize() {
        viewModel = new GroupTreeViewModel(stateManager, dialogService);

        // Set-up bindings
        viewModel.selectedGroupProperty().bind(
                EasyBind.monadic(groupTree.selectionModelProperty())
                        .flatMap(SelectionModel::selectedItemProperty)
                        .selectProperty(TreeItem::valueProperty)
        );
        viewModel.filterTextProperty().bind(searchField.textProperty());
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
        });

        groupTree.rootProperty().bind(
                EasyBind.map(viewModel.rootGroupProperty(),
                        group -> new RecursiveTreeItem<>(
                                group,
                                GroupNodeViewModel::getChildren,
                                GroupNodeViewModel::expandedProperty,
                                viewModel.filterPredicateProperty()))
        );

        // Icon and group name
        mainColumn.setCellValueFactory(cellData -> cellData.getValue().valueProperty());
        mainColumn.setCellFactory(new ViewModelTreeTableCellFactory<GroupNodeViewModel, GroupNodeViewModel>()
                .withText(GroupNodeViewModel::getDisplayName)
                .withIcon(GroupNodeViewModel::getIconCode, GroupNodeViewModel::getColor)
                .withTooltip(GroupNodeViewModel::getDescription)
        );

        // Number of hits
        PseudoClass anySelected = PseudoClass.getPseudoClass("any-selected");
        PseudoClass allSelected = PseudoClass.getPseudoClass("all-selected");
        numberColumn.setCellFactory(new ViewModelTreeTableCellFactory<GroupNodeViewModel, GroupNodeViewModel>()
                .withGraphic(group -> {
                    final StackPane node = new StackPane();
                    node.getStyleClass().setAll("hits");
                    if (!group.isRoot()) {
                        BindingsHelper.includePseudoClassWhen(node, anySelected, group.anySelectedEntriesMatchedProperty());
                        BindingsHelper.includePseudoClassWhen(node, allSelected, group.allSelectedEntriesMatchedProperty());
                    }
                    Text text = new Text();
                    text.textProperty().bind(group.getHits().asString());
                    text.getStyleClass().setAll("text");
                    node.getChildren().add(text);
                    node.setMaxWidth(Control.USE_PREF_SIZE);
                    return node;
                })
        );

        // Arrow indicating expanded status
        disclosureNodeColumn.setCellValueFactory(cellData -> cellData.getValue().valueProperty());
        disclosureNodeColumn.setCellFactory(new ViewModelTreeTableCellFactory<GroupNodeViewModel, GroupNodeViewModel>()
            .withGraphic(viewModel -> {
                final StackPane disclosureNode = new StackPane();
                disclosureNode.visibleProperty().bind(viewModel.hasChildrenProperty());
                disclosureNode.getStyleClass().setAll("tree-disclosure-node");

                final StackPane disclosureNodeArrow = new StackPane();
                disclosureNodeArrow.getStyleClass().setAll("arrow");
                disclosureNode.getChildren().add(disclosureNodeArrow);
                return disclosureNode;
            })
                .withOnMouseClickedEvent(group -> event -> group.toggleExpansion()));

        // Set pseudo-classes to indicate if row is root or sub-item ( > 1 deep)
        PseudoClass rootPseudoClass = PseudoClass.getPseudoClass("root");
        PseudoClass subElementPseudoClass = PseudoClass.getPseudoClass("sub");
        groupTree.setRowFactory(treeTable -> {
            TreeTableRow<GroupNodeViewModel> row = new TreeTableRow<>();
            row.treeItemProperty().addListener((ov, oldTreeItem, newTreeItem) -> {
                boolean isRoot = newTreeItem == treeTable.getRoot();
                row.pseudoClassStateChanged(rootPseudoClass, isRoot);

                boolean isFirstLevel = newTreeItem != null && newTreeItem.getParent() == treeTable.getRoot();
                row.pseudoClassStateChanged(subElementPseudoClass, !isRoot && !isFirstLevel);

            });
            // Remove disclosure node since we display custom version in separate column
            // Simply setting to null is not enough since it would be replaced by the default node on every change
            row.setDisclosureNode(null);
            row.disclosureNodeProperty().addListener((observable, oldValue, newValue) -> row.setDisclosureNode(null));

            // Add context menu (only for non-null items)
            row.contextMenuProperty().bind(
                    EasyBind.monadic(row.itemProperty())
                            .map(this::createContextMenuForGroup)
                            .orElse((ContextMenu) null)
            );


            return row;
        });

        // Filter text field
        setupClearButtonField(searchField);
    }

    private ContextMenu createContextMenuForGroup(GroupNodeViewModel group) {
        ContextMenu menu = new ContextMenu();

        MenuItem addSubgroup = new MenuItem(Localization.lang("Add subgroup"));
        addSubgroup.setOnAction(event -> viewModel.addNewSubgroup(group));

        MenuItem removeGroupAndSubgroups = new MenuItem(Localization.lang("Remove group and subgroups"));
        removeGroupAndSubgroups.setOnAction(event -> viewModel.removeGroupAndSubgroups(group));

        menu.getItems().add(addSubgroup);
        menu.getItems().add(removeGroupAndSubgroups);
        return menu;
    }

    public void addNewGroup(ActionEvent actionEvent) {
        viewModel.addNewGroupToRoot();
    }

    /**
     * Workaround taken from https://bitbucket.org/controlsfx/controlsfx/issues/330/making-textfieldssetupclearbuttonfield
     */
    private void setupClearButtonField(CustomTextField customTextField) {
        try {
            Method m = TextFields.class.getDeclaredMethod("setupClearButtonField", TextField.class, ObjectProperty.class);
            m.setAccessible(true);
            m.invoke(null, customTextField, customTextField.rightProperty());
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            LOGGER.error("Failed to decorate text field with clear button", ex);
        }
    }
}

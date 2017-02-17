package org.jabref.gui.groups;

import javax.inject.Inject;

import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Control;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionModel;
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

import org.fxmisc.easybind.EasyBind;

public class GroupTreeController extends AbstractController<GroupTreeViewModel> {

    @FXML private TreeTableView<GroupNodeViewModel> groupTree;
    @FXML private TreeTableColumn<GroupNodeViewModel,GroupNodeViewModel> mainColumn;
    @FXML private TreeTableColumn<GroupNodeViewModel,GroupNodeViewModel> numberColumn;
    @FXML private TreeTableColumn<GroupNodeViewModel,GroupNodeViewModel> disclosureNodeColumn;

    @Inject private StateManager stateManager;
    @Inject private DialogService dialogService;

    @FXML
    public void initialize() {
        viewModel = new GroupTreeViewModel(stateManager, dialogService);

        // Set-up bindings
        groupTree.rootProperty().bind(
                EasyBind.map(viewModel.rootGroupProperty(),
                        group -> new RecursiveTreeItem<>(group, GroupNodeViewModel::getChildren))
        );
        viewModel.selectedGroupProperty().bind(
                EasyBind.monadic(groupTree.selectionModelProperty())
                        .flatMap(SelectionModel::selectedItemProperty)
                        .selectProperty(TreeItem::valueProperty)
        );

        // Icon and group name
        mainColumn.setCellValueFactory(cellData -> cellData.getValue().valueProperty());
        mainColumn.setCellFactory(new ViewModelTreeTableCellFactory<GroupNodeViewModel, GroupNodeViewModel>()
                .withText(GroupNodeViewModel::getName)
                .withIcon(GroupNodeViewModel::getIconCode)
                .withTooltip(GroupNodeViewModel::getDescription)
        );

        // Number of hits
        PseudoClass anySelected = PseudoClass.getPseudoClass("any-selected");
        PseudoClass allSelected = PseudoClass.getPseudoClass("all-selected");
        numberColumn.setCellFactory(new ViewModelTreeTableCellFactory<GroupNodeViewModel, GroupNodeViewModel>()
                .withGraphic(viewModel -> {
                    final StackPane node = new StackPane();
                    node.getStyleClass().setAll("hits");
                    if (!viewModel.isRoot()) {
                        BindingsHelper.includePseudoClassWhen(node, anySelected, viewModel.anySelectedEntriesMatchedProperty());
                        BindingsHelper.includePseudoClassWhen(node, allSelected, viewModel.allSelectedEntriesMatchedProperty());
                    }
                    Text text = new Text();
                    text.textProperty().bind(viewModel.getHits().asString());
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
            }));

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
    }

    private ContextMenu createContextMenuForGroup(GroupNodeViewModel group) {
        ContextMenu menu = new ContextMenu();

        MenuItem addSubgroup = new MenuItem(Localization.lang("Add subgroup"));
        addSubgroup.setOnAction(event -> viewModel.addNewSubgroup(group));

        menu.getItems().add(addSubgroup);
        return menu;
    }

    public void addNewGroup(ActionEvent actionEvent) {
        viewModel.addNewGroupToRoot();
    }
}

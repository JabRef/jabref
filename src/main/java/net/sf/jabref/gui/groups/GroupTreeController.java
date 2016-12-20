package net.sf.jabref.gui.groups;

import javax.inject.Inject;

import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.Control;
import javafx.scene.control.SelectionModel;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;

import net.sf.jabref.gui.AbstractController;
import net.sf.jabref.gui.StateManager;
import net.sf.jabref.gui.util.RecursiveTreeItem;
import net.sf.jabref.gui.util.ViewModelTreeTableCellFactory;

import org.fxmisc.easybind.EasyBind;

public class GroupTreeController extends AbstractController<GroupTreeViewModel> {

    @FXML private TreeTableView<GroupNodeViewModel> groupTree;
    @FXML private TreeTableColumn<GroupNodeViewModel,GroupNodeViewModel> mainColumn;
    @FXML private TreeTableColumn<GroupNodeViewModel,Integer> numberColumn;
    @FXML private TreeTableColumn<GroupNodeViewModel,GroupNodeViewModel> disclosureNodeColumn;

    @Inject private StateManager stateManager;

    @FXML
    public void initialize() {
        viewModel = new GroupTreeViewModel(stateManager);

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
        //numberColumn.setCellValueFactory(cellData -> cellData.getValue().getValue().getHits());
        numberColumn.setCellFactory(new ViewModelTreeTableCellFactory<GroupNodeViewModel, Integer>()
                .withGraphic(viewModel -> {
                    final StackPane node = new StackPane();
                    node.getStyleClass().setAll("hits");
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
                if (viewModel.isLeaf()) {
                    return null;
                } else {
                    final StackPane disclosureNode = new StackPane();
                    disclosureNode.getStyleClass().setAll("tree-disclosure-node");

                    final StackPane disclosureNodeArrow = new StackPane();
                    disclosureNodeArrow.getStyleClass().setAll("arrow");
                    disclosureNode.getChildren().add(disclosureNodeArrow);
                    return disclosureNode;
                }
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
            return row;
        });
    }
}

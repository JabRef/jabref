package org.jabref.gui.groups;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import javax.inject.Inject;

import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Control;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;

import org.jabref.Globals;
import org.jabref.gui.AbstractController;
import org.jabref.gui.DialogService;
import org.jabref.gui.DragAndDropDataFormats;
import org.jabref.gui.StateManager;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.RecursiveTreeItem;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.gui.util.ViewModelTreeTableCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.groups.AllEntriesGroup;

import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.control.textfield.TextFields;
import org.fxmisc.easybind.EasyBind;
import org.reactfx.util.FxTimer;
import org.reactfx.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupTreeController extends AbstractController<GroupTreeViewModel> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GroupTreeController.class);

    @FXML private TreeTableView<GroupNodeViewModel> groupTree;
    @FXML private TreeTableColumn<GroupNodeViewModel, GroupNodeViewModel> mainColumn;
    @FXML private TreeTableColumn<GroupNodeViewModel, GroupNodeViewModel> numberColumn;
    @FXML private TreeTableColumn<GroupNodeViewModel, GroupNodeViewModel> disclosureNodeColumn;
    @FXML private CustomTextField searchField;

    @Inject private StateManager stateManager;
    @Inject private DialogService dialogService;
    @Inject private TaskExecutor taskExecutor;

    private static void removePseudoClasses(TreeTableRow<GroupNodeViewModel> row, PseudoClass... pseudoClasses) {
        for (PseudoClass pseudoClass : pseudoClasses) {
            row.pseudoClassStateChanged(pseudoClass, false);
        }
    }

    @FXML
    public void initialize() {
        viewModel = new GroupTreeViewModel(stateManager, dialogService, taskExecutor);

        // Set-up groups tree
        groupTree.setStyle("-fx-font-size: " + Globals.prefs.getFontSizeFX() + "pt;");
        groupTree.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Set-up bindings
        Consumer<ObservableList<GroupNodeViewModel>> updateSelectedGroups =
                (newSelectedGroups) -> newSelectedGroups.forEach(this::selectNode);

        BindingsHelper.bindContentBidirectional(
                groupTree.getSelectionModel().getSelectedItems(),
                viewModel.selectedGroupsProperty(),
                updateSelectedGroups,
                this::updateSelection
        );

        // We try to to prevent publishing changes in the search field directly to the search task that takes some time
        // for larger group structures.
        final Timer searchTask = FxTimer.create(Duration.ofMillis(400), () -> {
            LOGGER.debug("Run group search " + searchField.getText());
            viewModel.filterTextProperty().setValue(searchField.textProperty().getValue());
        });
        searchField.textProperty().addListener((observable, oldValue, newValue) -> searchTask.restart());

        groupTree.rootProperty().bind(
                EasyBind.map(viewModel.rootGroupProperty(),
                        group -> new RecursiveTreeItem<>(
                                group,
                                GroupNodeViewModel::getChildren,
                                GroupNodeViewModel::expandedProperty,
                                viewModel.filterPredicateProperty())));

        // Icon and group name
        mainColumn.setCellValueFactory(cellData -> cellData.getValue().valueProperty());
        mainColumn.setCellFactory(new ViewModelTreeTableCellFactory<GroupNodeViewModel, GroupNodeViewModel>()
                .withText(GroupNodeViewModel::getDisplayName)
                .withIcon(GroupNodeViewModel::getIcon, GroupNodeViewModel::getColor)
                .withTooltip(GroupNodeViewModel::getDescription));

        // Number of hits
        PseudoClass anySelected = PseudoClass.getPseudoClass("any-selected");
        PseudoClass allSelected = PseudoClass.getPseudoClass("all-selected");
        numberColumn.setCellFactory(new ViewModelTreeTableCellFactory<GroupNodeViewModel, GroupNodeViewModel>()
                .withGraphic(group -> {
                    final StackPane node = new StackPane();
                    node.getStyleClass().setAll("hits");
                    if (!group.isRoot()) {
                        BindingsHelper.includePseudoClassWhen(node, anySelected,
                                group.anySelectedEntriesMatchedProperty());
                        BindingsHelper.includePseudoClassWhen(node, allSelected,
                                group.allSelectedEntriesMatchedProperty());
                    }
                    Text text = new Text();
                    text.textProperty().bind(group.getHits().asString());
                    text.getStyleClass().setAll("text");
                    node.getChildren().add(text);
                    node.setMaxWidth(Control.USE_PREF_SIZE);
                    return node;
                }));

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

        // Pseudo-classes for drag and drop
        PseudoClass dragOverBottom = PseudoClass.getPseudoClass("dragOver-bottom");
        PseudoClass dragOverCenter = PseudoClass.getPseudoClass("dragOver-center");
        PseudoClass dragOverTop = PseudoClass.getPseudoClass("dragOver-top");
        groupTree.setRowFactory(treeTable -> {
            TreeTableRow<GroupNodeViewModel> row = new TreeTableRow<>();
            row.treeItemProperty().addListener((ov, oldTreeItem, newTreeItem) -> {
                boolean isRoot = newTreeItem == treeTable.getRoot();
                row.pseudoClassStateChanged(rootPseudoClass, isRoot);

                boolean isFirstLevel = (newTreeItem != null) && (newTreeItem.getParent() == treeTable.getRoot());
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
                            .orElse((ContextMenu) null));

            // Drag and drop support
            row.setOnDragDetected(event -> {
                TreeItem<GroupNodeViewModel> selectedItem = treeTable.getSelectionModel().getSelectedItem();
                if ((selectedItem != null) && (selectedItem.getValue() != null)) {
                    Dragboard dragboard = treeTable.startDragAndDrop(TransferMode.MOVE);

                    // Display the group when dragging
                    dragboard.setDragView(row.snapshot(null, null));

                    // Put the group node as content
                    ClipboardContent content = new ClipboardContent();
                    content.put(DragAndDropDataFormats.GROUP, selectedItem.getValue().getPath());
                    dragboard.setContent(content);

                    event.consume();
                }
            });
            row.setOnDragOver(event -> {
                Dragboard dragboard = event.getDragboard();
                if ((event.getGestureSource() != row) && row.getItem().acceptableDrop(dragboard)) {
                    event.acceptTransferModes(TransferMode.MOVE, TransferMode.LINK);

                    removePseudoClasses(row, dragOverBottom, dragOverCenter, dragOverTop);
                    switch (getDroppingMouseLocation(row, event)) {
                        case BOTTOM:
                            row.pseudoClassStateChanged(dragOverBottom, true);
                            break;
                        case CENTER:
                            row.pseudoClassStateChanged(dragOverCenter, true);
                            break;
                        case TOP:
                            row.pseudoClassStateChanged(dragOverTop, true);
                            break;
                    }
                }
                event.consume();
            });
            row.setOnDragExited(event -> {
                removePseudoClasses(row, dragOverBottom, dragOverCenter, dragOverTop);
            });

            row.setOnDragDropped(event -> {
                Dragboard dragboard = event.getDragboard();
                boolean success = false;
                if (dragboard.hasContent(DragAndDropDataFormats.GROUP)) {
                    String pathToSource = (String) dragboard.getContent(DragAndDropDataFormats.GROUP);
                    Optional<GroupNodeViewModel> source = viewModel.rootGroupProperty().get()
                            .getChildByPath(pathToSource);
                    if (source.isPresent()) {
                        source.get().draggedOn(row.getItem(), getDroppingMouseLocation(row, event));
                        success = true;
                    }
                }
                if (dragboard.hasContent(DragAndDropDataFormats.ENTRIES)) {
                    TransferableEntrySelection entrySelection = (TransferableEntrySelection) dragboard
                            .getContent(DragAndDropDataFormats.ENTRIES);

                    row.getItem().addEntriesToGroup(entrySelection.getSelection());
                    success = true;
                }
                event.setDropCompleted(success);
                event.consume();
            });

            return row;
        });

        // Filter text field
        setupClearButtonField(searchField);
    }

    private void updateSelection(List<TreeItem<GroupNodeViewModel>> newSelectedGroups) {
        if (newSelectedGroups == null || newSelectedGroups.isEmpty()) {
            viewModel.selectedGroupsProperty().clear();
        } else {
            List<GroupNodeViewModel> list = new ArrayList<>();
            for (TreeItem<GroupNodeViewModel> model : newSelectedGroups) {
                if (model != null && model.getValue() != null && !(model.getValue().getGroupNode().getGroup() instanceof AllEntriesGroup)) {
                    list.add(model.getValue());
                }
            }
            viewModel.selectedGroupsProperty().setAll(list);
        }
    }

    private void selectNode(GroupNodeViewModel value) {
        getTreeItemByValue(value)
                .ifPresent(treeItem -> groupTree.getSelectionModel().select(treeItem));
    }

    private Optional<TreeItem<GroupNodeViewModel>> getTreeItemByValue(GroupNodeViewModel value) {
        return getTreeItemByValue(groupTree.getRoot(), value);
    }

    private Optional<TreeItem<GroupNodeViewModel>> getTreeItemByValue(TreeItem<GroupNodeViewModel> root,
                                                                      GroupNodeViewModel value) {
        if (root.getValue().equals(value)) {
            return Optional.of(root);
        }

        for (TreeItem<GroupNodeViewModel> child : root.getChildren()) {
            Optional<TreeItem<GroupNodeViewModel>> treeItemByValue = getTreeItemByValue(child, value);
            if (treeItemByValue.isPresent()) {
                return treeItemByValue;
            }
        }

        return Optional.empty();
    }

    private ContextMenu createContextMenuForGroup(GroupNodeViewModel group) {
        ContextMenu menu = new ContextMenu();

        MenuItem editGroup = new MenuItem(Localization.lang("Edit group"));
        editGroup.setOnAction(event -> {
            menu.hide();
            viewModel.editGroup(group);
        });

        MenuItem addSubgroup = new MenuItem(Localization.lang("Add subgroup"));
        addSubgroup.setOnAction(event -> {
            menu.hide();
            viewModel.addNewSubgroup(group);
        });
        MenuItem removeGroupAndSubgroups = new MenuItem(Localization.lang("Remove group and subgroups"));
        removeGroupAndSubgroups.setOnAction(event -> viewModel.removeGroupAndSubgroups(group));
        MenuItem removeGroupKeepSubgroups = new MenuItem(Localization.lang("Remove group, keep subgroups"));
        removeGroupKeepSubgroups.setOnAction(event -> viewModel.removeGroupKeepSubgroups(group));
        MenuItem removeSubgroups = new MenuItem(Localization.lang("Remove subgroups"));
        removeSubgroups.setOnAction(event -> viewModel.removeSubgroups(group));

        MenuItem addEntries = new MenuItem(Localization.lang("Add selected entries to this group"));
        addEntries.setOnAction(event -> viewModel.addSelectedEntries(group));
        MenuItem removeEntries = new MenuItem(Localization.lang("Remove selected entries from this group"));
        removeEntries.setOnAction(event -> viewModel.removeSelectedEntries(group));

        MenuItem sortAlphabetically = new MenuItem(Localization.lang("Sort all subgroups (recursively)"));
        sortAlphabetically.setOnAction(event -> viewModel.sortAlphabeticallyRecursive(group));

        menu.getItems().add(editGroup);
        menu.getItems().add(new SeparatorMenuItem());
        menu.getItems().addAll(addSubgroup, removeSubgroups, removeGroupAndSubgroups, removeGroupKeepSubgroups);
        menu.getItems().add(new SeparatorMenuItem());
        menu.getItems().addAll(addEntries, removeEntries);
        menu.getItems().add(new SeparatorMenuItem());
        menu.getItems().add(sortAlphabetically);

        // TODO: Disable some actions under certain conditions
        //if (group.canBeEdited()) {
        //editGroupPopupAction.setEnabled(false);
        //addGroupPopupAction.setEnabled(false);
        //removeGroupAndSubgroupsPopupAction.setEnabled(false);
        //removeGroupKeepSubgroupsPopupAction.setEnabled(false);
        //} else {
        //editGroupPopupAction.setEnabled(true);
        //addGroupPopupAction.setEnabled(true);
        //addGroupPopupAction.setNode(node);
        //removeGroupAndSubgroupsPopupAction.setEnabled(true);
        //removeGroupKeepSubgroupsPopupAction.setEnabled(true);
        //}
        //sortSubmenu.setEnabled(!node.isLeaf());
        //removeSubgroupsPopupAction.setEnabled(!node.isLeaf());

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
            Method m = TextFields.class.getDeclaredMethod("setupClearButtonField", TextField.class,
                    ObjectProperty.class);
            m.setAccessible(true);
            m.invoke(null, customTextField, customTextField.rightProperty());
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            LOGGER.error("Failed to decorate text field with clear button", ex);
        }
    }

    /**
     * Determines where the mouse is in the given row.
     */
    private DroppingMouseLocation getDroppingMouseLocation(TreeTableRow<GroupNodeViewModel> row, DragEvent event) {
        if ((row.getHeight() * 0.25) > event.getY()) {
            return DroppingMouseLocation.TOP;
        } else if ((row.getHeight() * 0.75) < event.getY()) {
            return DroppingMouseLocation.BOTTOM;
        } else {
            return DroppingMouseLocation.CENTER;
        }
    }
}

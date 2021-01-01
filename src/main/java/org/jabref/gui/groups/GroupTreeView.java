package org.jabref.gui.groups;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.css.PseudoClass;
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
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;

import org.jabref.gui.DialogService;
import org.jabref.gui.DragAndDropDataFormats;
import org.jabref.gui.StateManager;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.CustomLocalDragboard;
import org.jabref.gui.util.RecursiveTreeItem;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.gui.util.ViewModelTreeTableCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.groups.AllEntriesGroup;
import org.jabref.preferences.PreferencesService;

import com.tobiasdiez.easybind.EasyBind;
import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.control.textfield.TextFields;
import org.reactfx.util.FxTimer;
import org.reactfx.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupTreeView {

    private static final Logger LOGGER = LoggerFactory.getLogger(GroupTreeView.class);

    @FXML private TreeTableView<GroupNodeViewModel> groupTree;
    @FXML private TreeTableColumn<GroupNodeViewModel, GroupNodeViewModel> mainColumn;
    @FXML private TreeTableColumn<GroupNodeViewModel, GroupNodeViewModel> numberColumn;
    @FXML private TreeTableColumn<GroupNodeViewModel, GroupNodeViewModel> expansionNodeColumn;
    @FXML private CustomTextField searchField;

    @Inject private StateManager stateManager;
    @Inject private DialogService dialogService;
    @Inject private TaskExecutor taskExecutor;
    @Inject private PreferencesService preferencesService;

    private GroupTreeViewModel viewModel;
    private CustomLocalDragboard localDragboard;

    private DragExpansionHandler dragExpansionHandler;

    @FXML
    public void initialize() {
        this.localDragboard = stateManager.getLocalDragboard();
        viewModel = new GroupTreeViewModel(stateManager, dialogService, preferencesService, taskExecutor, localDragboard);

        // Set-up groups tree
        groupTree.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        dragExpansionHandler = new DragExpansionHandler();

        // Set-up bindings
        Platform.runLater(() ->
                BindingsHelper.bindContentBidirectional(
                        groupTree.getSelectionModel().getSelectedItems(),
                        viewModel.selectedGroupsProperty(),
                        (newSelectedGroups) -> newSelectedGroups.forEach(this::selectNode),
                        this::updateSelection
                ));

        // We try to to prevent publishing changes in the search field directly to the search task that takes some time
        // for larger group structures.
        final Timer searchTask = FxTimer.create(Duration.ofMillis(400), () -> {
            LOGGER.debug("Run group search " + searchField.getText());
            viewModel.filterTextProperty().setValue(searchField.textProperty().getValue());
        });
        searchField.textProperty().addListener((observable, oldValue, newValue) -> searchTask.restart());

        groupTree.rootProperty().bind(
                EasyBind.map(viewModel.rootGroupProperty(),
                        group -> {
                            if (group == null) {
                                return null;
                            } else {
                                return new RecursiveTreeItem<>(
                                        group,
                                        GroupNodeViewModel::getChildren,
                                        GroupNodeViewModel::expandedProperty,
                                        viewModel.filterPredicateProperty());
                            }
                        }));

        // Icon and group name
        new ViewModelTreeTableCellFactory<GroupNodeViewModel>()
                .withText(GroupNodeViewModel::getDisplayName)
                .withIcon(GroupNodeViewModel::getIcon)
                .withTooltip(GroupNodeViewModel::getDescription)
                .install(mainColumn);

        // Number of hits (only if user wants to see them)
        PseudoClass anySelected = PseudoClass.getPseudoClass("any-selected");
        PseudoClass allSelected = PseudoClass.getPseudoClass("all-selected");
        new ViewModelTreeTableCellFactory<GroupNodeViewModel>()
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
                    if (preferencesService.getDisplayGroupCount()) {
                        text.textProperty().bind(group.getHits().asString());
                    }
                    text.getStyleClass().setAll("text");
                    node.getChildren().add(text);
                    node.setMaxWidth(Control.USE_PREF_SIZE);
                    return node;
                })
                .install(numberColumn);

        // Arrow indicating expanded status
        new ViewModelTreeTableCellFactory<GroupNodeViewModel>()
                .withGraphic(viewModel -> {
                    final StackPane disclosureNode = new StackPane();
                    disclosureNode.visibleProperty().bind(viewModel.hasChildrenProperty());
                    disclosureNode.getStyleClass().setAll("tree-disclosure-node");

                    final StackPane disclosureNodeArrow = new StackPane();
                    disclosureNodeArrow.getStyleClass().setAll("arrow");
                    disclosureNode.getChildren().add(disclosureNodeArrow);
                    return disclosureNode;
                })
                .withOnMouseClickedEvent(group -> event -> {
                    group.toggleExpansion();
                    event.consume();
                })
                .install(expansionNodeColumn);

        // Set pseudo-classes to indicate if row is root or sub-item ( > 1 deep)
        PseudoClass rootPseudoClass = PseudoClass.getPseudoClass("root");
        PseudoClass subElementPseudoClass = PseudoClass.getPseudoClass("sub");

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
                    EasyBind.wrapNullable(row.itemProperty())
                            .map(this::createContextMenuForGroup)
                            .orElse((ContextMenu) null));
            row.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                if (event.getButton() == MouseButton.SECONDARY) {
                    // Prevent right-click to select group
                    event.consume();
                }
            });

            // Drag and drop support
            row.setOnDragDetected(event -> {
                List<String> groupsToMove = new ArrayList<>();
                for (TreeItem<GroupNodeViewModel> selectedItem : treeTable.getSelectionModel().getSelectedItems()) {
                    if ((selectedItem != null) && (selectedItem.getValue() != null)) {
                        groupsToMove.add(selectedItem.getValue().getPath());
                    }
                }

                if (groupsToMove.size() > 0) {
                    localDragboard.clearAll();
                }

                // Put the group nodes as content
                Dragboard dragboard = treeTable.startDragAndDrop(TransferMode.MOVE);
                // Display the group when dragging
                dragboard.setDragView(row.snapshot(null, null));
                ClipboardContent content = new ClipboardContent();
                content.put(DragAndDropDataFormats.GROUP, groupsToMove);
                dragboard.setContent(content);
                event.consume();
            });
            row.setOnDragOver(event -> {
                Dragboard dragboard = event.getDragboard();
                if ((event.getGestureSource() != row) && (row.getItem() != null) && row.getItem().acceptableDrop(dragboard)) {
                    event.acceptTransferModes(TransferMode.MOVE, TransferMode.LINK);

                    // expand node and all children on drag over
                    dragExpansionHandler.expandGroup(row.getTreeItem());

                    if (localDragboard.hasBibEntries()) {
                        ControlHelper.setDroppingPseudoClasses(row);
                    } else {
                        ControlHelper.setDroppingPseudoClasses(row, event);
                    }
                }
                event.consume();
            });
            row.setOnDragExited(event -> {
                ControlHelper.removeDroppingPseudoClasses(row);
            });

            row.setOnDragDropped(event -> {
                Dragboard dragboard = event.getDragboard();
                boolean success = false;

                if (dragboard.hasContent(DragAndDropDataFormats.GROUP)) {
                    List<String> pathToSources = (List<String>) dragboard.getContent(DragAndDropDataFormats.GROUP);
                    List<GroupNodeViewModel> changedGroups = new LinkedList<>();
                    for (String pathToSource : pathToSources) {
                        Optional<GroupNodeViewModel> source = viewModel
                                .rootGroupProperty().get()
                                .getChildByPath(pathToSource);
                        if (source.isPresent()) {
                            source.get().draggedOn(row.getItem(), ControlHelper.getDroppingMouseLocation(row, event));
                            changedGroups.add(source.get());
                            success = true;
                        }
                    }
                    groupTree.getSelectionModel().clearSelection();
                    changedGroups.forEach(value -> selectNode(value, true));
                }

                if (localDragboard.hasBibEntries()) {
                    List<BibEntry> entries = localDragboard.getBibEntries();
                    row.getItem().addEntriesToGroup(entries);
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
        if ((newSelectedGroups == null) || newSelectedGroups.isEmpty()) {
            viewModel.selectedGroupsProperty().clear();
        } else {
            List<GroupNodeViewModel> list = newSelectedGroups.stream().filter(model -> model != null && !(model.getValue().getGroupNode().getGroup() instanceof AllEntriesGroup)).map(TreeItem::getValue).collect(Collectors.toList());
            viewModel.selectedGroupsProperty().setAll(list);
        }
    }

    private void selectNode(GroupNodeViewModel value) {
        selectNode(value, false);
    }

    private void selectNode(GroupNodeViewModel value, boolean expandParents) {
        getTreeItemByValue(value)
                .ifPresent(treeItem -> {
                    if (expandParents) {
                        TreeItem<GroupNodeViewModel> parent = treeItem.getParent();
                        while (parent != null) {
                            parent.setExpanded(true);
                            parent = parent.getParent();
                        }
                    }
                    groupTree.getSelectionModel().select(treeItem);
                });
    }

    private Optional<TreeItem<GroupNodeViewModel>> getTreeItemByValue(GroupNodeViewModel value) {
        return getTreeItemByValue(groupTree.getRoot(), value);
    }

    private Optional<TreeItem<GroupNodeViewModel>> getTreeItemByValue(TreeItem<GroupNodeViewModel> root,
                                                                      GroupNodeViewModel value) {
        if (root.getValue().equals(value)) {
            return Optional.of(root);
        }

        Optional<TreeItem<GroupNodeViewModel>> node = Optional.empty();
        for (TreeItem<GroupNodeViewModel> child : root.getChildren()) {
            node = getTreeItemByValue(child, value);
            if (node.isPresent()) {
                break;
            }
        }

        return node;
    }

    private ContextMenu createContextMenuForGroup(GroupNodeViewModel group) {
        ContextMenu menu = new ContextMenu();

        MenuItem editGroup = new MenuItem(Localization.lang("Edit group"));
        editGroup.setOnAction(event -> {
            menu.hide();
            viewModel.editGroup(group);
            groupTree.refresh();
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
        // if (group.canBeEdited()) {
        // editGroupPopupAction.setEnabled(false);
        // addGroupPopupAction.setEnabled(false);
        // removeGroupAndSubgroupsPopupAction.setEnabled(false);
        // removeGroupKeepSubgroupsPopupAction.setEnabled(false);
        // } else {
        // editGroupPopupAction.setEnabled(true);
        // addGroupPopupAction.setEnabled(true);
        // addGroupPopupAction.setNode(node);
        // removeGroupAndSubgroupsPopupAction.setEnabled(true);
        // removeGroupKeepSubgroupsPopupAction.setEnabled(true);
        // }
        // sortSubmenu.setEnabled(!node.isLeaf());
        // removeSubgroupsPopupAction.setEnabled(!node.isLeaf());

        return menu;
    }

    @FXML
    private void addNewGroup() {
        viewModel.addNewGroupToRoot();
    }

    /**
     * Workaround taken from https://bitbucket.org/controlsfx/controlsfx/issues/330/making-textfieldssetupclearbuttonfield
     */
    private void setupClearButtonField(CustomTextField customTextField) {
        try {
            // TODO: reflective access, should be removed
            Method m = TextFields.class.getDeclaredMethod("setupClearButtonField", TextField.class, ObjectProperty.class);
            m.setAccessible(true);
            m.invoke(null, customTextField, customTextField.rightProperty());
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            LOGGER.error("Failed to decorate text field with clear button", ex);
        }
    }

    private static class DragExpansionHandler {
        private static final long DRAG_TIME_BEFORE_EXPANDING_MS = 1000;
        private TreeItem<GroupNodeViewModel> draggedItem;
        private long dragStarted;

        public void expandGroup(TreeItem<GroupNodeViewModel> treeItem) {
            if (!treeItem.equals(draggedItem)) {
                this.draggedItem = treeItem;
                this.dragStarted = System.currentTimeMillis();
                this.draggedItem.setExpanded(this.draggedItem.isExpanded());
                return;
            }

            if (System.currentTimeMillis() - this.dragStarted > DRAG_TIME_BEFORE_EXPANDING_MS) {
                // expand or collapse the tree item and reset the time
                this.dragStarted = System.currentTimeMillis();
                this.draggedItem.setExpanded(!this.draggedItem.isExpanded());
            } else {
                // leave the expansion state of the tree item as it is
                this.draggedItem.setExpanded(this.draggedItem.isExpanded());
            }
        }
    }
}

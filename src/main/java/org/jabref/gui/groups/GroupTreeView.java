package org.jabref.gui.groups;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Control;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;

import org.jabref.gui.DialogService;
import org.jabref.gui.DragAndDropDataFormats;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.CustomLocalDragboard;
import org.jabref.gui.util.RecursiveTreeItem;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.gui.util.ViewModelTreeTableCellFactory;
import org.jabref.gui.util.ViewModelTreeTableRowFactory;
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

public class GroupTreeView extends BorderPane {

    private static final Logger LOGGER = LoggerFactory.getLogger(GroupTreeView.class);

    private static final PseudoClass PSEUDOCLASS_ANYSELECTED = PseudoClass.getPseudoClass("any-selected");
    private static final PseudoClass PSEUDOCLASS_ALLSELECTED = PseudoClass.getPseudoClass("all-selected");
    private static final PseudoClass PSEUDOCLASS_ROOTELEMENT = PseudoClass.getPseudoClass("root");
    private static final PseudoClass PSEUDOCLASS_SUBELEMENT = PseudoClass.getPseudoClass("sub"); // > 1 deep

    private static final double SCROLL_SPEED_UP = 3.0;

    private TreeTableView<GroupNodeViewModel> groupTree;
    private TreeTableColumn<GroupNodeViewModel, GroupNodeViewModel> mainColumn;
    private TreeTableColumn<GroupNodeViewModel, GroupNodeViewModel> numberColumn;
    private TreeTableColumn<GroupNodeViewModel, GroupNodeViewModel> expansionNodeColumn;
    private CustomTextField searchField;

    private final StateManager stateManager;
    private final DialogService dialogService;
    private final TaskExecutor taskExecutor;
    private final PreferencesService preferencesService;

    private GroupTreeViewModel viewModel;
    private CustomLocalDragboard localDragboard;

    private DragExpansionHandler dragExpansionHandler;

    private Timer scrollTimer;
    private double scrollVelocity = 0;
    private double scrollableAreaHeight;
    private double upperBorder;
    private double lowerBorder;
    private double baseFactor;

    /**
     * Note: This panel is deliberately not created in fxml, since parsing equivalent fxml takes about 500 msecs
     */
    public GroupTreeView(TaskExecutor taskExecutor,
                         StateManager stateManager,
                         PreferencesService preferencesService,
                         DialogService dialogService) {
        this.taskExecutor = taskExecutor;
        this.stateManager = stateManager;
        this.preferencesService = preferencesService;
        this.dialogService = dialogService;

        createNodes();
        this.getStylesheets().add(Objects.requireNonNull(GroupTreeView.class.getResource("GroupTree.css")).toExternalForm());
        initialize();
    }

    private void createNodes() {
        searchField = new CustomTextField();

        searchField.setPromptText(Localization.lang("Filter groups"));
        searchField.setId("searchField");
        HBox.setHgrow(searchField, Priority.ALWAYS);
        HBox groupFilterBar = new HBox(searchField);
        groupFilterBar.setId("groupFilterBar");
        this.setTop(groupFilterBar);

        mainColumn = new TreeTableColumn<>();
        mainColumn.setId("mainColumn");
        mainColumn.setResizable(true);
        numberColumn = new TreeTableColumn<>();
        numberColumn.getStyleClass().add("numberColumn");
        numberColumn.setMinWidth(60d);
        numberColumn.setMaxWidth(60d);
        numberColumn.setPrefWidth(60d);
        numberColumn.setResizable(false);
        expansionNodeColumn = new TreeTableColumn<>();
        expansionNodeColumn.getStyleClass().add("expansionNodeColumn");
        expansionNodeColumn.setMaxWidth(20d);
        expansionNodeColumn.setMinWidth(20d);
        expansionNodeColumn.setPrefWidth(20d);
        expansionNodeColumn.setResizable(false);

        groupTree = new TreeTableView<>();
        groupTree.setId("groupTree");
        groupTree.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        groupTree.getColumns().addAll(List.of(mainColumn, numberColumn, expansionNodeColumn));
        this.setCenter(groupTree);

        mainColumn.prefWidthProperty().bind(groupTree.widthProperty().subtract(80d).subtract(15d));

        Button addNewGroup = new Button(Localization.lang("Add group"));
        addNewGroup.setId("addNewGroup");
        addNewGroup.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(addNewGroup, Priority.ALWAYS);
        addNewGroup.setTooltip(new Tooltip(Localization.lang("New group")));
        addNewGroup.setOnAction(event -> addNewGroup());

        HBox groupBar = new HBox(addNewGroup);
        groupBar.setId("groupBar");
        this.setBottom(groupBar);
    }

    private void initialize() {
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
                        newSelectedGroups -> newSelectedGroups.forEach(this::selectNode),
                        this::updateSelection
                ));

        // We try to prevent publishing changes in the search field directly to the search task that takes some time
        // for larger group structures.
        final Timer searchTask = FxTimer.create(Duration.ofMillis(400), () -> {
            LOGGER.debug("Run group search {}", searchField.getText());
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
        new ViewModelTreeTableCellFactory<GroupNodeViewModel>()
                .withGraphic(this::createNumberCell)
                .install(numberColumn);

        // Arrow indicating expanded status
        new ViewModelTreeTableCellFactory<GroupNodeViewModel>()
                .withGraphic(this::getArrowCell)
                .withOnMouseClickedEvent(group -> event -> {
                    group.toggleExpansion();
                    event.consume();
                })
                .install(expansionNodeColumn);

        new ViewModelTreeTableRowFactory<GroupNodeViewModel>()
                .withContextMenu(this::createContextMenuForGroup)
                .withEventFilter(MouseEvent.MOUSE_PRESSED, (row, event) -> {
                    if (((MouseEvent) event).getButton() == MouseButton.SECONDARY && !stateManager.getSelectedEntries().isEmpty()) {
                        // Prevent right-click to select group whe we have selected entries
                        event.consume();
                    } else if (event.getTarget() instanceof StackPane pane) {
                        if (pane.getStyleClass().contains("arrow") || pane.getStyleClass().contains("tree-disclosure-node")) {
                            event.consume();
                        }
                    }
                })
                .withCustomInitializer(row -> {
                    // Remove disclosure node since we display custom version in separate column
                    // Simply setting to null is not enough since it would be replaced by the default node on every change
                    row.setDisclosureNode(null);
                    row.disclosureNodeProperty().addListener((observable, oldValue, newValue) -> row.setDisclosureNode(null));
                })
                .setOnDragDetected(this::handleOnDragDetected)
                .setOnDragDropped(this::handleOnDragDropped)
                .setOnDragExited(this::handleOnDragExited)
                .setOnDragOver(this::handleOnDragOver)
                .withPseudoClass(PSEUDOCLASS_ROOTELEMENT, row -> Bindings.createBooleanBinding(
                        () -> (row != null) && (groupTree.getRoot() != null) && (row.getItem() == groupTree.getRoot().getValue()), row.treeItemProperty()))
                .withPseudoClass(PSEUDOCLASS_SUBELEMENT, row -> Bindings.createBooleanBinding(
                        () -> (row != null) && (groupTree.getTreeItemLevel(row.getTreeItem()) > 1), row.treeItemProperty()))
                .install(groupTree);

        setupDragScrolling();

        // Filter text field
        setupClearButtonField(searchField);
    }

    private StackPane getArrowCell(GroupNodeViewModel viewModel) {
        final StackPane disclosureNode = new StackPane();
        disclosureNode.visibleProperty().bind(viewModel.hasChildrenProperty());
        disclosureNode.getStyleClass().setAll("tree-disclosure-node");

        final StackPane disclosureNodeArrow = new StackPane();
        disclosureNodeArrow.getStyleClass().setAll("arrow");
        disclosureNode.getChildren().add(disclosureNodeArrow);
        return disclosureNode;
    }

    private StackPane createNumberCell(GroupNodeViewModel group) {
        final StackPane node = new StackPane();
        node.getStyleClass().add("hits");
        if (!group.isRoot()) {
            BindingsHelper.includePseudoClassWhen(node, PSEUDOCLASS_ANYSELECTED,
                    group.anySelectedEntriesMatchedProperty());
            BindingsHelper.includePseudoClassWhen(node, PSEUDOCLASS_ALLSELECTED,
                    group.allSelectedEntriesMatchedProperty());
        }
        Text text = new Text();
        EasyBind.subscribe(preferencesService.getGroupsPreferences().displayGroupCountProperty(),
                shouldDisplayGroupCount -> {
                    if (text.textProperty().isBound()) {
                        text.textProperty().unbind();
                        text.setText("");
                    }

                    if (shouldDisplayGroupCount) {
                        text.textProperty().bind(group.getHits().map(Number::intValue).map(this::getFormattedNumber));
                        Tooltip tooltip = new Tooltip();
                        tooltip.textProperty().bind(group.getHits().asString());
                        Tooltip.install(text, tooltip);
                    }
                });
        text.getStyleClass().setAll("text");

        text.styleProperty().bind(Bindings.createStringBinding(() -> {
            double reducedFontSize;
            double font_size = preferencesService.getWorkspacePreferences().getMainFontSize();
            // For each breaking point, the font size is reduced 0.20 em to fix issue 8797
            if (font_size > 26.0) {
                reducedFontSize = 0.25;
            } else if (font_size > 22.0) {
                reducedFontSize = 0.35;
            } else if (font_size > 18.0) {
                reducedFontSize = 0.55;
            } else {
                reducedFontSize = 0.75;
            }
            return "-fx-font-size: %fem;".formatted(reducedFontSize);
        }, preferencesService.getWorkspacePreferences().mainFontSizeProperty()));

        node.getChildren().add(text);
        node.setMaxWidth(Control.USE_PREF_SIZE);
        return node;
    }

    private void handleOnDragExited(TreeTableRow<GroupNodeViewModel> row, GroupNodeViewModel fieldViewModel, DragEvent dragEvent) {
        ControlHelper.removeDroppingPseudoClasses(row);
    }

    private void handleOnDragDetected(TreeTableRow<GroupNodeViewModel> row, GroupNodeViewModel groupViewModel, MouseEvent event) {
        List<String> groupsToMove = new ArrayList<>();
        for (TreeItem<GroupNodeViewModel> selectedItem : row.getTreeTableView().getSelectionModel().getSelectedItems()) {
            if ((selectedItem != null) && (selectedItem.getValue() != null)) {
                groupsToMove.add(selectedItem.getValue().getPath());
            }
        }

        if (!groupsToMove.isEmpty()) {
            localDragboard.clearAll();
        }

        // Put the group nodes as content
        Dragboard dragboard = row.startDragAndDrop(TransferMode.MOVE);
        // Display the group when dragging
        dragboard.setDragView(row.snapshot(null, null));
        ClipboardContent content = new ClipboardContent();
        content.put(DragAndDropDataFormats.GROUP, groupsToMove);
        dragboard.setContent(content);
        event.consume();
    }

    private void handleOnDragDropped(TreeTableRow<GroupNodeViewModel> row, GroupNodeViewModel originalItem, DragEvent event) {
        Dragboard dragboard = event.getDragboard();
        boolean success = false;

        if (dragboard.hasContent(DragAndDropDataFormats.GROUP) && row.getItem().canAddGroupsIn()) {
            List<String> pathToSources = (List<String>) dragboard.getContent(DragAndDropDataFormats.GROUP);
            List<GroupNodeViewModel> changedGroups = new LinkedList<>();
            for (String pathToSource : pathToSources) {
                Optional<GroupNodeViewModel> source = viewModel
                        .rootGroupProperty().get()
                        .getChildByPath(pathToSource);
                if (source.isPresent() && source.get().canBeDragged()) {
                    source.get().draggedOn(row.getItem(), ControlHelper.getDroppingMouseLocation(row, event));
                    changedGroups.add(source.get());
                    success = true;
                }
            }
            groupTree.getSelectionModel().clearSelection();
            changedGroups.forEach(value -> selectNode(value, true));
            if (success) {
                viewModel.writeGroupChangesToMetaData();
            }
        }

        if (localDragboard.hasBibEntries()) {
            List<BibEntry> entries = localDragboard.getBibEntries();
            row.getItem().addEntriesToGroup(entries);
            success = true;
        }
        event.setDropCompleted(success);
        event.consume();
    }

    private void handleOnDragOver(TreeTableRow<GroupNodeViewModel> row, GroupNodeViewModel originalItem, DragEvent event) {
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
    }

    private void updateSelection(List<TreeItem<GroupNodeViewModel>> newSelectedGroups) {
        if ((newSelectedGroups == null) || newSelectedGroups.isEmpty()) {
            viewModel.selectedGroupsProperty().clear();
        } else {
            List<GroupNodeViewModel> list = newSelectedGroups.stream().filter(model -> (model != null) && !(model.getValue().getGroupNode().getGroup() instanceof AllEntriesGroup)).map(TreeItem::getValue).collect(Collectors.toList());
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
        if (root == null) {
            return Optional.empty();
        }

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

    private void setupDragScrolling() {
        // see http://programmingtipsandtraps.blogspot.com/2015/10/drag-and-drop-in-treetableview-with.html

        // timer used to implement scrolling
        scrollTimer = FxTimer.createPeriodic(Duration.ofMillis(100), () ->
                getVerticalScrollbar().ifPresent(scrollBar -> {
                    double newValue = scrollBar.getValue() + scrollVelocity;
                    newValue = Math.min(newValue, 1d);
                    newValue = Math.max(newValue, 0d);
                    scrollBar.setValue(newValue);
                }));

        // Start
        groupTree.setOnDragEntered(event -> {
            initScrolling();
            scrollTimer.restart();
        });

        // During dragging
        groupTree.setOnDragOver(event -> {
            boolean scrollingUp = event.getY() < upperBorder;
            boolean scrollingDown = event.getY() > lowerBorder;

            if (!scrollingUp && !scrollingDown) {
                scrollVelocity = 0;
                return;
            }

            double distanceFromNonScrollableInsideArea;
            if (scrollingUp) {
                distanceFromNonScrollableInsideArea = scrollableAreaHeight - event.getY();
            } else {
                distanceFromNonScrollableInsideArea = scrollableAreaHeight - (groupTree.getHeight() - event.getY());
            }

            // part "(1+x)" of formula "speed = 20px/s (1+x)" (proposed by https://github.com/JabRef/jabref/issues/9754#issuecomment-1766864908)
            // / 10.0 is because of the 100 milliseconds above. (it is 20px per second, 10.0 * 100.0 ms = 1 second)
            scrollVelocity = (baseFactor * (1.0 + distanceFromNonScrollableInsideArea)) / 10.0;
            if (scrollingUp) {
                scrollVelocity = -scrollVelocity;
            }
        });

        // Stop
        groupTree.setOnScroll(event -> scrollTimer.stop());
        groupTree.setOnDragDone(event -> scrollTimer.stop());
        groupTree.setOnDragDropped(event -> scrollTimer.stop());
        groupTree.setOnDragExited(event -> scrollTimer.stop());
    }

    private void initScrolling() {
        int numberOfShownGroups = groupTree.getExpandedItemCount();

        if (numberOfShownGroups == 0) {
            scrollVelocity = 0;
            return;
        }

        double heightOfOneNode = groupTree.getChildrenUnmodifiable().get(0).getLayoutBounds().getHeight();
        // heightOfOneNode is the size of text. We need including surroundings.
        // We found no way to get this. We can only do a heuristics here.
        // 2.0 is backed by measurement using the screen ruler utility (https://learn.microsoft.com/en-us/windows/powertoys/screen-ruler)
        heightOfOneNode = heightOfOneNode * 2.0;

        // At most scroll area is three entries large
        scrollableAreaHeight = Math.min(heightOfOneNode * 3.0, groupTree.getHeight() / 3.0);
        upperBorder = scrollableAreaHeight;
        lowerBorder = groupTree.getHeight() - scrollableAreaHeight;

        // 20 is derived from "speed = 20px/s (1+x)" (proposed by https://github.com/JabRef/jabref/issues/9754#issuecomment-1766864908)
        // (1.0 / groupTree.getHeight()) is the factor to convert from px to fraction of total height
        double totalHeight = heightOfOneNode * numberOfShownGroups;
        baseFactor = 20.0 * (1.0 / totalHeight);
    }

    private Optional<ScrollBar> getVerticalScrollbar() {
        for (Node node : groupTree.lookupAll(".scroll-bar")) {
            if (node instanceof ScrollBar scrollbar
                    && scrollbar.getOrientation().equals(Orientation.VERTICAL)) {
                return Optional.of(scrollbar);
            }
        }
        return Optional.empty();
    }

    private ContextMenu createContextMenuForGroup(GroupNodeViewModel group) {
        if (group == null) {
            return null;
        }

        ContextMenu contextMenu = new ContextMenu();
        ActionFactory factory = new ActionFactory(preferencesService.getKeyBindingRepository());

        MenuItem removeGroup;
        if (group.hasSubgroups() && group.canAddGroupsIn() && !group.isRoot()) {
            removeGroup = new Menu(Localization.lang("Remove group"), null,
                    factory.createMenuItem(StandardActions.GROUP_REMOVE_KEEP_SUBGROUPS,
                            new GroupTreeView.ContextAction(StandardActions.GROUP_REMOVE_KEEP_SUBGROUPS, group)),
                    factory.createMenuItem(StandardActions.GROUP_REMOVE_WITH_SUBGROUPS,
                            new GroupTreeView.ContextAction(StandardActions.GROUP_REMOVE_WITH_SUBGROUPS, group))
            );
        } else {
            removeGroup = factory.createMenuItem(StandardActions.GROUP_REMOVE, new GroupTreeView.ContextAction(StandardActions.GROUP_REMOVE, group));
        }

        contextMenu.getItems().addAll(
                factory.createMenuItem(StandardActions.GROUP_EDIT, new ContextAction(StandardActions.GROUP_EDIT, group)),
                removeGroup,
                new SeparatorMenuItem(),
                factory.createMenuItem(StandardActions.GROUP_SUBGROUP_ADD, new ContextAction(StandardActions.GROUP_SUBGROUP_ADD, group)),
                factory.createMenuItem(StandardActions.GROUP_SUBGROUP_REMOVE, new ContextAction(StandardActions.GROUP_SUBGROUP_REMOVE, group)),
                factory.createMenuItem(StandardActions.GROUP_SUBGROUP_SORT, new ContextAction(StandardActions.GROUP_SUBGROUP_SORT, group)),
                factory.createMenuItem(StandardActions.GROUP_SUBGROUP_SORT_REVERSE, new ContextAction(StandardActions.GROUP_SUBGROUP_SORT_REVERSE, group)),
                factory.createMenuItem(StandardActions.GROUP_SUBGROUP_SORT_ENTRIES, new ContextAction(StandardActions.GROUP_SUBGROUP_SORT_ENTRIES, group)),
                factory.createMenuItem(StandardActions.GROUP_SUBGROUP_SORT_ENTRIES_REVERSE, new ContextAction(StandardActions.GROUP_SUBGROUP_SORT_ENTRIES_REVERSE, group)),
                new SeparatorMenuItem(),
                factory.createMenuItem(StandardActions.GROUP_ENTRIES_ADD, new ContextAction(StandardActions.GROUP_ENTRIES_ADD, group)),
                factory.createMenuItem(StandardActions.GROUP_ENTRIES_REMOVE, new ContextAction(StandardActions.GROUP_ENTRIES_REMOVE, group))
        );

        contextMenu.getItems().forEach(item -> item.setGraphic(null));
        contextMenu.getStyleClass().add("context-menu");
        return contextMenu;
    }

    private void addNewGroup() {
        viewModel.addNewGroupToRoot();
    }

    private String getFormattedNumber(int hits) {
        if (hits >= 1000000) {
            double millions = hits / 1000000.0;
            return new DecimalFormat("#,##0.#").format(millions) + "m";
        } else if (hits >= 1000) {
            double thousands = hits / 1000.0;
            return new DecimalFormat("#,##0.#").format(thousands) + "k";
        }
        return Integer.toString(hits);
    }

    // ToDo: reflective access, should be removed
    //  Workaround taken from https://github.com/controlsfx/controlsfx/issues/330
    private void setupClearButtonField(CustomTextField customTextField) {
        try {
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

            if ((System.currentTimeMillis() - this.dragStarted) > DRAG_TIME_BEFORE_EXPANDING_MS) {
                // expand or collapse the tree item and reset the time
                this.dragStarted = System.currentTimeMillis();
                this.draggedItem.setExpanded(!this.draggedItem.isExpanded());
            } else {
                // leave the expansion state of the tree item as it is
                this.draggedItem.setExpanded(this.draggedItem.isExpanded());
            }
        }
    }

    private class ContextAction extends SimpleCommand {

        private final StandardActions command;
        private final GroupNodeViewModel group;

        public ContextAction(StandardActions command, GroupNodeViewModel group) {
            this.command = command;
            this.group = group;

            this.executable.bind(BindingsHelper.constantOf(
                    switch (command) {
                        case GROUP_EDIT ->
                                group.isEditable();
                        case GROUP_REMOVE, GROUP_REMOVE_WITH_SUBGROUPS, GROUP_REMOVE_KEEP_SUBGROUPS ->
                                group.isEditable() && group.canRemove();
                        case GROUP_SUBGROUP_ADD ->
                                group.isEditable() && group.canAddGroupsIn()
                                        || group.isRoot();
                        case GROUP_SUBGROUP_REMOVE ->
                                group.isEditable() && group.hasSubgroups() && group.canRemove()
                                        || group.isRoot();
                        case GROUP_SUBGROUP_SORT ->
                                group.isEditable() && group.hasSubgroups() && group.canAddEntriesIn()
                                        || group.isRoot();
                        case GROUP_ENTRIES_ADD, GROUP_ENTRIES_REMOVE ->
                                group.canAddEntriesIn();
                        default ->
                                true;
                    }));
        }

        @Override
        public void execute() {
            switch (command) {
                case GROUP_REMOVE ->
                        viewModel.removeGroupNoSubgroups(group);
                case GROUP_REMOVE_KEEP_SUBGROUPS ->
                        viewModel.removeGroupKeepSubgroups(group);
                case GROUP_REMOVE_WITH_SUBGROUPS ->
                        viewModel.removeGroupAndSubgroups(group);
                case GROUP_EDIT -> {
                    viewModel.editGroup(group);
                    groupTree.refresh();
                }
                case GROUP_SUBGROUP_ADD ->
                        viewModel.addNewSubgroup(group, GroupDialogHeader.SUBGROUP);
                case GROUP_SUBGROUP_REMOVE ->
                        viewModel.removeSubgroups(group);
                case GROUP_SUBGROUP_SORT ->
                        viewModel.sortAlphabeticallyRecursive(group.getGroupNode());
                case GROUP_SUBGROUP_SORT_REVERSE ->
                        viewModel.sortReverseAlphabeticallyRecursive(group.getGroupNode());
                case GROUP_SUBGROUP_SORT_ENTRIES ->
                        viewModel.sortEntriesRecursive(group.getGroupNode());
                case GROUP_SUBGROUP_SORT_ENTRIES_REVERSE ->
                        viewModel.sortReverseEntriesRecursive(group.getGroupNode());
                case GROUP_ENTRIES_ADD ->
                        viewModel.addSelectedEntries(group);
                case GROUP_ENTRIES_REMOVE ->
                        viewModel.removeSelectedEntries(group);
            }
        }
    }

    /**
     * Focus on GroupTree
     */
    public void requestFocusGroupTree() {
        groupTree.requestFocus();
    }
}

package org.jabref.gui.frame;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.skin.TabPaneSkin;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

import org.jabref.gui.DragAndDropDataFormats;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.importer.actions.OpenDatabaseAction;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.groups.GroupTreeNode;

import com.tobiasdiez.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FrameDndHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(FrameDndHandler.class);

    private final TabPane tabPane;
    private final Supplier<Scene> scene;
    private final Supplier<OpenDatabaseAction> openDatabaseAction;
    private final StateManager stateManager;

    public FrameDndHandler(TabPane tabPane,
                           Supplier<Scene> scene,
                           Supplier<OpenDatabaseAction> openDatabaseAction,
                           StateManager stateManager) {
        this.tabPane = tabPane;
        this.scene = scene;
        this.openDatabaseAction = openDatabaseAction;
        this.stateManager = stateManager;
    }

    void initDragAndDrop() {
        tabPane.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);

        Tab dndIndicator = new Tab(Localization.lang("Open files..."), null);
        dndIndicator.getStyleClass().add("drop");

        EasyBind.subscribe(tabPane.skinProperty(), skin -> {
            if (!(skin instanceof TabPaneSkin)) {
                return;
            }
            // Add drag and drop listeners to JabRefFrame
            scene.get().setOnDragOver(event -> onSceneDragOver(event, dndIndicator));
            scene.get().setOnDragEntered(event -> {
                // It is necessary to setOnDragOver for newly opened tabs
                // drag'n'drop on tabs covered dnd on tabbedPane, so dnd on tabs should contain all dnds on tabbedPane
                for (Node destinationTabNode : tabPane.lookupAll(".tab")) {
                    destinationTabNode.setOnDragOver(tabDragEvent -> onTabDragOver(event, tabDragEvent, dndIndicator));
                    destinationTabNode.setOnDragExited(tabDragEvent -> tabPane.getTabs().remove(dndIndicator));
                    destinationTabNode.setOnDragDropped(tabDragEvent -> onTabDragDropped(destinationTabNode, tabDragEvent, dndIndicator));
                }
                event.consume();
            });
            scene.get().setOnDragExited(event -> tabPane.getTabs().remove(dndIndicator));
            scene.get().setOnDragDropped(event -> onSceneDragDropped(event, dndIndicator));
        });
    }

    private void onTabDragDropped(Node destinationTabNode, DragEvent tabDragEvent, Tab dndIndicator) {
        Dragboard dragboard = tabDragEvent.getDragboard();

        if (hasBibFiles(dragboard)) {
            tabPane.getTabs().remove(dndIndicator);
            List<Path> bibFiles = getBibFiles(dragboard);
            OpenDatabaseAction openDatabaseAction = this.openDatabaseAction.get();
            openDatabaseAction.openFiles(bibFiles);
            tabDragEvent.setDropCompleted(true);
            tabDragEvent.consume();
        } else {
            if (stateManager.getActiveDatabase().isEmpty()) {
                LOGGER.warn("Active library is empty when dropping entries");
                return;
            }

            LibraryTab destinationLibraryTab = null;
            for (Tab libraryTab : tabPane.getTabs()) {
                if (libraryTab.getId().equals(destinationTabNode.getId()) &&
                        !tabPane.getSelectionModel().getSelectedItem().equals(libraryTab)) {
                    destinationLibraryTab = (LibraryTab) libraryTab;
                    break;
                }
            }

            if (destinationLibraryTab == null) {
                LOGGER.warn("Failed to find library tab to drop into");
                return;
            }

            if (hasEntries(dragboard)) {
                List<BibEntry> entryCopies = stateManager.getLocalDragboard().getBibEntries().stream()
                                                         .map(BibEntry::new).toList();
                destinationLibraryTab.dropEntry(entryCopies);
            } else if (hasGroups(dragboard)) {
                dropGroups(dragboard, destinationLibraryTab);
            }

            tabDragEvent.consume();
        }
    }

    private void dropGroups(Dragboard dragboard, LibraryTab destinationLibraryTab) {
        List<String> groupPathToSources = getGroups(dragboard);

        copyRootNode(destinationLibraryTab);

        GroupTreeNode destinationLibraryGroupRoot = destinationLibraryTab
                .getBibDatabaseContext()
                .getMetaData()
                .getGroups().get();

        GroupTreeNode groupsTreeNode = stateManager.getActiveDatabase().get()
                                                   .getMetaData()
                                                   .getGroups()
                                                   .get();

        for (String pathToSource : groupPathToSources) {
            GroupTreeNode groupTreeNodeToCopy = groupsTreeNode
                    .getChildByPath(pathToSource)
                    .get();
            copyGroupTreeNode(destinationLibraryTab, destinationLibraryGroupRoot, groupTreeNodeToCopy);
        }
    }

    private boolean hasEntries(Dragboard dragboard) {
        return dragboard.hasContent(DragAndDropDataFormats.ENTRIES);
    }

    private void onTabDragOver(DragEvent event, DragEvent tabDragEvent, Tab dndIndicator) {
        if (hasBibFiles(tabDragEvent.getDragboard()) || hasGroups(tabDragEvent.getDragboard())) {
            tabDragEvent.acceptTransferModes(TransferMode.ANY);
            if (!tabPane.getTabs().contains(dndIndicator)) {
                tabPane.getTabs().add(dndIndicator);
            }
            event.consume();
        } else {
            tabPane.getTabs().remove(dndIndicator);
        }

        if (tabDragEvent.getDragboard().hasContent(DragAndDropDataFormats.ENTRIES)) {
            tabDragEvent.acceptTransferModes(TransferMode.COPY);
            tabDragEvent.consume();
        }
    }

    private void onSceneDragOver(DragEvent event, Tab dndIndicator) {
        if (hasBibFiles(event.getDragboard())) {
            event.acceptTransferModes(TransferMode.ANY);
            if (!tabPane.getTabs().contains(dndIndicator)) {
                tabPane.getTabs().add(dndIndicator);
            }
            event.consume();
        } else {
            tabPane.getTabs().remove(dndIndicator);
        }
        // Accept drag entries from MainTable
        if (event.getDragboard().hasContent(DragAndDropDataFormats.ENTRIES)) {
            event.acceptTransferModes(TransferMode.COPY);
            event.consume();
        }
    }

    private void onSceneDragDropped(DragEvent event, Tab dndIndicator) {
        tabPane.getTabs().remove(dndIndicator);
        List<Path> bibFiles = getBibFiles(event.getDragboard());
        OpenDatabaseAction openDatabaseAction = this.openDatabaseAction.get();
        openDatabaseAction.openFiles(bibFiles);
        event.setDropCompleted(true);
        event.consume();
    }

    private void copyRootNode(LibraryTab destinationLibraryTab) {
        if (destinationLibraryTab.getBibDatabaseContext().getMetaData().getGroups().isPresent()
                && stateManager.getActiveDatabase().isEmpty()) {
            return;
        }

        // a root (all entries) GroupTreeNode
        GroupTreeNode currentLibraryGroupRoot = stateManager.getActiveDatabase().get()
                                                            .getMetaData()
                                                            .getGroups()
                                                            .get()
                                                            .copyNode();

        // add currentLibraryGroupRoot to the Library if it does not have a root.
        destinationLibraryTab.getBibDatabaseContext()
                             .getMetaData()
                             .setGroups(currentLibraryGroupRoot);
    }

    private void copyGroupTreeNode(LibraryTab destinationLibraryTab, GroupTreeNode parent, GroupTreeNode groupTreeNodeToCopy) {
        if (stateManager.getActiveDatabase().isEmpty()) {
            return;
        }

        List<BibEntry> allEntries = stateManager.getActiveDatabase().get().getEntries();
        // add groupTreeNodeToCopy to the parent-- in the first run that will the source/main GroupTreeNode
        GroupTreeNode copiedNode = parent.addSubgroup(groupTreeNodeToCopy.copyNode().getGroup());
        // add all entries of a groupTreeNode to the new library.
        destinationLibraryTab.dropEntry(groupTreeNodeToCopy.getEntriesInGroup(allEntries));
        // List of all children of groupTreeNodeToCopy
        List<GroupTreeNode> children = groupTreeNodeToCopy.getChildren();

        if (!children.isEmpty()) {
            // use recursion to add all subgroups of the original groupTreeNodeToCopy
            for (GroupTreeNode child : children) {
                copyGroupTreeNode(destinationLibraryTab, copiedNode, child);
            }
        }
    }

    private boolean hasBibFiles(Dragboard dragboard) {
        return !getBibFiles(dragboard).isEmpty();
    }

    private List<Path> getBibFiles(Dragboard dragboard) {
        if (!dragboard.hasFiles()) {
            return List.of();
        } else {
            return dragboard.getFiles().stream().map(File::toPath).filter(FileUtil::isBibFile).collect(Collectors.toList());
        }
    }

    private boolean hasGroups(Dragboard dragboard) {
        return !getGroups(dragboard).isEmpty();
    }

    private List<String> getGroups(Dragboard dragboard) {
        if (!dragboard.hasContent(DragAndDropDataFormats.GROUP)) {
            return List.of();
        } else {
            return (List<String>) dragboard.getContent(DragAndDropDataFormats.GROUP);
        }
    }
}

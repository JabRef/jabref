package org.jabref.gui.groups;

import java.awt.Point;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragGestureRecognizer;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.InputEvent;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.Vector;

import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.jabref.model.groups.GroupTreeNode;

public class GroupsTree extends JTree implements DragSourceListener,
        DropTargetListener, DragGestureListener {

    /** distance from component borders from which on autoscrolling starts. */
    private static final int DRAG_SCROLL_ACTIVATION_MARGIN = 10;

    /** number of pixels to scroll each time handler is called. */
    private static final int DRAG_SCROLL_DISTANCE = 5;
    /** minimum interval between two autoscroll events (for limiting speed). */
    private static final long MIN_AUTOSCROLL_INTERVAL = 50L;
    /** max. distance cursor may move in x or y direction while idling. */
    private static final int IDLE_MARGIN = 1;
    /** idle time after which the node below is expanded. */
    private static final long IDLE_TIME_TO_EXPAND_NODE = 1000L;
    /**
     * time of last autoscroll event (for limiting speed).
     */
    private static long lastDragAutoscroll;
    private final GroupSelector groupSelector;
    private final GroupTreeCellRenderer localCellRenderer = new GroupTreeCellRenderer();
    /**
     * the point on which the cursor is currently idling during a drag
     * operation.
     */
    private Point idlePoint;
    /**
     * time since which cursor is idling.
     */
    private long idleStartTime;
    private GroupTreeNodeViewModel dragNode;

    /**
     * @param groupSelector the parent UI component
     */
    public GroupsTree(GroupSelector groupSelector) {
        this.groupSelector = groupSelector;
        DragGestureRecognizer dgr = DragSource.getDefaultDragSource()
                .createDefaultDragGestureRecognizer(this,
                        DnDConstants.ACTION_MOVE, this);
        if (dgr != null) {
            // Eliminates right mouse clicks as valid actions
            dgr.setSourceActions(dgr.getSourceActions() & ~InputEvent.BUTTON3_MASK);
        }
        new DropTarget(this, this);
        setCellRenderer(localCellRenderer);
        setFocusable(false);
        setToggleClickCount(0);
        ToolTipManager.sharedInstance().registerComponent(this);
        setShowsRootHandles(false);
        getSelectionModel().setSelectionMode(
                TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        this.setFocusable(true);
    }

    @Override
    public void dragEnter(DragSourceDragEvent dsde) {
        // ignore
    }

    /** This is for moving of nodes within myself */
    @Override
    public void dragOver(DragSourceDragEvent dsde) {
    }

    @Override
    public void dropActionChanged(DragSourceDragEvent dsde) {
        // ignore
    }

    @Override
    public void dragDropEnd(DragSourceDropEvent dsde) {
        dragNode = null;
    }

    @Override
    public void dragExit(DragSourceEvent dse) {
        // ignore
    }

    @Override
    public void dragEnter(DropTargetDragEvent dtde) {
        // ignore
    }

    /** This handles dragging of nodes (from myself) or entries (from the table) */
    @Override
    public void dragOver(DropTargetDragEvent dtde) {
        /*
        // accept or reject
            if (dtde
                .isDataFlavorSupported(TransferableEntrySelection.FLAVOR_INTERNAL)) {
            // check if node accepts explicit assignment
            if (target == null) {
                dtde.rejectDrag();
            } else {
                // this would be the place to check if the dragging entries
                // maybe are in this group already, but I think that's not
                // worth the bother (DropTargetDragEvent does not provide
                // access to the drag object)...
                // it might even be irritating to the user.
                if (target.getNode().getGroup() instanceof GroupEntryChanger) {
                    // accept: assignment from EntryTable
                    dtde.acceptDrag(DnDConstants.ACTION_LINK);
                } else {
                    dtde.rejectDrag();
                }
            }
        } else {
            dtde.rejectDrag();
        }
        */
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {
        // ignore
    }

    @Override
    public void drop(DropTargetDropEvent dtde) {
        /*
        try {

            final GroupTreeNodeViewModel target = (GroupTreeNodeViewModel) path
                    .getLastPathComponent();
            // check supported flavors
            final Transferable transferable = dtde.getTransferable();


            } else if (transferable
                    .isDataFlavorSupported(TransferableEntrySelection.FLAVOR_INTERNAL)) {
                final AbstractGroup group = target.getNode().getGroup();
                if (!(target.getNode().getGroup() instanceof GroupEntryChanger)) {
                    // this should never happen, because the same condition
                    // is checked in dragOver already
                    dtde.rejectDrop();
                    return;
                }
                final TransferableEntrySelection selection = (TransferableEntrySelection) transferable
                        .getTransferData(TransferableEntrySelection.FLAVOR_INTERNAL);
                final List<BibEntry> entries = selection.getSelection();
                int assignedEntries = 0;
                for (BibEntry entry : entries) {
                    if (!target.getNode().getGroup().contains(entry)) {
                        ++assignedEntries;
                    }
                }

                // warn if assignment has undesired side effects (modifies a
                // field != keywords)
                if (!WarnAssignmentSideEffects.warnAssignmentSideEffects(group, groupSelector.frame))
                 {
                    return; // user aborted operation
                }

                // if an editor is showing, its fields must be updated
                // after the assignment, and before that, the current
                // edit has to be stored:
                groupSelector.getActiveBasePanel().storeCurrentEdit();

                List<FieldChange> undo = target.addEntriesToGroup(selection.getSelection());
                if (!undo.isEmpty()) {
                    dtde.getDropTargetContext().dropComplete(true);
                    groupSelector.concludeAssignment(UndoableChangeEntriesOfGroup.getUndoableEdit(target, undo),
                            target.getNode(), assignedEntries);
                }
            } else {
                dtde.rejectDrop();
            }
        } catch (IOException | UnsupportedFlavorException ioe) {
            // ignore
        }
        */
    }

    @Override
    public void dragExit(DropTargetEvent dte) {
        setHighlight1Cell(null);
    }

    @Override
    public void dragGestureRecognized(DragGestureEvent dge) {
    }

    /** Returns the first selected node, or null if nothing is selected. */
    private GroupTreeNodeViewModel getSelectedNode() {
        TreePath selectionPath = getSelectionPath();
        return selectionPath == null ? null : (GroupTreeNodeViewModel) selectionPath.getLastPathComponent();
    }

    /**
     * Refresh paths that may have become invalid due to node movements within
     * the tree. This method creates new paths to the last path components
     * (which must still exist) of the specified paths.
     *
     * @param paths
     *            Paths that may have become invalid.
     * @return Refreshed paths that are all valid.
     */
    public Enumeration<TreePath> refreshPaths(Enumeration<TreePath> paths) {
        if (paths == null) {
            return new Vector<TreePath>().elements();
        }

        Vector<TreePath> freshPaths = new Vector<>();
        while (paths.hasMoreElements()) {
            freshPaths.add(((GroupTreeNodeViewModel) paths.nextElement().getLastPathComponent()).getTreePath());
        }
        return freshPaths.elements();
    }

    /**
     * Refresh paths that may have become invalid due to node movements within
     * the tree. This method creates new paths to the last path components
     * (which must still exist) of the specified paths.
     *
     * @param paths
     *            Paths that may have become invalid.
     * @return Refreshed paths that are all valid.
     */
    public TreePath[] refreshPaths(TreePath[] paths) {
        TreePath[] freshPaths = new TreePath[paths.length];
        for (int i = 0; i < paths.length; ++i) {
            freshPaths[i] = ((GroupTreeNodeViewModel) paths[i].getLastPathComponent()).getTreePath();
        }
        return freshPaths;
    }

    /** Highlights the specified cell or disables highlight if cell == null */
    private void setHighlight1Cell(Object cell) {
        localCellRenderer.setHighlight1Cell(cell);
        repaint();
    }

    /**
     * Highlights the specified groups in red
     **/
    public void setOverlappingGroups(List<GroupTreeNode> nodes) {
        Objects.requireNonNull(nodes);
        localCellRenderer.setOverlappingGroups(nodes);
        repaint();
    }

    /**
     * Highlights the specified groups by underlining
     **/
    public void setMatchingGroups(List<GroupTreeNode> nodes) {
        Objects.requireNonNull(nodes);
        localCellRenderer.setMatchingGroups(nodes);
        repaint();
    }

    /** Highlights the specified cell or disables highlight if cell == null */
    public void setHighlightBorderCell(GroupTreeNodeViewModel node) {
        localCellRenderer.setHighlightBorderCell(node);
        repaint();
    }

    /** Sort immediate children of the specified node alphabetically. */
    public void sort(GroupTreeNodeViewModel node, boolean recursive) {
        node.sortChildrenByName(recursive);
    }

    /**
     * Returns true if the node specified by path has at least one descendant
     * that is currently expanded.
     */
    public boolean hasExpandedDescendant(TreePath path) {
        GroupTreeNodeViewModel node = (GroupTreeNodeViewModel) path.getLastPathComponent();
        for (GroupTreeNodeViewModel child : node.getChildren()) {
            if (child.isLeaf()) {
                continue; // don't care about this case
            }
            TreePath pathToChild = path.pathByAddingChild(child);
            if (isExpanded(pathToChild) || hasExpandedDescendant(pathToChild)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the node specified by path has at least one descendant
     * that is currently collapsed.
     */
    public boolean hasCollapsedDescendant(TreePath path) {
        GroupTreeNodeViewModel node = (GroupTreeNodeViewModel) path.getLastPathComponent();
        for (GroupTreeNodeViewModel child : node.getChildren()) {
            if (child.isLeaf()) {
                continue; // don't care about this case
            }
            TreePath pathToChild = path.pathByAddingChild(child);
            if (isCollapsed(pathToChild) || hasCollapsedDescendant(pathToChild)) {
                return true;
            }
        }
        return false;
    }
}

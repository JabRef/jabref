/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.gui.groups;

import java.awt.Cursor;
import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
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
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.Vector;

import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.logic.groups.AbstractGroup;
import net.sf.jabref.logic.groups.EntriesGroupChange;
import net.sf.jabref.logic.groups.MoveGroupChange;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.util.Util;

public class GroupsTree extends JTree implements DragSourceListener,
        DropTargetListener, DragGestureListener {

    /** distance from component borders from which on autoscrolling starts. */
    private static final int DRAG_SCROLL_ACTIVATION_MARGIN = 10;

    /** number of pixels to scroll each time handler is called. */
    private static final int DRAG_SCROLL_DISTANCE = 5;

    /** time of last autoscroll event (for limiting speed). */
    private static long lastDragAutoscroll;

    /** minimum interval between two autoscroll events (for limiting speed). */
    private static final long MIN_AUTOSCROLL_INTERVAL = 50L;

    /**
     * the point on which the cursor is currently idling during a drag
     * operation.
     */
    private Point idlePoint;

    /** time since which cursor is idling. */
    private long idleStartTime;

    /** max. distance cursor may move in x or y direction while idling. */
    private static final int IDLE_MARGIN = 1;

    /** idle time after which the node below is expanded. */
    private static final long IDLE_TIME_TO_EXPAND_NODE = 1000L;

    private final GroupSelector groupSelector;

    private GroupTreeNodeViewModel dragNode;

    private final GroupTreeCellRenderer localCellRenderer = new GroupTreeCellRenderer();


    /**
     * @param groupSelector the parent UI component
     */
    public GroupsTree(GroupSelector groupSelector) {
        // Adjust height according to http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4760081
        FontMetrics metrics = getFontMetrics(getFont());
        setRowHeight(Math.max(getRowHeight(), metrics.getHeight()));

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
        setVisibleRowCount(Globals.prefs.getInt(JabRefPreferences.GROUPS_VISIBLE_ROWS));
        getSelectionModel().setSelectionMode(
                TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
    }

    @Override
    public void dragEnter(DragSourceDragEvent dsde) {
        // ignore
    }

    /** This is for moving of nodes within myself */
    @Override
    public void dragOver(DragSourceDragEvent dsde) {
        final Point p = dsde.getLocation(); // screen coordinates!
        if (p != null) {
            SwingUtilities.convertPointFromScreen(p, this);
            final TreePath path = getPathForLocation(p.x, p.y);
            if (path == null) {
                dsde.getDragSourceContext().setCursor(DragSource.DefaultMoveNoDrop);
                return;
            }
            final GroupTreeNodeViewModel target = (GroupTreeNodeViewModel) path.getLastPathComponent();
            if ((target == null) || dragNode.getNode().isNodeDescendant(target.getNode()) || (dragNode.equals(target))) {
                dsde.getDragSourceContext().setCursor(DragSource.DefaultMoveNoDrop);
                return;
            }
            dsde.getDragSourceContext().setCursor(DragSource.DefaultMoveDrop);
        }
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
        final Point cursor = dtde.getLocation();
        final long currentTime = System.currentTimeMillis();
        if (idlePoint == null) {
            idlePoint = cursor;
        }

        // determine node over which the user is dragging
        final TreePath path = getPathForLocation(cursor.x, cursor.y);
        final GroupTreeNodeViewModel target = path == null ? null : (GroupTreeNodeViewModel) path.getLastPathComponent();
        setHighlight1Cell(target);

        // accept or reject
        if (dtde.isDataFlavorSupported(GroupTreeNodeViewModel.FLAVOR)) {
            // accept: move nodes within tree
            dtde.acceptDrag(DnDConstants.ACTION_MOVE);
        } else if (dtde
                .isDataFlavorSupported(TransferableEntrySelection.FLAVOR_INTERNAL)) {
            // check if node accepts explicit assignment
            if (path == null) {
                dtde.rejectDrag();
            } else {
                // this would be the place to check if the dragging entries
                // maybe are in this group already, but I think that's not
                // worth the bother (DropTargetDragEvent does not provide
                // access to the drag object)...
                // it might even be irritating to the user.
                if (target.getNode().supportsAddingEntries()) {
                    // accept: assignment from EntryTable
                    dtde.acceptDrag(DnDConstants.ACTION_LINK);
                } else {
                    dtde.rejectDrag();
                }
            }
        } else {
            dtde.rejectDrag();
        }

        // auto open
        if ((Math.abs(cursor.x - idlePoint.x) < GroupsTree.IDLE_MARGIN)
                && (Math.abs(cursor.y - idlePoint.y) < GroupsTree.IDLE_MARGIN)) {
            if (((currentTime - idleStartTime) >= GroupsTree.IDLE_TIME_TO_EXPAND_NODE) && (path != null)) {
                expandPath(path);
            }
        } else {
            idlePoint = cursor;
            idleStartTime = currentTime;
        }

        // autoscrolling
        if ((currentTime - GroupsTree.lastDragAutoscroll) < GroupsTree.MIN_AUTOSCROLL_INTERVAL) {
            return;
        }
        final Rectangle r = getVisibleRect();
        final boolean scrollUp = (cursor.y - r.y) < GroupsTree.DRAG_SCROLL_ACTIVATION_MARGIN;
        final boolean scrollDown = ((r.y + r.height) - cursor.y) < GroupsTree.DRAG_SCROLL_ACTIVATION_MARGIN;
        final boolean scrollLeft = (cursor.x - r.x) < GroupsTree.DRAG_SCROLL_ACTIVATION_MARGIN;
        final boolean scrollRight = ((r.x + r.width) - cursor.x) < GroupsTree.DRAG_SCROLL_ACTIVATION_MARGIN;
        if (scrollUp) {
            r.translate(0, -GroupsTree.DRAG_SCROLL_DISTANCE);
        } else if (scrollDown) {
            r.translate(0, +GroupsTree.DRAG_SCROLL_DISTANCE);
        }
        if (scrollLeft) {
            r.translate(-GroupsTree.DRAG_SCROLL_DISTANCE, 0);
        } else if (scrollRight) {
            r.translate(+GroupsTree.DRAG_SCROLL_DISTANCE, 0);
        }
        scrollRectToVisible(r);
        GroupsTree.lastDragAutoscroll = currentTime;
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {
        // ignore
    }

    @Override
    public void drop(DropTargetDropEvent dtde) {
        setHighlight1Cell(null);
        try {
            // initializations common to all flavors
            final Point p = dtde.getLocation();
            final TreePath path = getPathForLocation(p.x, p.y);
            if (path == null) {
                dtde.rejectDrop();
                return;
            }
            final GroupTreeNodeViewModel target = (GroupTreeNodeViewModel) path
                    .getLastPathComponent();
            // check supported flavors
            final Transferable transferable = dtde.getTransferable();
            if (transferable.isDataFlavorSupported(GroupTreeNodeViewModel.FLAVOR)) {
                GroupTreeNodeViewModel source = (GroupTreeNodeViewModel) transferable
                        .getTransferData(GroupTreeNodeViewModel.FLAVOR);
                if (source.equals(target)) {
                    dtde.rejectDrop(); // ignore this
                    return;
                }
                if (source.getNode().isNodeDescendant(target.getNode())) {
                    dtde.rejectDrop();
                    return;
                }
                Enumeration<TreePath> expandedPaths = groupSelector.getExpandedPaths();
                MoveGroupChange undo = new MoveGroupChange(((GroupTreeNodeViewModel)source.getParent()).getNode(),
                        source.getNode().getPositionInParent(), target.getNode(), target.getChildCount());
                source.getNode().moveTo(target.getNode());
                dtde.getDropTargetContext().dropComplete(true);
                // update selection/expansion state
                groupSelector.revalidateGroups(new TreePath[] {source.getTreePath()},
                        refreshPaths(expandedPaths));
                groupSelector.concludeMoveGroup(undo, source);
            } else if (transferable
                    .isDataFlavorSupported(TransferableEntrySelection.FLAVOR_INTERNAL)) {
                final AbstractGroup group = target.getNode().getGroup();
                if (!target.getNode().supportsAddingEntries()) {
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
                if (!Util.warnAssignmentSideEffects(group, groupSelector.frame))
                 {
                    return; // user aborted operation
                }

                // if an editor is showing, its fields must be updated
                // after the assignment, and before that, the current
                // edit has to be stored:
                groupSelector.getActiveBasePanel().storeCurrentEdit();

                Optional<EntriesGroupChange> undo = target.addEntriesToGroup(selection.getSelection());
                if (undo.isPresent()) {
                    dtde.getDropTargetContext().dropComplete(true);
                    groupSelector.revalidateGroups();
                    groupSelector.concludeAssignment(UndoableChangeEntriesOfGroup.getUndoableEdit(target, undo.get()),
                            target.getNode(), assignedEntries);
                }
            } else {
                dtde.rejectDrop();
            }
        } catch (IOException | UnsupportedFlavorException ioe) {
            // ignore
        }
    }

    @Override
    public void dragExit(DropTargetEvent dte) {
        setHighlight1Cell(null);
    }

    @Override
    public void dragGestureRecognized(DragGestureEvent dge) {
        GroupTreeNodeViewModel selectedNode = getSelectedNode();
        if (selectedNode == null)
         {
            return; // nothing to transfer (select manually?)
        }
        Cursor cursor = DragSource.DefaultMoveDrop;
        dragNode = selectedNode;
        dge.getDragSource().startDrag(dge, cursor, selectedNode, this);
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
        if(paths == null) {
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

    /** Highlights the specified cells or disables highlight if cells == null */
    public void setHighlight2Cells(Object[] cells) {
        localCellRenderer.setHighlight2Cells(cells);
        repaint();
    }

    /** Highlights the specified cells or disables highlight if cells == null */
    public void setHighlight3Cells(Object[] cells) {
        localCellRenderer.setHighlight3Cells(cells);
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
            if (child.isLeaf())
             {
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
            if (child.isLeaf())
             {
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

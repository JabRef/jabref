/*
 All programs in this directory and subdirectories are published under the 
 GNU General Public License as described below.

 This program is free software; you can redistribute it and/or modify it 
 under the terms of the GNU General Public License as published by the Free 
 Software Foundation; either version 2 of the License, or (at your option) 
 any later version.

 This program is distributed in the hope that it will be useful, but WITHOUT 
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or 
 FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for 
 more details.

 You should have received a copy of the GNU General Public License along 
 with this program; if not, write to the Free Software Foundation, Inc., 59 
 Temple Place, Suite 330, Boston, MA 02111-1307 USA

 Further information about the GNU GPL is available at:
 http://www.gnu.org/copyleft/gpl.ja.html
 */

package net.sf.jabref.groups;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.InputEvent;
import java.io.IOException;
import java.util.*;

import javax.swing.*;
import javax.swing.tree.*;

public class GroupsTree extends JTree implements DragSourceListener,
        DropTargetListener, DragGestureListener {

    private GroupSelector groupSelector;
    private GroupTreeNode dragNode = null;

    public GroupsTree(GroupSelector groupSelector) {
        this.groupSelector = groupSelector;
        DragGestureRecognizer dgr = DragSource.getDefaultDragSource()
                .createDefaultDragGestureRecognizer(this,
                        DnDConstants.ACTION_MOVE, this);
        // Eliminates right mouse clicks as valid actions
        dgr.setSourceActions(dgr.getSourceActions() & ~InputEvent.BUTTON3_MASK);
        DropTarget dropTarget = new DropTarget(this, this);
    }

    public void dragEnter(DragSourceDragEvent dsde) {
        // ignore
    }

    public void dragOver(DragSourceDragEvent dsde) {
        Point p = dsde.getLocation(); // screen coordinates!
        SwingUtilities.convertPointFromScreen(p, this);
        TreePath path = getPathForLocation(p.x, p.y);
        if (path == null) {
            dsde.getDragSourceContext().setCursor(DragSource.DefaultMoveNoDrop);
            return;
        }
        GroupTreeNode target = (GroupTreeNode) path.getLastPathComponent();
        if (dragNode.isNodeChild(target) || dragNode == target) {
            dsde.getDragSourceContext().setCursor(DragSource.DefaultMoveNoDrop);
            return;
        }
        dsde.getDragSourceContext().setCursor(DragSource.DefaultMoveDrop);
    }

    public void dropActionChanged(DragSourceDragEvent dsde) {
        // ignore
    }

    public void dragDropEnd(DragSourceDropEvent dsde) {
        dragNode = null;
    }

    public void dragExit(DragSourceEvent dse) {
        // ignore
    }

    public void dragEnter(DropTargetDragEvent dtde) {
        // ignore
    }

    public void dragOver(DropTargetDragEvent dtde) {
        // Point p = dtde.getLocation();
        // TreePath path = getPathForLocation(p.x, p.y);
        // if (path == null) {
        // dtde.rejectDrag();
        // return;
        // }
        // GroupTreeNode target = (GroupTreeNode) path.getLastPathComponent();
        // if (dragNode.isNodeChild(target)) {
        // dtde.rejectDrag();
        // return;
        // }
        // dtde.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
    }

    public void dropActionChanged(DropTargetDragEvent dtde) {
        // ignore
    }

    public void drop(DropTargetDropEvent dtde) {
        try {
            Transferable transferable = dtde.getTransferable();
            if (!transferable.isDataFlavorSupported(GroupTreeNode.flavor)) {
                dtde.rejectDrop();
                return;
            }
            Point p = dtde.getLocation();
            TreePath path = getPathForLocation(p.x, p.y);
            if (path == null) {
                dtde.rejectDrop();
                return;
            }
            GroupTreeNode target = (GroupTreeNode) path.getLastPathComponent();
            GroupTreeNode source = (GroupTreeNode) transferable
                    .getTransferData(GroupTreeNode.flavor);
            if (source == target) {
                dtde.rejectDrop(); // ignore this
                return;
            }
            if (source.isNodeChild(target)) {
                dtde.rejectDrop();
                // JZTODO invokeLater: error message; e.g. status line
                return;
            }
            Enumeration expandedPaths = groupSelector.getExpandedPaths(); 
            UndoableMoveGroup undo = new UndoableMoveGroup(groupSelector,
                    groupSelector.getGroupTreeRoot(), source, target, target
                            .getChildCount());
            target.add(source);
            dtde.getDropTargetContext().dropComplete(true);
            // update selection/expansion state
            groupSelector.revalidateGroups(new TreePath[]{new TreePath(source.getPath())},
                    refreshPaths(expandedPaths));
            groupSelector.concludeMoveGroup(undo, source);
        } catch (IOException ioe) {
            // ignore
        } catch (UnsupportedFlavorException e) {
            // ignore
        }
    }

    public void dragExit(DropTargetEvent dte) {
        // ignore
    }

    public void dragGestureRecognized(DragGestureEvent dge) {
        GroupTreeNode selectedNode = getSelectedNode();
        if (selectedNode == null)
            return; // nothing to transfer (select manually?)
        Cursor cursor = dge.getDragAction() == DnDConstants.ACTION_MOVE ? DragSource.DefaultMoveDrop
                : DragSource.DefaultCopyDrop;
        dragNode = selectedNode;
        dge.getDragSource().startDrag(dge, cursor, selectedNode, this);
    }

    /** Returns the first selected node, or null if nothing is selected. */
    public GroupTreeNode getSelectedNode() {
        TreePath selectionPath = getSelectionPath();
        return selectionPath != null ? (GroupTreeNode) selectionPath
                .getLastPathComponent() : null;
    }

    /** Refresh paths that may have become invalid due to node movements
     * within the tree. This method creates new paths to the last path
     * components (which must still exist) of the specified paths.
     * @param paths Paths that may have become invalid.
     * @return Refreshed paths that are all valid.
     */
    public Enumeration refreshPaths(Enumeration paths) {
        Vector freshPaths = new Vector();
        while (paths.hasMoreElements()) {
            freshPaths.add(new TreePath(
                    ((DefaultMutableTreeNode) ((TreePath) paths.nextElement())
                            .getLastPathComponent()).getPath()));
        }
        return freshPaths.elements();
    }
}

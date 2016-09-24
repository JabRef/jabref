package net.sf.jabref.gui.groups;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.border.Border;
import javax.swing.tree.DefaultTreeCellRenderer;

import net.sf.jabref.model.groups.GroupTreeNode;
import net.sf.jabref.model.strings.StringUtil;

/**
 * Renders a GroupTreeNode using its group's getName() method, rather that its toString() method.
 *
 * @author jzieren
 */
public class GroupTreeCellRenderer extends DefaultTreeCellRenderer {

    /** The cell over which the user is currently dragging */
    private Object highlight1Cell;
    private List<GroupTreeNode> overlappingGroups = new ArrayList<>();
    private List<GroupTreeNode> matchingGroups = new ArrayList<>();
    private Object highlightBorderCell;


    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf,
            int row, boolean tmpHasFocus) {
        // show as selected
        selected = (Objects.equals(highlight1Cell, value)) || sel;
        Component c = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, tmpHasFocus);
        // this is sometimes called from deep within somewhere, with a dummy
        // value (probably for layout etc.), so we've got to check here!
        if (!(value instanceof GroupTreeNodeViewModel)) {
            return c;
        }
        if (!(c instanceof JLabel)) {
            return c; // sanity check
        }

        GroupTreeNodeViewModel viewModel = (GroupTreeNodeViewModel) value;
        JLabel label = (JLabel) c;

        Border border;
        if (Objects.equals(highlightBorderCell, value)) {
            border = BorderFactory.createLineBorder(Color.BLACK);
        } else {
            border = BorderFactory.createEmptyBorder();
        }
        if (label.getBorder() != border) {
            label.setBorder(border);
        }

        Boolean red = printInRed(viewModel) && !selected; // do not print currently selected node in red
        Boolean underlined = printUnderlined(viewModel);
        StringBuilder sb = new StringBuilder(60);
        sb.append("<html>");
        if (red) {
            sb.append("<font color=\"#FF0000\">");
        }
        if (underlined) {
            sb.append("<u>");
        }
        if (viewModel.printInItalics()) {
            sb.append("<i>");
        }
        sb.append(StringUtil.quoteForHTML(viewModel.getText()));
        if (viewModel.printInItalics()) {
            sb.append("</i>");
        }
        if (underlined) {
            sb.append("</u>");
        }
        if (red) {
            sb.append("</font>");
        }
        sb.append("</html>");

        String text = sb.toString();
        if (!label.getText().equals(text)) {
            label.setText(text);
        }
        label.setToolTipText(viewModel.getDescription());

        Icon icon = viewModel.getIcon();
        if (label.getIcon() != icon) {
            label.setIcon(icon);
        }
        return c;
    }

    private boolean printInRed(GroupTreeNodeViewModel viewModel) {
        if(viewModel.isAllEntriesGroup()) {
            // Do not print all entries group in red
            return false;
        }

        return overlappingGroups.contains(viewModel.getNode());
    }

    private boolean printUnderlined(GroupTreeNodeViewModel viewModel) {
        if(viewModel.isAllEntriesGroup()) {
            // Do not underline all entries group
            return false;
        }
        return matchingGroups.contains(viewModel.getNode());
    }

    /**
     * For use when dragging: The specified cell is always rendered as selected.
     *
     * @param cell The cell over which the user is currently dragging.
     */
    public void setHighlight1Cell(Object cell) {
        this.highlight1Cell = cell;
    }

    /**
     * Highlights the specified groups in red.
     */
    public void setOverlappingGroups(List<GroupTreeNode> nodes) {
        Objects.requireNonNull(nodes);
        this.overlappingGroups = nodes;
    }

    /**
     * Highlights the specified groups by underlining.
     */
    public void setMatchingGroups(List<GroupTreeNode> nodes) {
        Objects.requireNonNull(nodes);
        this.matchingGroups = nodes;
    }

    /**
     * Highlights the specified cells (by drawing a border around it), or disables highlight if highlightBorderCell ==
     * null.
     */
    public void setHighlightBorderCell(Object highlightBorderCell) {
        this.highlightBorderCell = highlightBorderCell;
    }
}

package net.sf.jabref.groups;

import java.io.*;
import javax.swing.*;
import javax.swing.tree.*;
import net.sf.jabref.Util;
import net.sf.jabref.groups.*;

/**
 * A node in the groups tree that holds exactly one AbstractGroup.
 * 
 * @author zieren
 */
public class GroupTreeNode extends DefaultMutableTreeNode {
    /**
     * Creates this node and associates the specified group with it.
     */
    public GroupTreeNode(AbstractGroup group) {
        setGroup(group);
    }

    /**
     * @return The group associated with this node.
     */
    public AbstractGroup getGroup() {
        return (AbstractGroup) getUserObject();
    }

    /**
     * Associates the specified group with this node.
     */
    public void setGroup(AbstractGroup group) {
        setUserObject(group);
    }

    /**
     * Returns a textual representation of this node and its children. This
     * representation contains both the tree structure and the textual
     * representations of the group associated with each node. It thus allows a
     * complete reconstruction of this object and its children.
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        if (getChildCount() > 0)
            sb.append("(");
        sb.append(Util.quote(getGroup().toString(), "(),;", '\\'));
        for (int i = 0; i < getChildCount(); ++i) {
            sb.append("," + getChildAt(i).toString());
        }
        if (getChildCount() > 0)
            sb.append(")");
        return sb.toString();
    }

    /**
     * Parses the textual representation obtained from GroupTreeNode.toString()
     * and recreates that node and all of its children from it.
     * @throws Exception When a group could not be recreated
     */
    public static GroupTreeNode fromString(String s) throws Exception {
        GroupTreeNode root = null;
        GroupTreeNode newNode;
        int i;
        String g;
        while (s.length() > 0) {
            if (s.startsWith("(")) {
                String subtree = getSubtree(s);
                newNode = fromString(subtree);
                // continue after this subtree by removing it
                // and the leading/trailing braces, and
                // the comma (that makes 3) that always trails it
                // unless it's at the end of s anyway.
                i = 3 + subtree.length();
                s = i >= s.length() ? "" : s.substring(i);
            } else {
                i = indexOfUnquoted(s, ',');
                g = i < 0 ? s : s.substring(0, i);
                if (i >= 0)
                    s = s.substring(i + 1);
                else
                    s = "";
                newNode = new GroupTreeNode(AbstractGroup.fromString(Util
                        .unquote(g, '\\')));
            }
            if (root == null) // first node will be root
                root = newNode;
            else
                root.add(newNode);
        }
        return root;
    }

    /**
     * Returns the substring delimited by a pair of matching braces, with the
     * first brace at index 0. Quoted characters are skipped.
     * 
     * @return the matching substring, or "" if not found.
     */
    private static String getSubtree(String s) {
        int i = 1;
        int level = 1;
        while (i < s.length()) {
            switch (s.charAt(i)) {
            case '\\':
                ++i;
                break;
            case '(':
                ++level;
                break;
            case ')':
                --level;
                if (level == 0)
                    return s.substring(1, i);
                break;
            }
            ++i;
        }
        return "";
    }

    /**
     * Returns the index of the first occurence of c, skipping quoted special
     * characters (escape character: '\\').
     * 
     * @param s
     *            The String to search in.
     * @param c
     *            The character to search
     * @return The index of the first unescaped occurence of c in s, or -1 if
     *         not found.
     */
    private static int indexOfUnquoted(String s, char c) {
        int i = 0;
        while (i < s.length()) {
            if (s.charAt(i) == '\\') {
                ++i; // skip quoted special
            } else {
                if (s.charAt(i) == c)
                    return i;
            }
            ++i;
        }
        return -1;
    }

    /**
     * Creates a deep copy of this node and all of its children, including all
     * groups.
     * 
     * @return This object's deep copy.
     */
    public GroupTreeNode deepCopy() {
        GroupTreeNode copy = new GroupTreeNode(getGroup());
        for (int i = 0; i < getChildCount(); ++i)
            copy.add(((GroupTreeNode) getChildAt(i)).deepCopy());
        return copy;
    }

    /**
     * @return An indexed path from the root node to this node. The elements in
     *         the returned array represent the child index of each node in the
     *         path. If this node is the root node, the returned array has zero
     *         elements.
     */
    public int[] getIndexedPath() {
        TreeNode[] path = getPath();
        int[] indexedPath = new int[path.length - 1];
        for (int i = 1; i < path.length; ++i)
            indexedPath[i - 1] = path[i - 1].getIndex(path[i]);
        return indexedPath;
    }

    /**
     * @param indexedPath
     *            A sequence of child indices that describe a path from this
     *            node to one of its desendants. Be aware that if <b>indexedPath
     *            </b> was obtained by getIndexedPath(), this node should
     *            usually be the root node.
     * @return The descendant found by evaluating <b>indexedPath </b>. If the
     *         path could not be traversed completely (i.e. one of the child
     *         indices did not exist), null will be returned.
     */
    public GroupTreeNode getDescendant(int[] indexedPath) {
        GroupTreeNode cursor = this;
        for (int i = 0; i < indexedPath.length && cursor != null; ++i)
            cursor = (GroupTreeNode) cursor.getChildAt(indexedPath[i]);
        return cursor;
    }
}

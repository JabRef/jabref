package antlr.debug.misc;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * $Id$
 */

import antlr.collections.AST;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

public class JTreeASTModel implements TreeModel {

    AST root = null;

    public JTreeASTModel(AST t) {
        if (t == null) {
            throw new IllegalArgumentException("root is null");
        }
        root = t;
    }

    public void addTreeModelListener(TreeModelListener l) {
    }

    public Object getChild(Object parent, int index) {
        if (parent == null) {
            return null;
        }
        AST p = (AST)parent;
        AST c = p.getFirstChild();
        if (c == null) {
            throw new ArrayIndexOutOfBoundsException("node has no children");
        }
        int i = 0;
        while (c != null && i < index) {
            c = c.getNextSibling();
            i++;
        }
        return c;
    }

    public int getChildCount(Object parent) {
        if (parent == null) {
            throw new IllegalArgumentException("root is null");
        }
        AST p = (AST)parent;
        AST c = p.getFirstChild();
        int i = 0;
        while (c != null) {
            c = c.getNextSibling();
            i++;
        }
        return i;
    }

    public int getIndexOfChild(Object parent, Object child) {
        if (parent == null || child == null) {
            throw new IllegalArgumentException("root or child is null");
        }
        AST p = (AST)parent;
        AST c = p.getFirstChild();
        if (c == null) {
            throw new ArrayIndexOutOfBoundsException("node has no children");
        }
        int i = 0;
        while (c != null && c != child) {
            c = c.getNextSibling();
            i++;
        }
        if (c == child) {
            return i;
        }
        throw new java.util.NoSuchElementException("node is not a child");
    }

    public Object getRoot() {
        return root;
    }

    public boolean isLeaf(Object node) {
        if (node == null) {
            throw new IllegalArgumentException("node is null");
        }
        AST t = (AST)node;
        return t.getFirstChild() == null;
    }

    public void removeTreeModelListener(TreeModelListener l) {
    }

    public void valueForPathChanged(TreePath path, Object newValue) {
        System.out.println("heh, who is calling this mystery method?");
    }
}

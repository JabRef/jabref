package antlr.collections.impl;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * $Id$
 */

import antlr.collections.impl.Vector;
import antlr.collections.ASTEnumeration;
import antlr.collections.AST;

import java.util.NoSuchElementException;

public class ASTEnumerator implements antlr.collections.ASTEnumeration {
    /** The list of root nodes for subtrees that match */
    VectorEnumerator nodes;
    int i = 0;


    public ASTEnumerator(Vector v) {
        nodes = new VectorEnumerator(v);
    }

    public boolean hasMoreNodes() {
        synchronized (nodes) {
            return i <= nodes.vector.lastElement;
        }
    }

    public antlr.collections.AST nextNode() {
        synchronized (nodes) {
            if (i <= nodes.vector.lastElement) {
                return (AST)nodes.vector.data[i++];
            }
            throw new NoSuchElementException("ASTEnumerator");
        }
    }
}

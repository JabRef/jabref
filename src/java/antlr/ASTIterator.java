package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * $Id$
 */

import antlr.collections.AST;

public class ASTIterator {
    protected AST cursor = null;
    protected AST original = null;


    public ASTIterator(AST t) {
        original = cursor = t;
    }

    /** Is 'sub' a subtree of 't' beginning at the root? */
    public boolean isSubtree(AST t, AST sub) {
        AST sibling;

        // the empty tree is always a subset of any tree.
        if (sub == null) {
            return true;
        }

        // if the tree is empty, return true if the subtree template is too.
        if (t == null) {
            if (sub != null) return false;
            return true;
        }

        // Otherwise, start walking sibling lists.  First mismatch, return false.
        for (sibling = t;
             sibling != null && sub != null;
             sibling = sibling.getNextSibling(), sub = sub.getNextSibling()) {
            // as a quick optimization, check roots first.
            if (sibling.getType() != sub.getType()) return false;
            // if roots match, do full match test on children.
            if (sibling.getFirstChild() != null) {
                if (!isSubtree(sibling.getFirstChild(), sub.getFirstChild())) return false;
            }
        }
        return true;
    }

    /** Find the next subtree with structure and token types equal to
     * those of 'template'.
     */
    public AST next(AST template) {
        AST t = null;
        AST sibling = null;

        if (cursor == null) {	// do nothing if no tree to work on
            return null;
        }

        // Start walking sibling list looking for subtree matches.
        for (; cursor != null; cursor = cursor.getNextSibling()) {
            // as a quick optimization, check roots first.
            if (cursor.getType() == template.getType()) {
                // if roots match, do full match test on children.
                if (cursor.getFirstChild() != null) {
                    if (isSubtree(cursor.getFirstChild(), template.getFirstChild())) {
                        return cursor;
                    }
                }
            }
        }
        return t;
    }
}

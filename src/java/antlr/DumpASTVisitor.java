package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * $Id$
 */

import java.io.*;

import antlr.collections.AST;

/** Simple class to dump the contents of an AST to the output */
public class DumpASTVisitor implements ASTVisitor {
    protected int level = 0;


    private void tabs() {
        for (int i = 0; i < level; i++) {
            System.out.print("   ");
        }
    }

    public void visit(AST node) {
        // Flatten this level of the tree if it has no children
        boolean flatten = /*true*/ false;
        AST node2;
        for (node2 = node; node2 != null; node2 = node2.getNextSibling()) {
            if (node2.getFirstChild() != null) {
                flatten = false;
                break;
            }
        }

        for (node2 = node; node2 != null; node2 = node2.getNextSibling()) {
            if (!flatten || node2 == node) {
                tabs();
            }
            if (node2.getText() == null) {
                System.out.print("nil");
            }
            else {
                System.out.print(node2.getText());
            }

            System.out.print(" [" + node2.getType() + "] ");

            if (flatten) {
                System.out.print(" ");
            }
            else {
                System.out.println("");
            }

            if (node2.getFirstChild() != null) {
                level++;
                visit(node2.getFirstChild());
                level--;
            }
        }

        if (flatten) {
            System.out.println("");
        }
    }
}

package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/RIGHTS.html
 *
 * $Id$
 */

import antlr.collections.AST;
import antlr.collections.impl.ASTArray;

/** AST Support code shared by TreeParser and Parser.
 *  We use delegation to share code (and have only one
 *  bit of code to maintain) rather than subclassing
 *  or superclassing (forces AST support code to be
 *  loaded even when you don't want to do AST stuff).
 *
 *  Typically, setASTNodeType is used to specify the
 *  type of node to create, but you can override
 *  create to make heterogeneous nodes etc...
 */
public class ASTFactory {
    /** Name of AST class to create during tree construction.
     *  Null implies that the create method should create
     *  a default AST type such as CommonAST.
     */
    protected String theASTNodeType = null;
    protected Class theASTNodeTypeClass = null;


    /** Add a child to the current AST */
    public void addASTChild(ASTPair currentAST, AST child) {
	if (child != null) {
	    if (currentAST.root == null) {
				// Make new child the current root
		currentAST.root = child;
	    } 
	    else {
		if (currentAST.child == null) {
		    // Add new child to current root
		    currentAST.root.setFirstChild(child);
		}
		else {
		    currentAST.child.setNextSibling(child);
		}
	    }
	    // Make new child the current child
	    currentAST.child = child;
	    currentAST.advanceChildToEnd();
	}
    }

    /** Create a new empty AST node; if the user did not specify
     *  an AST node type, then create a default one: CommonAST.
     */
    public AST create() {
	AST t = null;
	if (theASTNodeTypeClass == null) {
	    t = new CommonAST();
	} else {
	    try {
		t = (AST) theASTNodeTypeClass.newInstance(); // make a new one
	    } catch (Exception e) {
		antlr.Tool.warning("Can't create AST Node " + theASTNodeType);
		return null;
	    }
	}
	return t;
    }
    public AST create(int type) { 
	AST t = create();
	t.initialize(type,"");
	return t;	
    }
    public AST create(int type, String txt) { 
	AST t = create();
	t.initialize(type,txt);
	return t;	
    }

    /** Create a new empty AST node; if the user did not specify
     *  an AST node type, then create a default one: CommonAST.
     */
    public AST create(AST tr) { 
	if ( tr==null ) return null;		// create(null) == null
	AST t = create();
	t.initialize(tr);
	return t;	
    }

    public AST create(Token tok) { 
	AST t = create();
	t.initialize(tok);
	return t;	
    }

    /** Copy a single node.  clone() is not used because
     *  we want to return an AST not a plain object...a type
     *  safety issue.  Further, we want to have all AST node
     *  creation go through the factory so creation can be
     *  tracked.  Returns null if t is null.
     */
    public AST dup(AST t) {
	return create(t);		// if t==null, create returns null
    }

    /** Duplicate tree including siblings of root. */
    public AST dupList(AST t) {
	AST result = dupTree(t);            // if t == null, then result==null
	AST nt = result;
	while (t != null) {						// for each sibling of the root
	    t = t.getNextSibling();
	    nt.setNextSibling(dupTree(t));	// dup each subtree, building new tree
	    nt = nt.getNextSibling();
	}
	return result;
    }

    /**Duplicate a tree, assuming this is a root node of a tree--
     * duplicate that node and what's below; ignore siblings of root node.
     */
    public AST dupTree(AST t) {
	AST result = dup(t);		// make copy of root
	// copy all children of root.
	if ( t!=null ) {
	    result.setFirstChild( dupList(t.getFirstChild()) );
	}
	return result;
    }

    /** Make a tree from a list of nodes.  The first element in the
     *  array is the root.  If the root is null, then the tree is
     *  a simple list not a tree.  Handles null children nodes correctly.
     *  For example, build(a, b, null, c) yields tree (a b c).  build(null,a,b)
     *  yields tree (nil a b).
     */
    public AST make(AST[] nodes) {
	if ( nodes==null || nodes.length==0 ) return null;
	AST root = nodes[0];
	AST tail = null;
	if (root != null) {
	    root.setFirstChild(null);	// don't leave any old pointers set
	}
	// link in children;
	for (int i=1; i<nodes.length; i++) {
	    if ( nodes[i]==null ) continue;	// ignore null nodes
	    if (root == null) {
				// Set the root and set it up for a flat list
		root = tail = nodes[i];
	    }
	    else if ( tail==null ) {
		root.setFirstChild(nodes[i]);
		tail = root.getFirstChild();
	    }
	    else {
		tail.setNextSibling(nodes[i]);
		tail = tail.getNextSibling();
	    }
	    // Chase tail to last sibling
	    while (tail.getNextSibling() != null) {
		tail = tail.getNextSibling();
	    }
	}
	return root;
    }

    /** Make a tree from a list of nodes, where the nodes are contained
     * in an ASTArray object
     */
    public AST make(ASTArray nodes) {
	return make(nodes.array);
    }

    /** Make an AST the root of current AST */
    public void makeASTRoot(ASTPair currentAST, AST root) {
	if (root != null) {
	    // Add the current root as a child of new root
	    root.addChild(currentAST.root);
	    // The new current child is the last sibling of the old root
	    currentAST.child = currentAST.root;
	    currentAST.advanceChildToEnd();
	    // Set the new root
	    currentAST.root = root;
	}
    }

    public void setASTNodeType(String t) {
	theASTNodeType = t;
	try {
	    theASTNodeTypeClass = Class.forName(t); // get class def
	} catch (Exception e) {
	    // either class not found,
	    // class is interface/abstract, or
	    // class or initializer is not accessible.
	    antlr.Tool.warning("Can't find/access AST Node type"+t);
	}
    }
}

package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/RIGHTS.html
 *
 * $Id$
 */

import antlr.collections.AST;
import antlr.collections.ASTEnumeration;
import antlr.collections.impl.ASTEnumerator;
import antlr.collections.impl.Vector;
import java.io.Serializable;
import java.io.IOException;
import java.io.Writer;

/**
 * A Child-Sibling Tree.
 *
 * A tree with PLUS at the root and with two children 3 and 4 is
 * structured as:
 *
 *		PLUS
 *		  |
 *		  3 -- 4
 *
 * and can be specified easily in LISP notation as
 *
 * (PLUS 3 4)
 *
 * where every '(' starts a new subtree.
 *
 * These trees are particular useful for translators because of
 * the flexibility of the children lists.  They are also very easy
 * to walk automatically, whereas trees with specific children
 * reference fields can't easily be walked automatically.
 *
 * This class contains the basic support for an AST.
 * Most people will create ASTs that are subclasses of
 * BaseAST or of CommonAST.
 */
public abstract class BaseAST implements AST, Serializable {
    protected BaseAST down;
    protected BaseAST right;

    private static boolean verboseStringConversion = false;
    private static String[] tokenNames = null;
	
    /**Add a node to the end of the child list for this node */
    public void addChild(AST node) {
	if ( node==null ) return;
	BaseAST t = this.down;
	if ( t!=null ) {
	    while ( t.right!=null ) {
		t = t.right;
	    }
	    t.right = (BaseAST)node;
	}
	else {
	    this.down = (BaseAST)node;
	}
    }

    private void doWorkForFindAll(Vector v, AST target, boolean partialMatch) {
	AST sibling;
	
	// Start walking sibling lists, looking for matches.
    siblingWalk:
	for (sibling=this;
	     sibling!=null;
	     sibling=sibling.getNextSibling())
	    {
		if ( (partialMatch && sibling.equalsTreePartial(target)) ||
		     (!partialMatch && sibling.equalsTree(target)) ) {
		    v.appendElement(sibling);
		}
		// regardless of match or not, check any children for matches
		if ( sibling.getFirstChild()!=null ) {
		    ((BaseAST)sibling.getFirstChild()).doWorkForFindAll(v, target, partialMatch);
		}
	    }		
    }

    /** Is node t equal to this in terms of token type and text? */
    public boolean equals(AST t) {
	if ( t==null ) return false;
	return this.getText().equals(t.getText()) &&
	    this.getType() == t.getType();
    }

    /** Is t an exact structural and equals() match of this tree.  The
 *  'this' reference is considered the start of a sibling list.
 */
    public boolean equalsList(AST t) {
	AST sibling;

	// the empty tree is not a match of any non-null tree.
	if (t == null) {
	    return false;
	}

	// Otherwise, start walking sibling lists.  First mismatch, return false.
	for (sibling = this; sibling != null && t != null; sibling = sibling.getNextSibling(), t = t.getNextSibling()) {
	    // as a quick optimization, check roots first.
	    if (!sibling.equals(t)) {
		return false;
	    }
	    // if roots match, do full list match test on children.
	    if (sibling.getFirstChild() != null) {
		if (!sibling.getFirstChild().equalsList(t.getFirstChild())) {
		    return false;
		}
	    }
	    // sibling has no kids, make sure t doesn't either
	    else if (t.getFirstChild() != null) {
		return false;
	    }
	}
	if (sibling == null && t == null) {
	    return true;
	}
	// one sibling list has more than the other
	return false;
    }

    /** Is 'sub' a subtree of this list?
     *  The siblings of the root are NOT ignored.
     */
    public boolean equalsListPartial(AST sub) {
	AST sibling;

	// the empty tree is always a subset of any tree.
	if ( sub==null ) {
	    return true;
	}
	
	// Otherwise, start walking sibling lists.  First mismatch, return false.
	for (sibling=this;
	     sibling!=null&&sub!=null;
	     sibling=sibling.getNextSibling(), sub=sub.getNextSibling())
	    {
		// as a quick optimization, check roots first.
		if ( !sibling.equals(sub) ) return false;
		// if roots match, do partial list match test on children.
		if ( sibling.getFirstChild()!=null ) {
		    if ( !sibling.getFirstChild().equalsListPartial(sub.getFirstChild()) ) return false;
		}	
	    }
	if ( sibling==null && sub!=null ) {
	    // nothing left to match in this tree, but subtree has more
	    return false;
	}
	// either both are null or sibling has more, but subtree doesn't	
	return true;
    }

    /** Is tree rooted at 'this' equal to 't'?  The siblings
     *  of 'this' are ignored.
     */
    public boolean equalsTree(AST t) {
	// check roots first.
	if ( !this.equals(t) ) return false;
	// if roots match, do full list match test on children.
	if ( this.getFirstChild()!=null ) {
	    if ( !this.getFirstChild().equalsList(t.getFirstChild()) ) return false;
	}
	// sibling has no kids, make sure t doesn't either
	else if (t.getFirstChild() != null) {
	    return false;
	}
	return true;		
    }

    /** Is 't' a subtree of the tree rooted at 'this'?  The siblings
     *  of 'this' are ignored. 
     */
    public boolean equalsTreePartial(AST sub) {
	// the empty tree is always a subset of any tree.
	if ( sub==null ) {
	    return true;
	}
	
	// check roots first.
	if ( !this.equals(sub) ) return false;
	// if roots match, do full list partial match test on children.
	if ( this.getFirstChild()!=null ) {
	    if ( !this.getFirstChild().equalsListPartial(sub.getFirstChild()) ) return false;
	}
	return true;		
    }

    /** Walk the tree looking for all exact subtree matches.  Return
     *  an ASTEnumerator that lets the caller walk the list
     *  of subtree roots found herein.
     */
    public ASTEnumeration findAll(AST target) {
	Vector roots = new Vector(10);
	AST sibling;

	// the empty tree cannot result in an enumeration
	if ( target==null ) {
	    return null;
	}

	doWorkForFindAll(roots, target, false);  // find all matches recursively

	return new ASTEnumerator(roots);
    }

    /** Walk the tree looking for all subtrees.  Return
     *  an ASTEnumerator that lets the caller walk the list
     *  of subtree roots found herein.
     */
    public ASTEnumeration findAllPartial(AST sub) {
	Vector roots = new Vector(10);
	AST sibling;

	// the empty tree cannot result in an enumeration
	if ( sub==null ) {
	    return null;
	}

	doWorkForFindAll(roots, sub, true);  // find all matches recursively

	return new ASTEnumerator(roots);
    }

    /** Get the first child of this node; null if not children */
    public AST getFirstChild() {
	return down;
    }

    /** Get the next sibling in line after this one */
    public AST getNextSibling() {
	return right;
    }

    /** Get the token text for this node */
    public String getText() { return ""; }

    /** Get the token type for this node */
    public int getType() { return 0; }

    public abstract void initialize(int t, String txt);

    public abstract void initialize(AST t);

    public abstract void initialize(Token t);

	/** Remove all children */
    public void removeChildren() {
	down = null;
    }

    public void setFirstChild(AST c) {
	down = (BaseAST)c;
    }

    public void setNextSibling(AST n) {
	right = (BaseAST)n;
    }

    /** Set the token text for this node */
    public void setText(String text) {;}

    /** Set the token type for this node */
    public void setType(int ttype) {;}

    public static void setVerboseStringConversion(boolean verbose, String[] names) {
	verboseStringConversion = verbose;
	tokenNames = names;
    }

    public String toString() {
	StringBuffer b = new StringBuffer();
	// if verbose and type name not same as text (keyword probably)
	if ( verboseStringConversion &&
	     !getText().equalsIgnoreCase(tokenNames[getType()]) &&
	     !getText().equalsIgnoreCase(Tool.stripFrontBack(tokenNames[getType()],"\"","\"")) ) {
	    b.append('[');
	    b.append(getText());
	    b.append(",<");
	    b.append(tokenNames[getType()]);
	    b.append(">]");
	    return b.toString();
	}
	return getText();
    }

    /** Print out a child-sibling tree in LISP notation */
    public String toStringList() {
	AST t = this;
	String ts="";
	if ( t.getFirstChild()!=null ) ts+=" (";
	ts += " "+this.toString();
	if ( t.getFirstChild()!=null ) {
	    ts += ((BaseAST)t.getFirstChild()).toStringList();
	}
	if ( t.getFirstChild()!=null ) ts+=" )";
	if ( t.getNextSibling()!=null ) {
	    ts += ((BaseAST)t.getNextSibling()).toStringList();
	}
	return ts;
    }

    public String toStringTree() {
	AST t = this;
	String ts="";
	if ( t.getFirstChild()!=null ) ts+=" (";
	ts += " "+this.toString();
	if ( t.getFirstChild()!=null ) {
	    ts += ((BaseAST)t.getFirstChild()).toStringList();
	}
	if ( t.getFirstChild()!=null ) ts+=" )";
	return ts;
    }

    public static String decode(String text)
    {
	char c, c1, c2, c3, c4, c5;
	StringBuffer n = new StringBuffer();
	for (int i=0; i < text.length(); i++)
	    {
		c = text.charAt(i);
		if (c == '&') {
		    c1 = text.charAt(i+1); c2 = text.charAt(i+2);
		    c3 = text.charAt(i+3); c4 = text.charAt(i+4);
		    c5 = text.charAt(i+5);
				
		    if ( c1 == 'a' && c2 == 'm' && c3 == 'p' && c4 == ';') {
			n.append("&");
			i += 5;
		    }
		    else if ( c1 == 'l' && c2 == 't' && c3 == ';') {
			n.append("<");
			i += 4;
		    }
		    else if ( c1 == 'g' && c2 == 't' && c3 == ';') {
			n.append(">");
			i += 4;
		    }
		    else if ( c1 == 'q' && c2 == 'u' && c3 == 'o' && 
			      c4 == 't' && c5 == ';') {
			n.append("\"");
			i += 6;
		    }
		    else if ( c1 == 'a' && c2 == 'p' && c3 == 'o' && 
			      c4 == 's' && c5 == ';') {
			n.append("'");
			i += 6;
		    }
		    else n.append("&");
		}
		else n.append(c);
	    }
	return new String(n);
    }

    public static String encode(String text)
    {
	char c;
	StringBuffer n = new StringBuffer();
	for (int i=0; i < text.length(); i++)
	    {
		c = text.charAt(i);
		switch (c) {
		case '&' : { n.append("&amp;"); break; }
		case '<' : { n.append("&lt;"); break; }
		case '>' : { n.append("&gt;"); break; }
		case '"' : { n.append("&quot;"); break; }
		case '\'' : { n.append("&apos;"); break; }
		default : { n.append(c); break; }
		}
	    }
	return new String(n);
    }

    public void xmlSerializeNode(Writer out)
	throws IOException
    {
	StringBuffer buf = new StringBuffer(100);
	buf.append("<");
	buf.append(getClass().getName()+" ");
	buf.append("text=\""+encode(getText())+"\" type=\""+
		   getType()+"\"/>");
	out.write(buf.toString());
    }

    public void xmlSerializeRootOpen(Writer out)
	throws IOException
    {
	StringBuffer buf = new StringBuffer(100);
	buf.append("<");
	buf.append(getClass().getName()+" ");
	buf.append("text=\""+encode(getText())+"\" type=\""+
		   getType()+"\">\n");
	out.write(buf.toString());
    }

    public void xmlSerializeRootClose(Writer out)
	throws IOException
    {
	out.write("</"+getClass().getName()+">\n");
    }

    public void xmlSerialize(Writer out) throws IOException
    {
	// print out this node and all siblings
	for (AST node = this; 
	     node != null; 
	     node = node.getNextSibling())
	{
	    if (node.getFirstChild() == null) {
		// print guts (class name, attributes)
		((BaseAST)node).xmlSerializeNode(out);
	    }
	    else {
		((BaseAST)node).xmlSerializeRootOpen(out);
		
		// print children
		((BaseAST)node.getFirstChild()).xmlSerialize(out);
		
		// print end tag
		((BaseAST)node).xmlSerializeRootClose(out);
	    }
	}
    }

}

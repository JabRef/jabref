package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * $Id$
 */

import antlr.collections.AST;
import antlr.collections.impl.ASTArray;

import java.util.Hashtable;
import java.lang.reflect.Constructor;

/** AST Support code shared by TreeParser and Parser.
 *  We use delegation to share code (and have only one
 *  bit of code to maintain) rather than subclassing
 *  or superclassing (forces AST support code to be
 *  loaded even when you don't want to do AST stuff).
 *
 *  Typically, setASTNodeType is used to specify the
 *  homogeneous type of node to create, but you can override
 *  create to make heterogeneous nodes etc...
 */
public class ASTFactory {
    /** Name of AST class to create during tree construction.
     *  Null implies that the create method should create
     *  a default AST type such as CommonAST.  This is for
	 *  homogeneous nodes.
     */
    protected String theASTNodeType = null;
    protected Class theASTNodeTypeClass = null;

	/** How to specify the classname to create for a particular
	 *  token type.  Note that ANTLR allows you to say, for example,
	 *
	    tokens {
         PLUS<AST=PLUSNode>;
         ...
        }
	 *
	 *  and it tracks everything statically.  #[PLUS] will make you
	 *  a PLUSNode w/o use of this table.
	 *
	 *  For tokens that ANTLR cannot track statically like #[i],
	 *  you can use this table to map PLUS (Integer) -> PLUSNode (Class)
	 *  etc... ANTLR sets the class map from the tokens {...} section
	 *  via the ASTFactory(Hashtable) ctor in antlr.Parser.
	 */
	protected Hashtable tokenTypeToASTClassMap = null;

	public ASTFactory() {
	}

	/** Create factory with a specific mapping from token type
	 *  to Java AST node type.  Your subclasses of ASTFactory
	 *  can override and reuse the map stuff.
	 */
	public ASTFactory(Hashtable tokenTypeToClassMap) {
		setTokenTypeToASTClassMap(tokenTypeToClassMap);
	}

	/** Specify an "override" for the Java AST object created for a
	 *  specific token.  It is provided as a convenience so
	 *  you can specify node types dynamically.  ANTLR sets
	 *  the token type mapping automatically from the tokens{...}
	 *  section, but you can change that mapping with this method.
	 *  ANTLR does it's best to statically determine the node
	 *  type for generating parsers, but it cannot deal with
	 *  dynamic values like #[LT(1)].  In this case, it relies
	 *  on the mapping.  Beware differences in the tokens{...}
	 *  section and what you set via this method.  Make sure
	 *  they are the same.
	 *
	 *  Set className to null to remove the mapping.
	 *
	 *  @since 2.7.2
	 */
	public void setTokenTypeASTNodeType(int tokenType, String className)
		throws IllegalArgumentException
	{
		if ( tokenTypeToASTClassMap==null ) {
			tokenTypeToASTClassMap = new Hashtable();
		}
		if ( className==null ) {
			tokenTypeToASTClassMap.remove(new Integer(tokenType));
			return;
		}
		Class c = null;
		try {
			c = Class.forName(className);
			tokenTypeToASTClassMap.put(new Integer(tokenType), c);
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Invalid class, "+className);
		}
	}

	/** For a given token type, what is the AST node object type to create
	 *  for it?
	 *  @since 2.7.2
	 */
	public Class getASTNodeType(int tokenType) {
		// try node specific class
		if ( tokenTypeToASTClassMap!=null ) {
			Class c = (Class)tokenTypeToASTClassMap.get(new Integer(tokenType));
			if ( c!=null ) {
				return c;
			}
		}

		// try a global specified class
		if (theASTNodeTypeClass != null) {
			return theASTNodeTypeClass;
		}

		// default to the common type
		return CommonAST.class;
	}

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
		return create(Token.INVALID_TYPE);
    }

    public AST create(int type) {
		Class c = getASTNodeType(type);
		AST t = create(c);
		if ( t!=null ) {
			t.initialize(type, "");
		}
		return t;
	}

	public AST create(int type, String txt) {
        AST t = create(type);
		if ( t!=null ) {
			t.initialize(type, txt);
		}
        return t;
    }

	/** Create an AST node with the token type and text passed in, but
	 *  with a specific Java object type. Typically called when you
	 *  say @[PLUS,"+",PLUSNode] in an antlr action.
	 *  @since 2.7.2
	 */
	public AST create(int type, String txt, String className) {
        AST t = create(className);
		if ( t!=null ) {
			t.initialize(type, txt);
		}
        return t;
    }

    /** Create a new empty AST node; if the user did not specify
     *  an AST node type, then create a default one: CommonAST.
     */
    public AST create(AST tr) {
        if (tr == null) return null;		// create(null) == null
        AST t = create(tr.getType());
		if ( t!=null ) {
			t.initialize(tr);
		}
        return t;
    }

	public AST create(Token tok) {
        AST t = create(tok.getType());
		if ( t!=null ) {
			t.initialize(tok);
		}
        return t;
    }

	/** ANTLR generates reference to this when you reference a token
	 *  that has a specified heterogeneous AST node type.  This is
	 *  also a special case node creation routine for backward
	 *  compatibility.  Before, ANTLR generated "new T(tokenObject)"
	 *  and so I must call the appropriate constructor not T().
	 *
	 * @since 2.7.2
	 */
	public AST create(Token tok, String className) {
        AST t = createUsingCtor(tok,className);
        return t;
    }

	/**
	 * @since 2.7.2
	 */
	public AST create(String className) {
		Class c = null;
		try {
			c = Class.forName(className);
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Invalid class, "+className);
		}
		return create(c);
	}

	/**
	 * @since 2.7.2
	 */
	protected AST createUsingCtor(Token token, String className) {
		Class c = null;
		AST t = null;
		try {
			c = Class.forName(className);
			Class[] tokenArgType = new Class[] { antlr.Token.class };
			try {
				Constructor ctor = c.getConstructor(tokenArgType);
				t = (AST)ctor.newInstance(new Object[]{token}); // make a new one
			}
			catch (NoSuchMethodException e){
				// just do the regular thing if you can't find the ctor
				// Your AST must have default ctor to use this.
				t = create(c);
				if ( t!=null ) {
					t.initialize(token);
				}
			}
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Invalid class or can't make instance, "+className);
		}
		return t;
	}

	/**
	 * @since 2.7.2
	 */
	protected AST create(Class c) {
		AST t = null;
		try {
			t = (AST)c.newInstance(); // make a new one
		}
		catch (Exception e) {
			error("Can't create AST Node " + c.getName());
			return null;
		}
        return t;
    }

    /** Copy a single node with same Java AST objec type.
	 *  Ignore the tokenType->Class mapping since you know
	 *  the type of the node, t.getClass(), and doing a dup.
	 *
	 *  clone() is not used because we want all AST creation
	 *  to go thru the factory so creation can be
     *  tracked.  Returns null if t is null.
     */
    public AST dup(AST t) {
		if ( t==null ) {
			return null;
		}
		AST dup_t = create(t.getClass());
		dup_t.initialize(t);
		return dup_t;
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
        if (t != null) {
            result.setFirstChild(dupList(t.getFirstChild()));
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
        if (nodes == null || nodes.length == 0) return null;
        AST root = nodes[0];
        AST tail = null;
        if (root != null) {
            root.setFirstChild(null);	// don't leave any old pointers set
        }
        // link in children;
        for (int i = 1; i < nodes.length; i++) {
            if (nodes[i] == null) continue;	// ignore null nodes
            if (root == null) {
                // Set the root and set it up for a flat list
                root = tail = nodes[i];
            }
            else if (tail == null) {
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

    public void setASTNodeClass(String t) {
        theASTNodeType = t;
        try {
            theASTNodeTypeClass = Class.forName(t); // get class def
        }
        catch (Exception e) {
            // either class not found,
            // class is interface/abstract, or
            // class or initializer is not accessible.
            error("Can't find/access AST Node type" + t);
        }
    }

    /** Specify the type of node to create during tree building.
     * 	@deprecated since 2.7.1
     */
    public void setASTNodeType(String t) {
        setASTNodeClass(t);
    }

	public Hashtable getTokenTypeToASTClassMap() {
		return tokenTypeToASTClassMap;
	}

	public void setTokenTypeToASTClassMap(Hashtable tokenTypeToClassMap) {
		this.tokenTypeToASTClassMap = tokenTypeToClassMap;
	}

    /** To change where error messages go, can subclass/override this method
     *  and then setASTFactory in Parser and TreeParser.  This method removes
     *  a prior dependency on class antlr.Tool.
     */
    public void error(String e) {
        System.err.println(e);
    }
}

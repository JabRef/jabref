package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * $Id$
 */

import antlr.collections.impl.Vector;

class RuleSymbol extends GrammarSymbol {
    RuleBlock block;	// list of alternatives
    boolean defined;	// has the rule been defined yet?
    Vector references;	// list of all nodes referencing this rule
    // not strictly needed by generic symbol table
    // but we will almost always analyze/gen code
    String access;	// access specifier for this rule
    String comment;	// A javadoc comment if any.

    public RuleSymbol(String r) {
        super(r);
        references = new Vector();
    }

    public void addReference(RuleRefElement e) {
        references.appendElement(e);
    }

    public RuleBlock getBlock() {
        return block;
    }

    public RuleRefElement getReference(int i) {
        return (RuleRefElement)references.elementAt(i);
    }

    public boolean isDefined() {
        return defined;
    }

    public int numReferences() {
        return references.size();
    }

    public void setBlock(RuleBlock rb) {
        block = rb;
    }

    public void setDefined() {
        defined = true;
    }
}

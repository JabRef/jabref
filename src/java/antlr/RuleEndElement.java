package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * $Id$
 */

/**Contains a list of all places that reference
 * this enclosing rule.  Useful for FOLLOW computations.
 */
class RuleEndElement extends BlockEndElement {
    protected Lookahead[] cache;	// Each rule can cache it's lookahead computation.
    // The FOLLOW(rule) is stored in this cache.
    // 1..k
    protected boolean noFOLLOW;


    public RuleEndElement(Grammar g) {
        super(g);
        cache = new Lookahead[g.maxk + 1];
    }

    public Lookahead look(int k) {
        return grammar.theLLkAnalyzer.look(k, this);
    }

    public String toString() {
        //return " [RuleEnd]";
        return "";
    }
}

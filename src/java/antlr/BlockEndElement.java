package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * $Id$
 */

/**All alternative blocks are "terminated" by BlockEndElements unless
 * they are rule blocks (in which case they use RuleEndElement).
 */
class BlockEndElement extends AlternativeElement {
    protected boolean[] lock;	// for analysis; used to avoid infinite loops
    protected AlternativeBlock block;// ending blocks know what block they terminate


    public BlockEndElement(Grammar g) {
        super(g);
        lock = new boolean[g.maxk + 1];
    }

    public Lookahead look(int k) {
        return grammar.theLLkAnalyzer.look(k, this);
    }

    public String toString() {
        //return " [BlkEnd]";
        return "";
    }
}

package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/RIGHTS.html
 *
 * $Id$
 */

/** A TreeElement is a block with one alternative and a root node */
class TreeElement extends AlternativeBlock {
	GrammarAtom root;


	public TreeElement(Grammar g, int line) {
		super(g, line, false);
	}
	public void generate() {
		grammar.generator.gen(this);
	}
	public Lookahead look(int k) {
		return grammar.theLLkAnalyzer.look(k, this);
	}
	public String toString() {
		String s = " #(" + root;
		Alternative a = (Alternative)alternatives.elementAt(0);
		AlternativeElement p = a.head;
		while ( p!=null ) {
			s += p;
			p = p.next;
		}
		return s + " )";
	}
}

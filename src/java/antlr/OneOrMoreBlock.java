package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/RIGHTS.html
 *
 * $Id$
 */

class OneOrMoreBlock extends BlockWithImpliedExitPath {


	public OneOrMoreBlock(Grammar g) {
		super(g);
	}
	public OneOrMoreBlock(Grammar g, int line) {
		super(g, line);
	}
	public void generate() {
		grammar.generator.gen(this);
	}
	public Lookahead look(int k) {
		return grammar.theLLkAnalyzer.look(k, this);
	}
	public String toString() {
		return super.toString() + "+";
	}
}

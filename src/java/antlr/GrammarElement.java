package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * $Id$
 */

/**A GrammarElement is a generic node in our
 * data structure that holds a grammar in memory.
 * This data structure can be used for static
 * analysis or for dynamic analysis (during parsing).
 * Every node must know which grammar owns it, how
 * to generate code, and how to do analysis.
 */
abstract class GrammarElement {
    public static final int AUTO_GEN_NONE = 1;
    public static final int AUTO_GEN_CARET = 2;
    public static final int AUTO_GEN_BANG = 3;

    /*
	 * Note that Java does static argument type matching to
	 * determine which function to execute on the receiver.
	 * Here, that implies that we cannot simply say
	 * grammar.generator.gen(this) in GrammarElement or
	 * only CodeGenerator.gen(GrammarElement ge) would
	 * ever be called.
	 */
    protected Grammar grammar;
    protected int line;
    protected int column;

    public GrammarElement(Grammar g) {
        grammar = g;
        line = -1;
        column = -1;
    }

    public GrammarElement(Grammar g, Token start) {
        grammar = g;
        line = start.getLine();
        column = start.getColumn();
    }

    public void generate() {
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public Lookahead look(int k) {
        return null;
    }

    public abstract String toString();
}

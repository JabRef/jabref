package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * $Id$
 */

class CharLiteralElement extends GrammarAtom {


    public CharLiteralElement(LexerGrammar g, Token t, boolean inverted, int autoGenType) {
        super(g, t, AUTO_GEN_NONE);
        tokenType = ANTLRLexer.tokenTypeForCharLiteral(t.getText());
        g.charVocabulary.add(tokenType);
        line = t.getLine();
        not = inverted;
        this.autoGenType = autoGenType;
    }

    public void generate() {
        grammar.generator.gen(this);
    }

    public Lookahead look(int k) {
        return grammar.theLLkAnalyzer.look(k, this);
    }
}

package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * $Id$
 */

class TokenRangeElement extends AlternativeElement {
    String label;
    protected int begin = Token.INVALID_TYPE;
    protected int end = Token.INVALID_TYPE;
    protected String beginText;
    protected String endText;

    public TokenRangeElement(Grammar g, Token t1, Token t2, int autoGenType) {
        super(g, t1, autoGenType);
        begin = grammar.tokenManager.getTokenSymbol(t1.getText()).getTokenType();
        beginText = t1.getText();
        end = grammar.tokenManager.getTokenSymbol(t2.getText()).getTokenType();
        endText = t2.getText();
        line = t1.getLine();
    }

    public void generate() {
        grammar.generator.gen(this);
    }

    public String getLabel() {
        return label;
    }

    public Lookahead look(int k) {
        return grammar.theLLkAnalyzer.look(k, this);
    }

    public void setLabel(String label_) {
        label = label_;
    }

    public String toString() {
        if (label != null) {
            return " " + label + ":" + beginText + ".." + endText;
        }
        else {
            return " " + beginText + ".." + endText;
        }
    }
}

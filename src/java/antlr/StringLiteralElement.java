package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * $Id$
 */

class StringLiteralElement extends GrammarAtom {
    // atomText with quotes stripped and escape codes processed
    protected String processedAtomText;


    public StringLiteralElement(Grammar g, Token t, int autoGenType) {
        super(g, t, autoGenType);
        if (!(g instanceof LexerGrammar)) {
            // lexer does not have token types for string literals
            TokenSymbol ts = grammar.tokenManager.getTokenSymbol(atomText);
            if (ts == null) {
                g.antlrTool.error("Undefined literal: " + atomText, grammar.getFilename(), t.getLine(), t.getColumn());
            }
            else {
                tokenType = ts.getTokenType();
            }
        }
        line = t.getLine();

        // process the string literal text by removing quotes and escaping chars
        // If a lexical grammar, add the characters to the char vocabulary
        processedAtomText = new String();
        for (int i = 1; i < atomText.length() - 1; i++) {
            char c = atomText.charAt(i);
            if (c == '\\') {
                if (i + 1 < atomText.length() - 1) {
                    i++;
                    c = atomText.charAt(i);
                    switch (c) {
                        case 'n':
                            c = '\n';
                            break;
                        case 'r':
                            c = '\r';
                            break;
                        case 't':
                            c = '\t';
                            break;
                    }
                }
            }
            if (g instanceof LexerGrammar) {
                ((LexerGrammar)g).charVocabulary.add(c);
            }
            processedAtomText += c;
        }
    }

    public void generate() {
        grammar.generator.gen(this);
    }

    public Lookahead look(int k) {
        return grammar.theLLkAnalyzer.look(k, this);
    }
}

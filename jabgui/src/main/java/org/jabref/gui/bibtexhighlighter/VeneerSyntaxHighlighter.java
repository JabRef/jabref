package org.jabref.gui.bibtexhighlighter;
import java.util.List;

import io.github.kusoroadeolu.veneer.BibTeXLexer;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import org.fxmisc.richtext.CodeArea;

public class VeneerSyntaxHighlighter implements BibTeXSyntaxHighlighter {

    @Override
    public void applyHighlighting(String source, CodeArea codeArea) {
        if (source == null || source.isEmpty() || codeArea == null) {
            return;
        }
        codeArea.setStyleClass(0, codeArea.getLength(), BibTeXStyleClass.DEFAULT.getClassName());

        CharStream stream = CharStreams.fromString(source);
        BibTeXLexer lexer = new BibTeXLexer(stream);
        BufferedTokenStream tokenStream = new BufferedTokenStream(lexer);
        tokenStream.fill();

        List<Token> tokens = tokenStream.getTokens();

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.getType() == Token.EOF) {
                continue;
            }

            int start = token.getStartIndex();
            int end = token.getStopIndex() + 1;
            if (start < 0 || end < 0 || start >= end || end > codeArea.getLength()) {
                continue;
            }

            BibTeXStyleClass style = getStyleClass(token, tokens, i);
            codeArea.setStyleClass(start, end, style.getClassName());
        }
    }


    private BibTeXStyleClass getStyleClass(Token token, List<Token> tokens, int index) {
        int type = token.getType();

        if (type == BibTeXLexer.LINE_COMMENT) {
            return BibTeXStyleClass.COMMENT;
        } else if (type == BibTeXLexer.AT_STRING || type == BibTeXLexer.AT_PREAMBLE || type == BibTeXLexer.AT_COMMENT || type == BibTeXLexer.AT_ENTRY) {
            return BibTeXStyleClass.KEYWORD;
        } else if (type == BibTeXLexer.DQUOTE_STRING || type == BibTeXLexer.BRACE_STRING) {
            return BibTeXStyleClass.STRING;
        } else if (type == BibTeXLexer.NUMBER) {
            return BibTeXStyleClass.NUMBER;
        } else if (isFieldNameToken(token, tokens, index)) {
            return BibTeXStyleClass.FIELD;
        } else if (isCiteKeyToken(token, tokens, index)) {
            return BibTeXStyleClass.KEY;
        } else if (type == BibTeXLexer.NAME_TOKEN) {
            return BibTeXStyleClass.TYPE;
        }
        return BibTeXStyleClass.DEFAULT;
    }

    private boolean isCiteKeyToken(Token token, List<Token> tokens, int index) {
        if (token.getType() != BibTeXLexer.NAME_TOKEN) return false;
        Token prev = previousDefaultToken(tokens, index);
        return prev != null && (prev.getType() == BibTeXLexer.LBRACE || prev.getType() == BibTeXLexer.LPAREN);
    }

    private boolean isFieldNameToken(Token token, List<Token> tokens, int index) {
        if (token.getType() != BibTeXLexer.NAME_TOKEN) return false;
        if (isCiteKeyToken(token, tokens, index)) return false;
        Token next = nextDefaultToken(tokens, index);
        return next != null && next.getType() == BibTeXLexer.EQUALS;
    }

    private Token previousDefaultToken(List<Token> tokens, int index) {
        for (int i = index - 1; i >= 0; i--) {
            Token candidate = tokens.get(i);
            if (candidate.getChannel() == Token.DEFAULT_CHANNEL) {
                return candidate;
            }
        }
        return null;
    }

    private Token nextDefaultToken(List<Token> tokens, int index) {
        for (int i = index + 1; i < tokens.size(); i++) {
            Token candidate = tokens.get(i);
            if (candidate.getType() == Token.EOF) break;
            if (candidate.getChannel() == Token.DEFAULT_CHANNEL) {
                return candidate;
            }
        }
        return null;
    }
}

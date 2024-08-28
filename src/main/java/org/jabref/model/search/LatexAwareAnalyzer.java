package org.jabref.model.search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;

/**
 * Lucene analyzer respecting the special "features" of JabRef.
 * Especially, LaTeX-encoded text.
 */
public class LatexAwareAnalyzer extends Analyzer {
    private final CharArraySet stopWords;
    public LatexAwareAnalyzer(CharArraySet stopWords) {
        this.stopWords = stopWords;
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer source = new WhitespaceTokenizer();
        TokenStream result = new LatexToUnicodeFoldingFilter(source);
        result = new StopFilter(result, stopWords);
        result = new ASCIIFoldingFilter(result);
        result = new LowerCaseFilter(result);
        return new TokenStreamComponents(source, result);
    }
}

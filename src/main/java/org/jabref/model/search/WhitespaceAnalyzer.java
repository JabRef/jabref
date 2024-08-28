package org.jabref.model.search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;

public class WhitespaceAnalyzer extends Analyzer {
    private final CharArraySet stopWords;
    public WhitespaceAnalyzer(CharArraySet stopWords) {
        this.stopWords = stopWords;
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer source = new WhitespaceTokenizer();
        TokenStream result = new LowerCaseFilter(source);
        result = new StopFilter(result, stopWords);
        result = new ASCIIFoldingFilter(result);
        return new TokenStreamComponents(source, result);
    }
}

package org.jabref.model.search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.ngram.NGramTokenFilter;

public class NGramAnalyzer extends Analyzer {
    private final int minGram;
    private final int maxGram;
    private final CharArraySet stopWords;

    public NGramAnalyzer(int minGram, int maxGram, CharArraySet stopWords) {
        this.minGram = minGram;
        this.maxGram = maxGram;
        this.stopWords = stopWords;
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer source = new WhitespaceTokenizer();
        TokenStream result = new LowerCaseFilter(source);
        result = new StopFilter(result, stopWords);
        result = new ASCIIFoldingFilter(result);
        result = new NGramTokenFilter(result, minGram, maxGram, true);
        return new TokenStreamComponents(source, result);
    }
}

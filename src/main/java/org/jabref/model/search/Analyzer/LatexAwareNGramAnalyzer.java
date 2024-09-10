package org.jabref.model.search.Analyzer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.ngram.EdgeNGramTokenFilter;

public class LatexAwareNGramAnalyzer extends Analyzer {
    private final int minGram;
    private final int maxGram;

    public LatexAwareNGramAnalyzer(int minGram, int maxGram) {
        this.minGram = minGram;
        this.maxGram = maxGram;
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer source = new WhitespaceTokenizer();
        TokenStream result = new LatexToUnicodeFoldingFilter(source);
        result = new StopFilter(result, EnglishAnalyzer.ENGLISH_STOP_WORDS_SET);
        result = new ASCIIFoldingFilter(result);
        result = new LowerCaseFilter(result);
        result = new EdgeNGramTokenFilter(result, minGram, maxGram, true);
        return new TokenStreamComponents(source, result);
    }
}

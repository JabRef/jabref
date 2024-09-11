package org.jabref.model.search.Analyzer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;

/**
 * Lucene analyzer respecting the special "features" of JabRef.
 * Especially, LaTeX-encoded text.
 */
public class LatexAwareAnalyzer extends Analyzer {
    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer source = new WhitespaceTokenizer();
        TokenStream result = new LatexToUnicodeFoldingFilter(source);
        result = new StopFilter(result, EnglishAnalyzer.ENGLISH_STOP_WORDS_SET);
        result = new ASCIIFoldingFilter(result);
        result = new LowerCaseFilter(result);
        return new TokenStreamComponents(source, result);
    }
}

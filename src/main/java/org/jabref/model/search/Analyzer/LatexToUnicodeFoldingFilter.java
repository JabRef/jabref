package org.jabref.model.search.Analyzer;

import java.io.IOException;
import java.util.Arrays;

import org.jabref.architecture.AllowedToUseLogic;
import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.layout.format.LatexToUnicodeFormatter;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @implNote Implementation based on {@link org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter}
 */
@AllowedToUseLogic("because it needs access to the LaTeXToUnicodeFormatter")
public final class LatexToUnicodeFoldingFilter extends TokenFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(LatexToUnicodeFoldingFilter.class);
    private static final Formatter FORMATTER = new LatexToUnicodeFormatter();

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final PositionIncrementAttribute posIncAttr =
            addAttribute(PositionIncrementAttribute.class);

    private State state;

    public LatexToUnicodeFoldingFilter(TokenStream input) {
        super(input);
    }

    private record FoldingResult(char[] output, int length) {
    }

    @Override
    public boolean incrementToken() throws IOException {
        if (state != null) {
            restoreState(state);
            posIncAttr.setPositionIncrement(0);
            state = null;
            return true;
        }
        if (input.incrementToken()) {
            final char[] buffer = termAtt.buffer();
            final int length = termAtt.length();
            FoldingResult foldingResult = foldToUnicode(buffer, length);
            termAtt.copyBuffer(foldingResult.output, 0, foldingResult.length);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        state = null;
    }

    /**
     * @param input  The string to fold
     * @param length The number of characters in the input string
     */
    public FoldingResult foldToUnicode(char[] input, int length) {
        FoldingResult result = foldToUnicode(input, 0, length);
        if (result.length != length) {
            // ASCIIFoldingFilter does "state = captureState();"
            // We do not do anything since the index also contains clean LaTeX only.
            // If we capture the state, the result is Synonym(LaTeX, Unicode)
        }
        return result;
    }

    /**
     * @param input    The characters to fold
     * @param inputPos Index of the first character to fold
     * @param length   The number of characters to fold
     */
    public static FoldingResult foldToUnicode(char[] input, int inputPos, int length) {
        char[] subArray = Arrays.copyOfRange(input, inputPos, inputPos + length);
        String s = new String(subArray);
        String result = FORMATTER.format(s);
        LOGGER.trace("Folding {} to {}", s, result);
        return new FoldingResult(result.toCharArray(), result.length());
    }
}

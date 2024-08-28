package org.jabref.model.search;

import java.io.IOException;
import java.util.Arrays;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.layout.format.LatexToUnicodeFormatter;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

/**
 * @implNote Implementation based on {@link org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter}
 */
public class LatexToUnicodeFoldingFilter extends TokenFilter {
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
     * @return
     */
    public FoldingResult foldToUnicode(char[] input, int length) {
        FoldingResult result = foldToUnicode(input, 0, length);
        if (result.length != length) {
            state = captureState();
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

        return new FoldingResult(result.toCharArray(), result.length());
    }
}

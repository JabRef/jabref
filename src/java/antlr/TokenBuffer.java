package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * $Id$
 */

/**A Stream of Token objects fed to the parser from a Tokenizer that can
 * be rewound via mark()/rewind() methods.
 * <p>
 * A dynamic array is used to buffer up all the input tokens.  Normally,
 * "k" tokens are stored in the buffer.  More tokens may be stored during
 * guess mode (testing syntactic predicate), or when LT(i>k) is referenced.
 * Consumption of tokens is deferred.  In other words, reading the next
 * token is not done by conume(), but deferred until needed by LA or LT.
 * <p>
 *
 * @see antlr.Token
 * @see antlr.Tokenizer
 * @see antlr.TokenQueue
 */

import java.io.IOException;

public class TokenBuffer {

    // Token source
    protected TokenStream input;

    // Number of active markers
    int nMarkers = 0;

    // Additional offset used when markers are active
    int markerOffset = 0;

    // Number of calls to consume() since last LA() or LT() call
    int numToConsume = 0;

    // Circular queue
    TokenQueue queue;

    /** Create a token buffer */
    public TokenBuffer(TokenStream input_) {
        input = input_;
        queue = new TokenQueue(1);
    }

    /** Reset the input buffer to empty state */
    public final void reset() {
        nMarkers = 0;
        markerOffset = 0;
        numToConsume = 0;
        queue.reset();
    }

    /** Mark another token for deferred consumption */
    public final void consume() {
        numToConsume++;
    }

    /** Ensure that the token buffer is sufficiently full */
    private final void fill(int amount) throws TokenStreamException {
        syncConsume();
        // Fill the buffer sufficiently to hold needed tokens
        while (queue.nbrEntries < amount + markerOffset) {
            // Append the next token
            queue.append(input.nextToken());
        }
    }

    /** return the Tokenizer (needed by ParseView) */
    public TokenStream getInput() {
        return input;
    }

    /** Get a lookahead token value */
    public final int LA(int i) throws TokenStreamException {
        fill(i);
        return queue.elementAt(markerOffset + i - 1).type;
    }

    /** Get a lookahead token */
    public final Token LT(int i) throws TokenStreamException {
        fill(i);
        return queue.elementAt(markerOffset + i - 1);
    }

    /**Return an integer marker that can be used to rewind the buffer to
     * its current state.
     */
    public final int mark() {
        syncConsume();
//System.out.println("Marking at " + markerOffset);
//try { for (int i = 1; i <= 2; i++) { System.out.println("LA("+i+")=="+LT(i).getText()); } } catch (ScannerException e) {}
        nMarkers++;
        return markerOffset;
    }

    /**Rewind the token buffer to a marker.
     * @param mark Marker returned previously from mark()
     */
    public final void rewind(int mark) {
        syncConsume();
        markerOffset = mark;
        nMarkers--;
//System.out.println("Rewinding to " + mark);
//try { for (int i = 1; i <= 2; i++) { System.out.println("LA("+i+")=="+LT(i).getText()); } } catch (ScannerException e) {}
    }

    /** Sync up deferred consumption */
    private final void syncConsume() {
        while (numToConsume > 0) {
            if (nMarkers > 0) {
                // guess mode -- leave leading tokens and bump offset.
                markerOffset++;
            }
            else {
                // normal mode -- remove first token
                queue.removeFirst();
            }
            numToConsume--;
        }
    }
}

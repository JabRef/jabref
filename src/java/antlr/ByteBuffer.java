package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * $Id$
 */

/**A Stream of characters fed to the lexer from a InputStream that can
 * be rewound via mark()/rewind() methods.
 * <p>
 * A dynamic array is used to buffer up all the input characters.  Normally,
 * "k" characters are stored in the buffer.  More characters may be stored during
 * guess mode (testing syntactic predicate), or when LT(i>k) is referenced.
 * Consumption of characters is deferred.  In other words, reading the next
 * character is not done by conume(), but deferred until needed by LA or LT.
 * <p>
 *
 * @see antlr.CharQueue
 */
// SAS: added this class to handle Binary input w/ FileInputStream

import java.io.InputStream;
import java.io.IOException;

public class ByteBuffer extends InputBuffer {

    // char source
    transient InputStream input;


    /** Create a character buffer */
    public ByteBuffer(InputStream input_) {
        super();
        input = input_;
    }

    /** Ensure that the character buffer is sufficiently full */
    public void fill(int amount) throws CharStreamException {
        try {
            syncConsume();
            // Fill the buffer sufficiently to hold needed characters
            while (queue.nbrEntries < amount + markerOffset) {
                // Append the next character
                queue.append((char)input.read());
            }
        }
        catch (IOException io) {
            throw new CharStreamIOException(io);
        }
    }
}

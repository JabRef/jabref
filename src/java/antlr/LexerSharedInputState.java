package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * $Id$
 */

import java.io.Reader;
import java.io.InputStream;

/** This object contains the data associated with an
 *  input stream of characters.  Multiple lexers
 *  share a single LexerSharedInputState to lex
 *  the same input stream.
 */
public class LexerSharedInputState {
    protected int column = 1;
    protected int line = 1;
    protected int tokenStartColumn = 1;
    protected int tokenStartLine = 1;
    protected InputBuffer input;

    /** What file (if known) caused the problem? */
    protected String filename;

    public int guessing = 0;

    public LexerSharedInputState(InputBuffer inbuf) {
        input = inbuf;
    }

    public LexerSharedInputState(InputStream in) {
        this(new ByteBuffer(in));
    }

    public LexerSharedInputState(Reader in) {
        this(new CharBuffer(in));
    }

    public void reset() {
        column = 1;
        line = 1;
        tokenStartColumn = 1;
        tokenStartLine = 1;
        guessing = 0;
        filename = null;
        input.reset();
    }
}

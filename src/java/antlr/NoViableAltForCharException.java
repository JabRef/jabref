package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * $Id$
 */

public class NoViableAltForCharException extends RecognitionException {
    public char foundChar;

    public NoViableAltForCharException(char c, CharScanner scanner) {
        super("NoViableAlt", scanner.getFilename(),
              scanner.getLine(), scanner.getColumn());
        foundChar = c;
    }

    /** @deprecated As of ANTLR 2.7.2 use {@see #NoViableAltForCharException(char, String, int, int) } */
    public NoViableAltForCharException(char c, String fileName, int line) {
        this(c, fileName, line, -1);
    }

    public NoViableAltForCharException(char c, String fileName, int line, int column) {
        super("NoViableAlt", fileName, line, column);
        foundChar = c;
    }

    /**
     * Returns a clean error message (no line number/column information)
     */
    public String getMessage() {
        String mesg = "unexpected char: ";

        // I'm trying to mirror a change in the C++ stuff.
        // But java seems to lack something convenient isprint-ish..
		  // actually we're kludging around unicode and non unicode savy
		  // output stuff like most terms.. Basically one would want to
		  // be able to tweak the generation of this message.

        if ((foundChar >= ' ') && (foundChar <= '~')) {
            mesg += '\'';
            mesg += foundChar;
            mesg += '\'';
        }
        else {
           mesg += "0x"+Integer.toHexString((int)foundChar).toUpperCase();
        }
        return mesg;
    }
}

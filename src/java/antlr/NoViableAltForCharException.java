package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/RIGHTS.html
 *
 * $Id$
 */

public class NoViableAltForCharException extends RecognitionException {
    public char foundChar;

    public NoViableAltForCharException(char c, CharScanner scanner) {
	super("NoViableAlt");
	foundChar = c;
	this.line = scanner.getLine();
	this.fileName = scanner.getFilename();
    }

    public NoViableAltForCharException(char c, String fileName, int line) {
	super("NoViableAlt");
	foundChar = c;
	this.line = line;
	this.fileName = fileName;
    }
    /**
     * @deprecated As of ANTLR 2.7.0
     */
    public String getErrorMessage()
    {
	return getMessage();
    }
    /**
     * Returns a clean error message (no line number/column information)
     */
    public String getMessage()
    {
	return "unexpected char: "+(char)foundChar;
    }
}

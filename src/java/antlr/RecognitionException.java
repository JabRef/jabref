package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/RIGHTS.html
 *
 * $Id$
 */

public class RecognitionException extends ANTLRException {
    public String fileName;		// not used by treeparsers
    public int line;			// not used by treeparsers
    public int column;			// not used by treeparsers

    public RecognitionException() {
	super("parsing error");
    }

    /**
     * RecognitionException constructor comment.
     * @param s java.lang.String
     */
    public RecognitionException(String s) {
	super(s);
    }

    /**
     * RecognitionException constructor comment.
     * @param s java.lang.String
     */
    public RecognitionException(String s, String fileName, int line) {
	super(s);
	this.fileName = fileName;
	this.line = line;
    }

    public int getColumn() { return column; }

    /** @deprecated As of ANTLR 2.7.0 */
    public String getErrorMessage () { return getMessage(); }

    public String getFilename() {
	return fileName;
    }

    public int getLine() { return line; }

    public String toString() {
	return FileLineFormatter.getFormatter().
	    getFormatString(fileName,line)+getMessage();
    }
}

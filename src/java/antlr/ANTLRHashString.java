package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * $Id$
 */

// class implements a String-like object whose sole purpose is to be
// entered into a lexer HashTable.  It uses a lexer object to get
// information about case sensitivity.

public class ANTLRHashString {
    // only one of s or buf is non-null
    private String s;
    private char[] buf;
    private int len;
    private CharScanner lexer;
    private static final int prime = 151;


    public ANTLRHashString(char[] buf, int length, CharScanner lexer) {
        this.lexer = lexer;
        setBuffer(buf, length);
    }

    // Hash strings constructed this way are unusable until setBuffer or setString are called.
    public ANTLRHashString(CharScanner lexer) {
        this.lexer = lexer;
    }

    public ANTLRHashString(String s, CharScanner lexer) {
        this.lexer = lexer;
        setString(s);
    }

    private final char charAt(int index) {
        return (s != null) ? s.charAt(index) : buf[index];
    }

    // Return true if o is an ANTLRHashString equal to this.
    public boolean equals(Object o) {
        if (!(o instanceof ANTLRHashString) && !(o instanceof String)) {
            return false;
        }

        ANTLRHashString s;
        if (o instanceof String) {
            s = new ANTLRHashString((String)o, lexer);
        }
        else {
            s = (ANTLRHashString)o;
        }
        int l = length();
        if (s.length() != l) {
            return false;
        }
        if (lexer.getCaseSensitiveLiterals()) {
            for (int i = 0; i < l; i++) {
                if (charAt(i) != s.charAt(i)) {
                    return false;
                }
            }
        }
        else {
            for (int i = 0; i < l; i++) {
                if (lexer.toLower(charAt(i)) != lexer.toLower(s.charAt(i))) {
                    return false;
                }
            }
        }
        return true;
    }

    public int hashCode() {
        int hashval = 0;
        int l = length();

        if (lexer.getCaseSensitiveLiterals()) {
            for (int i = 0; i < l; i++) {
                hashval = hashval * prime + charAt(i);
            }
        }
        else {
            for (int i = 0; i < l; i++) {
                hashval = hashval * prime + lexer.toLower(charAt(i));
            }
        }
        return hashval;
    }

    private final int length() {
        return (s != null) ? s.length() : len;
    }

    public void setBuffer(char[] buf, int length) {
        this.buf = buf;
        this.len = length;
        s = null;
    }

    public void setString(String s) {
        this.s = s;
        buf = null;
    }
}

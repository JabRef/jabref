package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * $Id$
 */

import java.util.Hashtable;
import java.util.Enumeration;

import antlr.collections.impl.Vector;

/** Interface that describes the set of defined tokens */
interface TokenManager {
    public Object clone();

    /** define a token symbol */
    public void define(TokenSymbol ts);

    /** Get the name of the token manager */
    public String getName();

    /** Get a token string by index */
    public String getTokenStringAt(int idx);

    /** Get the TokenSymbol for a string */
    public TokenSymbol getTokenSymbol(String sym);

    public TokenSymbol getTokenSymbolAt(int idx);

    /** Get an enumerator over the symbol table */
    public Enumeration getTokenSymbolElements();

    public Enumeration getTokenSymbolKeys();

    /** Get the token vocabulary (read-only).
     * @return A Vector of Strings indexed by token type */
    public Vector getVocabulary();

    /** Is this token manager read-only? */
    public boolean isReadOnly();

    public void mapToTokenSymbol(String name, TokenSymbol sym);

    /** Get the highest token type in use */
    public int maxTokenType();

    /** Get the next unused token type */
    public int nextTokenType();

    public void setName(String n);

    public void setReadOnly(boolean ro);

    /** Is a token symbol defined? */
    public boolean tokenDefined(String symbol);
}

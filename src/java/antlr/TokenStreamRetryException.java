package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * $Id$
 */

/**
 * Aborted recognition of current token. Try to get one again.
 * Used by TokenStreamSelector.retry() to force nextToken()
 * of stream to re-enter and retry.
 */
public class TokenStreamRetryException extends TokenStreamException {
    public TokenStreamRetryException() {
    }
}

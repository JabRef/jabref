package antlr;

/* ANTLR Translator Generator
 * Project led by Terence Parr at http://www.jGuru.com
 * Software rights: http://www.antlr.org/license.html
 *
 * $Id$
 */

class ExceptionHandler {
    // Type of the ANTLR exception class to catch and the variable decl
    protected Token exceptionTypeAndName;
    // The action to be executed when the exception is caught
    protected Token action;


    public ExceptionHandler(Token exceptionTypeAndName_,
                            Token action_) {
        exceptionTypeAndName = exceptionTypeAndName_;
        action = action_;
    }
}

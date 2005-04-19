package net.sf.jabref;

/**
 * A class implementing this interface can provided as a receiver for error messages originating
 * in a thread that can't return any value or throw any exceptions. E.g. net.sf.jabref.DatabaseSearch.
 *
 * The point is that the worker thread doesn't need to know what interface it is working against,
 * since the ErrorMessageDisplay implementer will be responsible for displaying the error message.
 */
public interface ErrorMessageDisplay {

    /**
     * An error has occured.
     * @param errorMessage Error message.
     */
    public void reportError(String errorMessage);

    /**
     * An error has occured.
     * @param errorMessage Error message.
     * @param exception Exception representing the error condition.
     */
    public void reportError(String errorMessage, Exception exception);

}

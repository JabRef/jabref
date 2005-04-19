package net.sf.jabref;

/**
 * A class implementing this interface can provided as a receiver for error messages originating
 * in a thread that can't return any value or throw any exceptions. E.g. net.sf.jabref.DatabaseSearch.
 */
public interface ErrorMessageDisplay {

    public void reportError(String errorMessage);

    public void reportError(String errorMessage, Exception exception);

}

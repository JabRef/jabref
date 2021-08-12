package org.jabref.model.openoffice.uno;

/**
 * Exception used to indicate failure in either
 *
 *  XMultiServiceFactory.createInstance()
 *  XMultiComponentFactory.createInstanceWithContext()
 */
public class CreationException extends Exception {

    public CreationException(String message) {
        super(message);
    }

}

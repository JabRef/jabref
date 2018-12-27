package org.jabref.logic.importer;

public interface OutputPrinter {

    void setStatus(String s);

    void showMessage(String message, String title, int msgType);

    void showMessage(String string);

}

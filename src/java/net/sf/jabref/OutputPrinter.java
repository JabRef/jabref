package net.sf.jabref;

public interface OutputPrinter {

    void setStatus(String s);

    void showMessage(Object message, String title, int msgType);

    void showMessage(String string);

}

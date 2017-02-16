package org.jabref.cli;

import org.jabref.logic.importer.OutputPrinter;

public class SystemOutputPrinter implements OutputPrinter {

    @Override
    public void setStatus(String s) {
        System.out.println(s);
    }

    @Override
    public void showMessage(String message, String title, int msgType) {
        System.out.println(title + ": " + message);
    }

    @Override
    public void showMessage(String message) {
        System.out.println(message);
    }
}

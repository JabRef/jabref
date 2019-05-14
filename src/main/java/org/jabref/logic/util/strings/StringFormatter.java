package org.jabref.logic.util.strings;

public class StringFormatter {

    public String formatErrorMessage(Exception e) {
        String message = e.getMessage();
        StringBuilder errorFormatter = new StringBuilder();
        int wordCounter = 0;
        for (int i = 0; i < message.length(); i++) {
            if (message.charAt(i) == ' ' && ++wordCounter % 25 == 0) {
                errorFormatter.append(" ").append(System.lineSeparator());
                continue;
            }
            errorFormatter.append(message.charAt(i));
        }
        return errorFormatter.toString();
    }
}

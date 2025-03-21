
package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

public class RemoveLatexCommandsFormatter implements LayoutFormatter {

    @Override
    public String format(String field) {
        StringBuilder cleanedField = new StringBuilder();
        boolean inCommand = false;
        int braceLevel = 0;

        for (int i = 0; i < field.length(); i++) {
            char currentChar = field.charAt(i);

            if (currentChar == '\\') {
                if (i + 1 < field.length() && (field.charAt(i + 1) == '\\' || field.charAt(i + 1) == '_')) {
                    cleanedField.append(field.charAt(i + 1));
                    i++;
                } else {
                    inCommand = true;
                }
            } else if (inCommand) {
                if (currentChar == '{') {
                    braceLevel++;
                } else if (currentChar == '}') {
                    braceLevel--;
                    if (braceLevel == 0) {
                        inCommand = false;
                    }
                } else if (!Character.isLetter(currentChar)) {
                    inCommand = false;
                }
            } else if (currentChar != '{' && currentChar != '}') {
                if (inCommand && Character.isWhitespace(currentChar)) {
                    inCommand = false;
                    continue;
                }
                if (currentChar == '&' && i + 1 < field.length() && field.charAt(i + 1) == '#') {
                    cleanedField.append(currentChar);
                } else if (currentChar == '&') {
                    cleanedField.append("&amp;");
                } else {
                    cleanedField.append(currentChar);
                }
            }
        }

        return cleanedField.toString().replaceAll("\\s+", " ").trim();
    }
}

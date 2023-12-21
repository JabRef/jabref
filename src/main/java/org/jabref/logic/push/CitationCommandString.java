package org.jabref.logic.push;

public record CitationCommandString(
        String prefix,
        String delimiter,
        String suffix) {
    private static final String CITE_KEY1 = "key1";
    private static final String CITE_KEY2 = "key2";

    @Override
    public String toString() {
        return prefix + CITE_KEY1 + delimiter + CITE_KEY2 + suffix;
    }

    public static CitationCommandString from(String completeCiteCommand) {

        int indexKey1 = completeCiteCommand.indexOf(CITE_KEY1);
        int indexKey2 = completeCiteCommand.indexOf(CITE_KEY2);
        if (indexKey1 < 0 || indexKey2 < 0 || indexKey2 < (indexKey1 + CITE_KEY1.length())) {
            return new CitationCommandString("", "", "");
        }

        String prefix = completeCiteCommand.substring(0, indexKey1);
        String delim = completeCiteCommand.substring(completeCiteCommand.lastIndexOf(CITE_KEY1) + CITE_KEY1.length(), indexKey2);
        String suffix = completeCiteCommand.substring(completeCiteCommand.lastIndexOf(CITE_KEY2) + CITE_KEY2.length());
        return new CitationCommandString(prefix, delim, suffix);
    }
}

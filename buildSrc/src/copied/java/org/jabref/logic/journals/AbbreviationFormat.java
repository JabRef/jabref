package org.jabref.logic.journals;

import org.apache.commons.csv.CSVFormat;

public final class AbbreviationFormat {

    public static final char DELIMITER = ';';
    public static final char ESCAPE = '\\';
    public static final char QUOTE = '"';

    private AbbreviationFormat() {
    }

    public static CSVFormat getCSVFormat() {
        return CSVFormat.DEFAULT.builder()
                .setIgnoreEmptyLines(true)
                .setDelimiter(DELIMITER)
                .setEscape(ESCAPE)
                .setQuote(QUOTE)
                .setTrim(true)
                .build();
    }
}

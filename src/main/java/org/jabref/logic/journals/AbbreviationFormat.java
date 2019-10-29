package org.jabref.logic.journals;

import org.apache.commons.csv.CSVFormat;

public class AbbreviationFormat {

    public static final char DELIMITER = ';';
    public static final char ESCAPE = '\\';
    public static final char QUOTE = '"';

    public static CSVFormat getCSVFormat() {
        return CSVFormat.DEFAULT
                .withIgnoreEmptyLines(true)
                .withDelimiter(DELIMITER)
                //.withRecordSeparator(recordSeparator)
                //.withNullString(nullString)
                .withEscape(ESCAPE)
                .withQuote(QUOTE)
                .withTrim();
                //.withQuoteMode(quoteMode);
    }
}

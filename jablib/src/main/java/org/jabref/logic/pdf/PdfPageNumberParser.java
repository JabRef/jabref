package org.jabref.logic.pdf;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PdfPageNumberParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfPageNumberParser.class);
    private static final Pattern PAGE_NUMBER_PATTERN = Pattern.compile("\\d+");

    private PdfPageNumberParser() {
    }

    public static Optional<Integer> parseFirstPageNumber(String pages) {
        Matcher matcher = PAGE_NUMBER_PATTERN.matcher(pages);
        if (!matcher.find()) {
            return Optional.empty();
        }

        try {
            return Optional.of(Integer.parseInt(matcher.group()));
        } catch (NumberFormatException exception) {
            LOGGER.debug("Could not parse first page number from '{}'", pages, exception);
            return Optional.empty();
        }
    }
}

package org.jabref.logic.importer.util;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.model.entry.identifier.ArXivIdentifier;
import org.jabref.model.entry.identifier.DOI;

/**
 * Parses identifiers from URLs and plain text.
 * Extracts DOI, arXiv ID, etc. from various URL formats.
 */
public class UrlIdentifierParser {

    private static final Pattern DOI_URL_PATTERN =
            Pattern.compile("https?://(?:dx\\.)?doi\\.org/(.+)");

    private static final Pattern DOI_ACM_PATTERN =
            Pattern.compile("https?://dl\\.acm\\.org/doi/(?:abs/)?(.+)");

    private static final Pattern ARXIV_URL_PATTERN =
            Pattern.compile("https?://arxiv\\.org/(?:abs|pdf)/([\\w.\\-]+?)(?:\\.pdf)?$");

    public static Optional<DOI> parseDOI(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }

        String trimmedInput = input.trim();

        Matcher doiUrlMatcher = DOI_URL_PATTERN.matcher(trimmedInput);
        if (doiUrlMatcher.find()) {
            return DOI.parse(doiUrlMatcher.group(1));
        }

        Matcher acmMatcher = DOI_ACM_PATTERN.matcher(trimmedInput);
        if (acmMatcher.find()) {
            return DOI.parse(acmMatcher.group(1));
        }

        return DOI.parse(trimmedInput);
    }

    public static Optional<ArXivIdentifier> parseArXiv(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }

        String trimmedInput = input.trim();

        Matcher arxivMatcher = ARXIV_URL_PATTERN.matcher(trimmedInput);
        if (arxivMatcher.find()) {
            return ArXivIdentifier.parse(arxivMatcher.group(1));
        }

        return ArXivIdentifier.parse(trimmedInput);
    }
}

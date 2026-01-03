package org.jabref.logic.citation.contextextractor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.model.citation.ReferenceEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PdfReferenceParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfReferenceParser.class);

    private static final String NUMERIC_BRACKETED_REGEX = "\\[\\d{1,3}\\]";
    private static final String NUMERIC_DOTTED_REGEX = "(?:^|\\n)\\d{1,3}\\.\\s";
    private static final String AUTHOR_KEY_REGEX = "\\[[A-Z][a-zA-Z]+\\d{2,4}[a-z]?\\]";

    private static final Pattern NUMERIC_BRACKETED_PATTERN = Pattern.compile(NUMERIC_BRACKETED_REGEX);
    private static final Pattern NUMERIC_BRACKETED_SPLIT_PATTERN = Pattern.compile("(?=" + NUMERIC_BRACKETED_REGEX + ")");
    private static final Pattern NUMERIC_DOTTED_PATTERN = Pattern.compile(NUMERIC_DOTTED_REGEX);
    private static final Pattern NUMERIC_DOTTED_SPLIT_PATTERN = Pattern.compile("(?=" + NUMERIC_DOTTED_REGEX + ")");
    private static final Pattern AUTHOR_KEY_PATTERN = Pattern.compile(AUTHOR_KEY_REGEX);
    private static final Pattern AUTHOR_KEY_SPLIT_PATTERN = Pattern.compile("(?=" + AUTHOR_KEY_REGEX + ")");

    private static final Pattern NUMERIC_MARKER_PATTERN = Pattern.compile(
            "^\\s*\\[?(\\d{1,3})\\]?\\.?\\s+"
    );

    private static final Pattern AUTHOR_KEY_MARKER_PATTERN = Pattern.compile(
            "^\\s*\\[([A-Z][a-zA-Z]+\\d{2,4}[a-z]?)\\]"
    );

    private static final Pattern DOI_PATTERN = Pattern.compile(
            "(?:doi:|DOI:|https?://doi\\.org/|https?://dx\\.doi\\.org/)?(10\\.\\d{4,}/[^\\s,;\"'<>]+)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern URL_PATTERN = Pattern.compile(
            "(https?://[^\\s,;\"'<>\\]\\)]+)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern YEAR_PATTERN = Pattern.compile(
            "\\b((?:19|20)\\d{2})[a-z]?\\b"
    );

    private static final Pattern AUTHORS_PATTERN = Pattern.compile(
            "^([A-Z][a-zA-Z'\\-]+(?:,\\s*[A-Z]\\.?(?:\\s*[A-Z]\\.?)*)?(?:(?:\\s*,\\s*|\\s+and\\s+|\\s*&\\s*)[A-Z][a-zA-Z'\\-]+(?:,\\s*[A-Z]\\.?(?:\\s*[A-Z]\\.?)*)?)*(?:\\s+et\\s+al\\.?)?)"
    );

    private static final Pattern QUOTED_TITLE_PATTERN = Pattern.compile(
            "[\"\\u201C\\u201D]([^\"\\u201C\\u201D]+)[\"\\u201C\\u201D]"
    );

    private static final Pattern JOURNAL_PATTERN = Pattern.compile(
            "(?:In\\s+)?(?:Proceedings\\s+of\\s+(?:the\\s+)?)?([A-Z][^,\\.]+(?:Journal|Conference|Symposium|Workshop|Transactions|Letters|Review|Magazine)[^,\\.]*)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern VOLUME_PAGES_PATTERN = Pattern.compile(
            "(?:vol\\.?|volume)?\\s*(\\d+)(?:\\s*\\(\\d+\\))?\\s*[,:;]?\\s*(?:pp?\\.?|pages?)?\\s*(\\d+(?:\\s*[-–—]\\s*\\d+)?)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PAGES_PATTERN = Pattern.compile(
            "(?:pp?\\.?|pages?)\\s*(\\d+(?:\\s*[-–—]\\s*\\d+)?)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern VOLUME_PATTERN = Pattern.compile(
            "(?:vol\\.?|volume)\\s*(\\d+)",
            Pattern.CASE_INSENSITIVE
    );

    public List<ReferenceEntry> parseReferences(String referenceSectionText) {
        if (referenceSectionText == null || referenceSectionText.isBlank()) {
            return List.of();
        }

        List<ReferenceEntry> references = new ArrayList<>();
        List<String> rawReferences = splitIntoRawReferences(referenceSectionText);

        int index = 1;
        for (String rawReference : rawReferences) {
            try {
                Optional<ReferenceEntry> entry = parseSingleReference(rawReference.trim(), index);
                entry.ifPresent(references::add);
                index++;
            } catch (Exception e) {
                LOGGER.warn("Failed to parse reference: {}", rawReference, e);
            }
        }

        LOGGER.debug("Parsed {} references from text", references.size());
        return references;
    }

    private List<String> splitIntoRawReferences(String text) {
        List<String> references = new ArrayList<>();

        String normalizedText = text
                .replaceAll("\\r\\n", "\n")
                .replaceAll("\\r", "\n")
                .replaceAll("(?m)^\\s+", "")
                .replaceAll("\\n{3,}", "\n\n");

        ReferenceFormat format = detectReferenceFormat(normalizedText);

        switch (format) {
            case NUMERIC_BRACKETED ->
                    references.addAll(splitByPattern(normalizedText, NUMERIC_BRACKETED_SPLIT_PATTERN));
            case NUMERIC_DOTTED ->
                    references.addAll(splitByPattern(normalizedText, NUMERIC_DOTTED_SPLIT_PATTERN));
            case AUTHOR_YEAR ->
                    references.addAll(splitByBlankLinesOrIndentation(normalizedText));
            case AUTHOR_KEY ->
                    references.addAll(splitByPattern(normalizedText, AUTHOR_KEY_SPLIT_PATTERN));
            default ->
                    references.addAll(splitByBlankLinesOrIndentation(normalizedText));
        }

        return references.stream()
                         .map(String::trim)
                         .filter(r -> r.length() > 20)
                         .toList();
    }

    private ReferenceFormat detectReferenceFormat(String text) {
        int numericBracketed = countMatches(text, NUMERIC_BRACKETED_PATTERN);
        int numericDotted = countMatches(text, NUMERIC_DOTTED_PATTERN);
        int authorKey = countMatches(text, AUTHOR_KEY_PATTERN);

        if (numericBracketed >= 3) {
            return ReferenceFormat.NUMERIC_BRACKETED;
        }
        if (numericDotted >= 3) {
            return ReferenceFormat.NUMERIC_DOTTED;
        }
        if (authorKey >= 3) {
            return ReferenceFormat.AUTHOR_KEY;
        }
        return ReferenceFormat.AUTHOR_YEAR;
    }

    private int countMatches(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private List<String> splitByPattern(String text, Pattern pattern) {
        String[] parts = pattern.split(text);
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            if (!part.isBlank()) {
                result.add(part.trim());
            }
        }
        return result;
    }

    private List<String> splitByBlankLinesOrIndentation(String text) {
        List<String> references = new ArrayList<>();

        String[] paragraphs = text.split("\\n\\s*\\n");

        for (String paragraph : paragraphs) {
            String trimmed = paragraph.trim();
            if (!trimmed.isEmpty()) {
                if (looksLikeMultipleReferences(trimmed)) {
                    references.addAll(splitHangingIndentReferences(trimmed));
                } else {
                    references.add(trimmed);
                }
            }
        }

        return references;
    }

    private boolean looksLikeMultipleReferences(String text) {
        String[] lines = text.split("\\n");
        int authorStartCount = 0;
        for (String line : lines) {
            if (AUTHORS_PATTERN.matcher(line.trim()).find() ||
                    NUMERIC_MARKER_PATTERN.matcher(line).find()) {
                authorStartCount++;
            }
        }
        return authorStartCount > 1;
    }

    private List<String> splitHangingIndentReferences(String text) {
        List<String> references = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        String[] lines = text.split("\\n");

        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) {
                continue;
            }

            boolean isNewReference = isNewReferenceStart(trimmedLine);

            if (isNewReference && !current.isEmpty()) {
                references.add(current.toString().trim());
                current = new StringBuilder();
            }

            if (!current.isEmpty()) {
                current.append(" ");
            }
            current.append(trimmedLine);
        }

        if (!current.isEmpty()) {
            references.add(current.toString().trim());
        }

        return references;
    }

    private boolean isNewReferenceStart(String line) {
        if (NUMERIC_MARKER_PATTERN.matcher(line).find()) {
            return true;
        }

        if (AUTHOR_KEY_MARKER_PATTERN.matcher(line).find()) {
            return true;
        }

        if (line.matches("^[A-Z][a-z]+,.*")) {
            return true;
        }

        return AUTHORS_PATTERN.matcher(line).find() && YEAR_PATTERN.matcher(line).find();
    }

    private Optional<ReferenceEntry> parseSingleReference(String referenceText, int index) {
        if (referenceText.isBlank()) {
            return Optional.empty();
        }

        String marker = extractMarker(referenceText, index);
        Optional<String> authors = extractAuthors(referenceText);
        Optional<String> title = extractTitle(referenceText);
        Optional<String> year = extractYear(referenceText);
        Optional<String> journal = extractJournal(referenceText);
        Optional<String> volume = extractVolume(referenceText);
        Optional<String> pages = extractPages(referenceText);
        Optional<String> doi = extractDoi(referenceText);
        Optional<String> url = extractUrl(referenceText);

        return Optional.of(new ReferenceEntry(
                referenceText,
                marker,
                authors,
                title,
                year,
                journal,
                volume,
                pages,
                doi,
                url
        ));
    }

    private String extractMarker(String text, int index) {
        Matcher numericBracketedMatcher = Pattern.compile("^\\s*\\[(\\d{1,3})\\]").matcher(text);
        if (numericBracketedMatcher.find()) {
            return "[" + numericBracketedMatcher.group(1) + "]";
        }

        Matcher numericDottedMatcher = Pattern.compile("^\\s*(\\d{1,3})\\.\\s").matcher(text);
        if (numericDottedMatcher.find()) {
            return "[" + numericDottedMatcher.group(1) + "]";
        }

        Matcher authorKeyMatcher = AUTHOR_KEY_MARKER_PATTERN.matcher(text);
        if (authorKeyMatcher.find()) {
            return "[" + authorKeyMatcher.group(1) + "]";
        }

        Optional<String> authors = extractAuthors(text);
        Optional<String> year = extractYear(text);

        if (authors.isPresent() && year.isPresent()) {
            String firstAuthor = extractFirstAuthorLastName(authors.get());
            if (!firstAuthor.isEmpty()) {
                return "(" + firstAuthor + " " + year.get() + ")";
            }
        }

        return "[" + index + "]";
    }

    private Optional<String> extractAuthors(String text) {
        String cleanedText = text
                .replaceFirst("^\\s*\\[\\d{1,3}\\]\\s*", "")
                .replaceFirst("^\\s*\\d{1,3}\\.\\s*", "")
                .replaceFirst("^\\s*\\[[A-Z][a-zA-Z]+\\d{2,4}[a-z]?\\]\\s*", "")
                .trim();

        Matcher matcher = AUTHORS_PATTERN.matcher(cleanedText);
        if (matcher.find()) {
            String authors = matcher.group(1).trim();
            if (authors.length() > 2 && authors.length() < 500) {
                return Optional.of(normalizeAuthors(authors));
            }
        }

        return Optional.empty();
    }

    private String normalizeAuthors(String authors) {
        return authors
                .replaceAll("\\s+", " ")
                .replaceAll("\\s*&\\s*", " and ")
                .trim();
    }

    private String extractFirstAuthorLastName(String authors) {
        String cleaned = authors
                .replaceAll("\\s+et\\s+al\\.?", "")
                .replaceAll("\\s+and\\s+.*", "")
                .replaceAll("\\s*&\\s*.*", "")
                .trim();

        if (cleaned.contains(",")) {
            return cleaned.split(",")[0].trim();
        }

        String[] parts = cleaned.split("\\s+");
        if (parts.length > 0) {
            return parts[parts.length - 1].replaceAll("[^a-zA-Z]", "");
        }

        return cleaned;
    }

    private Optional<String> extractTitle(String text) {
        Matcher quotedMatcher = QUOTED_TITLE_PATTERN.matcher(text);
        if (quotedMatcher.find()) {
            String title = quotedMatcher.group(1).trim();
            if (isValidTitle(title)) {
                return Optional.of(title);
            }
        }

        String cleanedText = text
                .replaceFirst("^\\s*\\[\\d{1,3}\\]\\s*", "")
                .replaceFirst("^\\s*\\d{1,3}\\.\\s*", "")
                .replaceFirst("^\\s*\\[[A-Z][a-zA-Z]+\\d{2,4}[a-z]?\\]\\s*", "");

        Matcher authorMatcher = AUTHORS_PATTERN.matcher(cleanedText);
        if (authorMatcher.find()) {
            cleanedText = cleanedText.substring(authorMatcher.end()).trim();
        }

        cleanedText = cleanedText
                .replaceFirst("^[,;:]\\s*", "")
                .replaceFirst("^\\(\\d{4}[a-z]?\\)\\.?\\s*", "")
                .trim();

        int periodIndex = findTitleEndPosition(cleanedText);
        if (periodIndex > 0) {
            String potentialTitle = cleanedText.substring(0, periodIndex).trim();
            if (isValidTitle(potentialTitle)) {
                return Optional.of(potentialTitle);
            }
        }

        return Optional.empty();
    }

    private int findTitleEndPosition(String text) {
        if (text.isEmpty()) {
            return -1;
        }

        boolean inParentheses = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '(') {
                inParentheses = true;
            } else if (c == ')') {
                inParentheses = false;
            } else if (c == '.' && !inParentheses) {
                if (i > 0 && i < text.length() - 1) {
                    char prev = text.charAt(i - 1);
                    char next = text.charAt(i + 1);
                    if (Character.isUpperCase(prev) && Character.isWhitespace(next)) {
                        continue;
                    }

                    String before = text.substring(Math.max(0, i - 4), i).toLowerCase();
                    if (before.endsWith("vol") || before.endsWith("fig") || before.endsWith("etc") || before.endsWith("al") || before.endsWith("no") || before.endsWith("pp")) {
                        continue;
                    }
                }
                return i;
            }
        }

        return -1;
    }

    private boolean isValidTitle(String title) {
        if (title == null || title.length() < 10 || title.length() > 500) {
            return false;
        }

        if (!Character.isUpperCase(title.charAt(0)) && !Character.isDigit(title.charAt(0))) {
            return false;
        }

        return title.matches(".*[a-z].*");
    }

    private Optional<String> extractYear(String text) {
        Matcher matcher = YEAR_PATTERN.matcher(text);
        String lastYear = null;

        while (matcher.find()) {
            String year = matcher.group(1);
            int yearInt = Integer.parseInt(year);

            if (yearInt >= 1900 && yearInt <= 2030) {
                lastYear = year;
            }
        }

        return Optional.ofNullable(lastYear);
    }

    private Optional<String> extractJournal(String text) {
        Matcher matcher = JOURNAL_PATTERN.matcher(text);
        if (matcher.find()) {
            String journal = matcher.group(1).trim();

            journal = journal
                    .replaceFirst("^In\\s+", "")
                    .replaceFirst("^Proceedings\\s+of\\s+(the\\s+)?", "")
                    .trim();
            if (journal.length() > 3) {
                return Optional.of(journal);
            }
        }

        Matcher italicMatcher = Pattern.compile("[*_]([^*_]+)[*_]").matcher(text);
        if (italicMatcher.find()) {
            String potential = italicMatcher.group(1).trim();
            if (potential.length() > 5 && !potential.contains(".")) {
                return Optional.of(potential);
            }
        }

        return Optional.empty();
    }

    private Optional<String> extractVolume(String text) {
        Matcher combinedMatcher = VOLUME_PAGES_PATTERN.matcher(text);
        if (combinedMatcher.find()) {
            return Optional.of(combinedMatcher.group(1));
        }

        Matcher volumeMatcher = VOLUME_PATTERN.matcher(text);
        if (volumeMatcher.find()) {
            return Optional.of(volumeMatcher.group(1));
        }

        return Optional.empty();
    }

    private Optional<String> extractPages(String text) {
        Matcher combinedMatcher = VOLUME_PAGES_PATTERN.matcher(text);
        if (combinedMatcher.find() && combinedMatcher.group(2) != null) {
            return Optional.of(normalizePages(combinedMatcher.group(2)));
        }

        Matcher pagesMatcher = PAGES_PATTERN.matcher(text);
        if (pagesMatcher.find()) {
            return Optional.of(normalizePages(pagesMatcher.group(1)));
        }

        return Optional.empty();
    }

    private String normalizePages(String pages) {
        return pages.replaceAll("[–—]", "-").replaceAll("\\s+", "");
    }

    private Optional<String> extractDoi(String text) {
        Matcher matcher = DOI_PATTERN.matcher(text);
        if (matcher.find()) {
            String doi = matcher.group(1).trim();
            doi = doi.replaceAll("[.,;:]+$", "");
            return Optional.of(doi);
        }
        return Optional.empty();
    }

    private Optional<String> extractUrl(String text) {
        if (extractDoi(text).isPresent()) {
            return Optional.empty();
        }

        Matcher matcher = URL_PATTERN.matcher(text);
        if (matcher.find()) {
            String url = matcher.group(1).trim();
            url = url.replaceAll("[.,;:]+$", "");
            return Optional.of(url);
        }
        return Optional.empty();
    }

    private enum ReferenceFormat {
        NUMERIC_BRACKETED,
        NUMERIC_DOTTED,
        AUTHOR_YEAR,
        AUTHOR_KEY
    }
}

package org.jabref.logic.relatedwork;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class RelatedWorkTextParser {
    /// Support [1] / [1-3] / [1, 2, 3]  / [1-3, 7, 9]
    private static final Pattern CITE_PATTERN = Pattern.compile("\\[(\\d{1,3}(?:\\s*(?:,|-|–)\\s*\\d{1,3})*)\\]");
    private static final Pattern SEGMENT_SPLIT_PATTERN = Pattern.compile("(?<=[.!?])\\s+|(?<=[.!?])(?=\\p{Lu})");
    private static final Pattern HYPHENATED_LINE_BREAK_PATTERN = Pattern.compile("-\\R");
    private static final Pattern NEWLINE_WITHOUT_SENTENCE_END_PATTERN = Pattern.compile("(?<![.:])\\R");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern SPACE_BEFORE_PUNCTUATION_PATTERN = Pattern.compile("\\s+([,.;:!?])");
    private static final Pattern TRAILING_CONJUNCTION_WITH_PUNCTUATION_PATTERN = Pattern.compile("\\s+(?:and|or)([.?!])$", Pattern.CASE_INSENSITIVE);
    private static final Pattern TRAILING_CONJUNCTION_PATTERN = Pattern.compile("\\s+(?:and|or)$", Pattern.CASE_INSENSITIVE);

    /// This method first cuts long texts into separate sentence segments, and then parse these sentences.
    public List<RelatedWorkSnippet> parseRelatedWork(String text) {
        String normalizedText = getNormalText(text);

        return SEGMENT_SPLIT_PATTERN.splitAsStream(normalizedText)
                                    .filter(segment -> !segment.isBlank())
                                    .map(this::parseTextSegment)
                                    .flatMap(List::stream)
                                    .toList();
    }

    /// Parse a sentence into citationKeys and text.
    public List<RelatedWorkSnippet> parseTextSegment(String text) {
        List<String> citationMarkers = CITE_PATTERN.matcher(text)
                                                   .results()
                                                   .map(MatchResult::group)
                                                   .flatMap(citation -> splitCitation(citation).stream())
                                                   .toList();

        if (citationMarkers.isEmpty()) {
            return List.of();
        }

        String contextText = extractContextText(text);

        return citationMarkers.stream()
                              .map(citationMarker -> new RelatedWorkSnippet(contextText, citationMarker))
                              .toList();
    }

    /// Split citations groups by comma, and then split into single citation by connector (if contains)
    private List<String> splitCitation(String citation) {
        String content = citation.substring(1, citation.length() - 1);
        List<String> singleCitations = new ArrayList<>();

        for (String part : content.split(",")) {
            part = part.trim();

            if (part.matches("\\d+\\s*[-–]\\s*\\d+")) {
                String[] bounds = part.split("\\s*[-–]\\s*", 2);
                int start = Integer.parseInt(bounds[0]);
                int end = Integer.parseInt(bounds[1]);

                for (int number = start; number <= end; number++) {
                    singleCitations.add("[" + number + "]");
                }
            } else {
                singleCitations.add("[" + Integer.parseInt(part) + "]");
            }
        }

        return singleCitations;
    }

    /// Remove citationKeys
    private String extractContextText(String text) {
        text = CITE_PATTERN.matcher(text).replaceAll(" ");
        text = WHITESPACE_PATTERN.matcher(text.trim()).replaceAll(" ");
        text = SPACE_BEFORE_PUNCTUATION_PATTERN.matcher(text).replaceAll("$1");
        text = TRAILING_CONJUNCTION_WITH_PUNCTUATION_PATTERN.matcher(text).replaceAll("$1");
        return TRAILING_CONJUNCTION_PATTERN.matcher(text).replaceAll("").trim();
    }

    /// Some long words may contain a hyphen and a newline symbol. This method transforms these words into normal ones.
    public String getNormalText(String text) {
        text = HYPHENATED_LINE_BREAK_PATTERN.matcher(text).replaceAll("");
        text = NEWLINE_WITHOUT_SENTENCE_END_PATTERN.matcher(text).replaceAll(" ");
        return WHITESPACE_PATTERN.matcher(text.trim()).replaceAll(" ");
    }
}

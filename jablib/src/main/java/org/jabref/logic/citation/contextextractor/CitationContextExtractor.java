package org.jabref.logic.citation.contextextractor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.model.citation.CitationContext;
import org.jabref.model.citation.CitationContextList;

public class CitationContextExtractor {

    private static final Pattern AUTHOR_YEAR_PATTERN = Pattern.compile(
            "\\(([A-Z][a-zA-Z'\\-]+(?:\\s+(?:and|&)\\s+[A-Z][a-zA-Z'\\-]+)*(?:\\s+et\\s+al\\.?)?)(?:[,\\s]+)(\\d{4}[a-z]?)\\)",
            Pattern.UNICODE_CHARACTER_CLASS
    );

    private static final Pattern INLINE_AUTHOR_YEAR_PATTERN = Pattern.compile(
            "([A-Z][a-zA-Z'\\-]+(?:\\s+(?:and|&)\\s+[A-Z][a-zA-Z'\\-]+)*(?:\\s+et\\s+al\\.?)?)\\s*\\((\\d{4}[a-z]?)\\)",
            Pattern.UNICODE_CHARACTER_CLASS
    );

    private static final Pattern NUMERIC_PATTERN = Pattern.compile(
            "\\[(\\d+(?:\\s*[-,]\\s*\\d+)*)\\]"
    );

    private static final Pattern AUTHOR_KEY_PATTERN = Pattern.compile(
            "\\[([A-Z][a-zA-Z]+\\d{2,4}[a-z]?)\\]"
    );

    private final int contextSentencesBefore;
    private final int contextSentencesAfter;

    public CitationContextExtractor() {
        this(1, 1);
    }

    public CitationContextExtractor(int contextSentencesBefore, int contextSentencesAfter) {
        this.contextSentencesBefore = contextSentencesBefore;
        this.contextSentencesAfter = contextSentencesAfter;
    }

    public CitationContextList extractContexts(String text, String sourceCitationKey) {
        CitationContextList result = new CitationContextList(sourceCitationKey);

        List<SentencePosition> sentences = splitIntoSentences(text);

        result.addAll(extractWithPattern(text, AUTHOR_YEAR_PATTERN, sentences, sourceCitationKey, true));
        result.addAll(extractWithPattern(text, INLINE_AUTHOR_YEAR_PATTERN, sentences, sourceCitationKey, true));
        result.addAll(extractWithPattern(text, NUMERIC_PATTERN, sentences, sourceCitationKey, false));
        result.addAll(extractWithPattern(text, AUTHOR_KEY_PATTERN, sentences, sourceCitationKey, false));

        return result;
    }

    private List<CitationContext> extractWithPattern(
            String text,
            Pattern pattern,
            List<SentencePosition> sentences,
            String sourceCitationKey,
            boolean combineGroups
    ) {
        List<CitationContext> contexts = new ArrayList<>();
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String citationMarker = matcher.group(0);
            int position = matcher.start();

            String context = extractSurroundingContext(text, position, sentences);

            if (!context.isBlank()) {
                contexts.add(new CitationContext(citationMarker, context.trim(), sourceCitationKey));
            }
        }

        return contexts;
    }

    private String extractSurroundingContext(String text, int citationPosition, List<SentencePosition> sentences) {
        int sentenceIndex = -1;
        for (int i = 0; i < sentences.size(); i++) {
            SentencePosition sp = sentences.get(i);
            if (citationPosition >= sp.start() && citationPosition < sp.end()) {
                sentenceIndex = i;
                break;
            }
        }

        if (sentenceIndex == -1) {
            int start = Math.max(0, citationPosition - 200);
            int end = Math.min(text.length(), citationPosition + 200);
            return text.substring(start, end).trim();
        }

        int startSentence = Math.max(0, sentenceIndex - contextSentencesBefore);
        int endSentence = Math.min(sentences.size() - 1, sentenceIndex + contextSentencesAfter);

        int startPos = sentences.get(startSentence).start();
        int endPos = sentences.get(endSentence).end();

        return text.substring(startPos, endPos).trim();
    }

    private List<SentencePosition> splitIntoSentences(String text) {
        List<SentencePosition> sentences = new ArrayList<>();

        Pattern sentenceEnd = Pattern.compile(
                "(?<![A-Z][a-z]\\.)(?<![A-Z]\\.)(?<=\\.|\\?|!)\\s+(?=[A-Z\"])|$"
        );

        Matcher matcher = sentenceEnd.matcher(text);
        int lastEnd = 0;

        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                sentences.add(new SentencePosition(lastEnd, matcher.start()));
            }
            lastEnd = matcher.end();
        }

        if (lastEnd < text.length()) {
            sentences.add(new SentencePosition(lastEnd, text.length()));
        }

        return sentences;
    }

    private record SentencePosition(int start, int end) {
    }
}

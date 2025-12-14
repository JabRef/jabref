package org.jabref.logic.citation.contextextractor;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.util.strings.StringSimilarity;
import org.jabref.model.citation.ReferenceEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CitationMatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(CitationMatcher.class);

    private static final Pattern NUMERIC_MARKER_PATTERN = Pattern.compile(
            "^\\[?(\\d{1,3})\\]?$"
    );

    private static final Pattern AUTHOR_YEAR_PATTERN = Pattern.compile(
            "([A-Za-z'\\-]+)(?:\\s+(?:et\\s+al\\.?|and|&)\\s+[A-Za-z'\\-]+)*\\s*(\\d{4})[a-z]?"
    );

    private static final Pattern AUTHOR_KEY_PATTERN = Pattern.compile(
            "^\\[?([A-Z][a-zA-Z]+)(\\d{2,4})[a-z]?\\]?$"
    );

    private static final double AUTHOR_SIMILARITY_THRESHOLD = 0.7;

    private static final double TEXT_SIMILARITY_THRESHOLD = 0.6;

    private final StringSimilarity stringSimilarity;

    public CitationMatcher() {
        this.stringSimilarity = new StringSimilarity();
    }

    public Optional<ReferenceEntry> matchMarkerToReference(String citationMarker, List<ReferenceEntry> references) {
        if (citationMarker == null || citationMarker.isBlank() || references == null || references.isEmpty()) {
            return Optional.empty();
        }

        String normalizedMarker = normalizeMarker(citationMarker);
        LOGGER.debug("Matching marker '{}' (normalized: '{}') against {} references",
                citationMarker, normalizedMarker, references.size());

        Optional<ReferenceEntry> exactMatch = findExactMarkerMatch(normalizedMarker, references);
        if (exactMatch.isPresent()) {
            LOGGER.debug("Found exact marker match for '{}'", citationMarker);
            return exactMatch;
        }

        Optional<ReferenceEntry> numericMatch = matchNumericMarker(normalizedMarker, references);
        if (numericMatch.isPresent()) {
            LOGGER.debug("Found numeric match for '{}'", citationMarker);
            return numericMatch;
        }

        Optional<ReferenceEntry> authorYearMatch = matchAuthorYearMarker(normalizedMarker, references);
        if (authorYearMatch.isPresent()) {
            LOGGER.debug("Found author-year match for '{}'", citationMarker);
            return authorYearMatch;
        }

        Optional<ReferenceEntry> authorKeyMatch = matchAuthorKeyMarker(normalizedMarker, references);
        if (authorYearMatch.isPresent()) {
            LOGGER.debug("Found author-key match for '{}'", citationMarker);
            return authorKeyMatch;
        }

        Optional<ReferenceEntry> fuzzyMatch = findBestFuzzyMatch(normalizedMarker, references);
        if (fuzzyMatch.isPresent()) {
            LOGGER.debug("Found fuzzy match for '{}'", citationMarker);
            return fuzzyMatch;
        }

        LOGGER.debug("No match found for marker '{}'", citationMarker);
        return Optional.empty();
    }

    public List<ReferenceEntry> matchMultipleMarkers(String citationMarker, List<ReferenceEntry> references) {
        if (citationMarker == null || citationMarker.isBlank() || references == null || references.isEmpty()) {
            return List.of();
        }

        String cleaned = citationMarker.replaceAll("[\\[\\]()]", "").trim();

        if (cleaned.contains(",")) {
            return java.util.Arrays.stream(cleaned.split(","))
                                   .map(String::trim)
                                   .map(m -> matchMarkerToReference("[" + m + "]", references))
                                   .filter(Optional::isPresent)
                                   .map(Optional::get)
                                   .toList();
        }

        if (cleaned.contains("-")) {
            String[] parts = cleaned.split("-");
            if (parts.length == 2) {
                try {
                    int start = Integer.parseInt(parts[0].trim());
                    int end = Integer.parseInt(parts[1].trim());
                    return java.util.stream.IntStream.rangeClosed(start, end)
                                                     .mapToObj(i -> matchMarkerToReference("[" + i + "]", references))
                                                     .filter(Optional::isPresent)
                                                     .map(Optional::get)
                                                     .toList();
                } catch (NumberFormatException e) {
                    // Not a numeric range, fall through to single match
                }
            }
        }

        return matchMarkerToReference(citationMarker, references)
                .map(List::of)
                .orElse(List.of());
    }

    public double calculateMatchScore(String citationMarker, ReferenceEntry reference) {
        if (citationMarker == null || reference == null) {
            return 0.0;
        }

        String normalizedMarker = normalizeMarker(citationMarker);
        String referenceMarker = normalizeMarker(reference.marker());

        if (normalizedMarker.equalsIgnoreCase(referenceMarker)) {
            return 1.0;
        }

        double maxScore = 0.0;

        Matcher numericMatcher = NUMERIC_MARKER_PATTERN.matcher(normalizedMarker);
        if (numericMatcher.matches()) {
            Matcher refNumericMatcher = NUMERIC_MARKER_PATTERN.matcher(referenceMarker);
            if (refNumericMatcher.matches()) {
                if (numericMatcher.group(1).equals(refNumericMatcher.group(1))) {
                    return 1.0;
                }
            }
        }

        Matcher authorYearMatcher = AUTHOR_YEAR_PATTERN.matcher(normalizedMarker);
        if (authorYearMatcher.find()) {
            String markerAuthor = authorYearMatcher.group(1).toLowerCase(Locale.ROOT);
            String markerYear = authorYearMatcher.group(2);

            double authorYearScore = calculateAuthorYearScore(markerAuthor, markerYear, reference);
            maxScore = Math.max(maxScore, authorYearScore);
        }

        Matcher authorKeyMatcher = AUTHOR_KEY_PATTERN.matcher(normalizedMarker);
        if (authorKeyMatcher.matches()) {
            String markerAuthor = authorKeyMatcher.group(1).toLowerCase(Locale.ROOT);
            String markerYear = normalizeYear(authorKeyMatcher.group(2));

            double authorKeyScore = calculateAuthorYearScore(markerAuthor, markerYear, reference);
            maxScore = Math.max(maxScore, authorKeyScore);
        }

        double markerSimilarity = stringSimilarity.similarity(normalizedMarker, referenceMarker);
        maxScore = Math.max(maxScore, markerSimilarity * 0.8);

        return maxScore;
    }

    private String normalizeMarker(String marker) {
        return marker
                .replaceAll("[\\[\\](){}]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private Optional<ReferenceEntry> findExactMarkerMatch(String normalizedMarker, List<ReferenceEntry> references) {
        return references.stream()
                         .filter(ref -> normalizeMarker(ref.marker()).equalsIgnoreCase(normalizedMarker))
                         .findFirst();
    }

    private Optional<ReferenceEntry> matchNumericMarker(String normalizedMarker, List<ReferenceEntry> references) {
        Matcher matcher = NUMERIC_MARKER_PATTERN.matcher(normalizedMarker);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        int number = Integer.parseInt(matcher.group(1));

        if (number > 0 && number <= references.size()) {
            ReferenceEntry candidate = references.get(number - 1);
            String refMarker = normalizeMarker(candidate.marker());
            Matcher refMatcher = NUMERIC_MARKER_PATTERN.matcher(refMarker);
            if (refMatcher.matches() && Integer.parseInt(refMatcher.group(1)) == number) {
                return Optional.of(candidate);
            }
        }

        return references.stream()
                         .filter(ref -> {
                             String refMarker = normalizeMarker(ref.marker());
                             Matcher refMatcher = NUMERIC_MARKER_PATTERN.matcher(refMarker);
                             return refMatcher.matches() && Integer.parseInt(refMatcher.group(1)) == number;
                         })
                         .findFirst();
    }

    private Optional<ReferenceEntry> matchAuthorYearMarker(String normalizedMarker, List<ReferenceEntry> references) {
        Matcher matcher = AUTHOR_YEAR_PATTERN.matcher(normalizedMarker);
        if (!matcher.find()) {
            return Optional.empty();
        }

        String markerAuthor = matcher.group(1).toLowerCase(Locale.ROOT);
        String markerYear = matcher.group(2);

        return references.stream()
                         .filter(ref -> matchesAuthorAndYear(markerAuthor, markerYear, ref))
                         .max((r1, r2) -> Double.compare(
                                 calculateAuthorYearScore(markerAuthor, markerYear, r1),
                                 calculateAuthorYearScore(markerAuthor, markerYear, r2)
                         ));
    }

    private Optional<ReferenceEntry> matchAuthorKeyMarker(String normalizedMarker, List<ReferenceEntry> references) {
        Matcher matcher = AUTHOR_KEY_PATTERN.matcher(normalizedMarker);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        String markerAuthor = matcher.group(1).toLowerCase(Locale.ROOT);
        String markerYear = normalizeYear(matcher.group(2));

        return references.stream()
                         .filter(ref -> matchesAuthorAndYear(markerAuthor, markerYear, ref))
                         .max((r1, r2) -> Double.compare(
                                 calculateAuthorYearScore(markerAuthor, markerYear, r1),
                                 calculateAuthorYearScore(markerAuthor, markerYear, r2)
                         ));
    }

    private boolean matchesAuthorAndYear(String markerAuthor, String markerYear, ReferenceEntry reference) {
        if (reference.year().isPresent()) {
            String refYear = reference.year().get();
            if (!refYear.equals(markerYear) && !refYear.endsWith(markerYear)) {
                return false;
            }
        }

        if (reference.authors().isPresent()) {
            String refAuthors = reference.authors().get().toLowerCase(Locale.ROOT);
            String firstAuthor = extractFirstAuthorLastName(refAuthors);

            double similarity = stringSimilarity.similarity(markerAuthor, firstAuthor);
            if (similarity >= AUTHOR_SIMILARITY_THRESHOLD) {
                return true;
            }

            if (refAuthors.contains(markerAuthor)) {
                return true;
            }
        }

        String refMarkerNormalized = normalizeMarker(reference.marker()).toLowerCase(Locale.ROOT);
        return refMarkerNormalized.contains(markerAuthor);
    }

    private double calculateAuthorYearScore(String markerAuthor, String markerYear, ReferenceEntry reference) {
        double score = 0.0;

        if (reference.year().isPresent()) {
            String refYear = reference.year().get();
            if (refYear.equals(markerYear)) {
                score += 0.4;
            } else if (refYear.endsWith(markerYear.substring(Math.max(0, markerYear.length() - 2)))) {
                score += 0.2;
            }
        }

        if (reference.authors().isPresent()) {
            String refAuthors = reference.authors().get().toLowerCase(Locale.ROOT);
            String firstAuthor = extractFirstAuthorLastName(refAuthors);

            double authorSimilarity = stringSimilarity.similarity(markerAuthor, firstAuthor);
            score += authorSimilarity * 0.6;
        } else {
            String refMarker = normalizeMarker(reference.marker()).toLowerCase(Locale.ROOT);
            double markerSimilarity = stringSimilarity.similarity(markerAuthor, refMarker);
            score += markerSimilarity * 0.4;
        }

        return score;
    }

    private String extractFirstAuthorLastName(String authors) {
        String cleaned = authors
                .replaceAll("\\s+et\\s+al\\.?", "")
                .replaceAll("\\s+and\\s+.*", "")
                .replaceAll("\\s*&\\s*.*", "")
                .replaceAll(",.*", "")
                .trim();

        String[] parts = cleaned.split("\\s+");
        if (parts.length == 0) {
            return cleaned;
        }

        return parts[parts.length - 1].replaceAll("[^a-zA-Z]", "").toLowerCase(Locale.ROOT);
    }

    private String normalizeYear(String year) {
        if (year.length() == 2) {
            int yearNum = Integer.parseInt(year);
            if (yearNum > 50) {
                return "19" + year;
            } else {
                return "20" + year;
            }
        }
        return year;
    }

    private Optional<ReferenceEntry> findBestFuzzyMatch(String normalizedMarker, List<ReferenceEntry> references) {
        ReferenceEntry bestMatch = null;
        double bestScore = TEXT_SIMILARITY_THRESHOLD;

        for (ReferenceEntry reference : references) {
            double score = calculateMatchScore(normalizedMarker, reference);
            if (score > bestScore) {
                bestScore = score;
                bestMatch = reference;
            }
        }

        return Optional.ofNullable(bestMatch);
    }

    public record MatchResult(
            ReferenceEntry reference,
            double confidence,
            MatchType matchType
    ) {
        public boolean isHighConfidence() {
            return confidence >= 0.8;
        }

        public boolean isMediumConfidence() {
            return confidence >= 0.5 && confidence < 0.8;
        }

        public boolean isLowConfidence() {
            return confidence < 0.5;
        }
    }

    public enum MatchType {
        EXACT_MARKER,
        NUMERIC_INDEX,
        AUTHOR_YEAR,
        AUTHOR_KEY,
        FUZZY
    }

    public Optional<MatchResult> matchWithDetails(String citationMarker, List<ReferenceEntry> references) {
        if (citationMarker == null || citationMarker.isBlank() || references == null || references.isEmpty()) {
            return Optional.empty();
        }

        String normalizedMarker = normalizeMarker(citationMarker);

        Optional<ReferenceEntry> exactMatch = findExactMarkerMatch(normalizedMarker, references);
        if (exactMatch.isPresent()) {
            return Optional.of(new MatchResult(exactMatch.get(), 1.0, MatchType.EXACT_MARKER));
        }

        Optional<ReferenceEntry> numericMatch = matchNumericMarker(normalizedMarker, references);
        if (numericMatch.isPresent()) {
            return Optional.of(new MatchResult(numericMatch.get(), 0.95, MatchType.NUMERIC_INDEX));
        }

        Optional<ReferenceEntry> authorYearMatch = matchAuthorYearMarker(normalizedMarker, references);
        if (authorYearMatch.isPresent()) {
            double score = calculateMatchScore(normalizedMarker, authorYearMatch.get());
            return Optional.of(new MatchResult(authorYearMatch.get(), score, MatchType.AUTHOR_YEAR));
        }

        Optional<ReferenceEntry> authorKeyMatch = matchAuthorKeyMarker(normalizedMarker, references);
        if (authorKeyMatch.isPresent()) {
            double score = calculateMatchScore(normalizedMarker, authorKeyMatch.get());
            return Optional.of(new MatchResult(authorKeyMatch.get(), score, MatchType.AUTHOR_KEY));
        }

        Optional<ReferenceEntry> fuzzyMatch = findBestFuzzyMatch(normalizedMarker, references);
        if (fuzzyMatch.isPresent()) {
            double score = calculateMatchScore(normalizedMarker, fuzzyMatch.get());
            return Optional.of(new MatchResult(fuzzyMatch.get(), score, MatchType.FUZZY));
        }

        return Optional.empty();
    }
}

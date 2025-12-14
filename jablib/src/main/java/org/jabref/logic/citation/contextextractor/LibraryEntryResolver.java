package org.jabref.logic.citation.contextextractor;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.database.DuplicateCheck;
import org.jabref.logic.importer.AuthorListParser;
import org.jabref.logic.util.strings.StringSimilarity;
import org.jabref.model.citation.ReferenceEntry;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LibraryEntryResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(LibraryEntryResolver.class);

    private static final double DOI_MATCH_CONFIDENCE = 1.0;
    private static final double EXACT_CITATION_KEY_CONFIDENCE = 0.95;
    private static final double HIGH_TITLE_SIMILARITY_CONFIDENCE = 0.85;
    private static final double MEDIUM_TITLE_SIMILARITY_CONFIDENCE = 0.70;
    private static final double AUTHOR_YEAR_TITLE_CONFIDENCE = 0.75;
    private static final double DUPLICATE_CHECK_CONFIDENCE = 0.80;

    private static final double TITLE_SIMILARITY_THRESHOLD = 0.85;
    private static final double AUTHOR_SIMILARITY_THRESHOLD = 0.75;

    private final BibDatabase database;
    private final BibDatabaseMode databaseMode;
    private final DuplicateCheck duplicateCheck;
    private final StringSimilarity stringSimilarity;
    private final AuthorListParser authorListParser;

    public LibraryEntryResolver(BibDatabase database, BibDatabaseMode databaseMode, BibEntryTypesManager entryTypesManager) {
        this.database = Objects.requireNonNull(database, "Database cannot be null");
        this.databaseMode = Objects.requireNonNull(databaseMode, "Database mode cannot be null");
        this.duplicateCheck = new DuplicateCheck(Objects.requireNonNull(entryTypesManager, "Entry types manager cannot be null"));
        this.stringSimilarity = new StringSimilarity();
        this.authorListParser = new AuthorListParser();
    }

    public ResolvedEntry resolveReference(ReferenceEntry reference) {
        Objects.requireNonNull(reference, "Reference cannot be null");

        LOGGER.debug("Resolving reference: {}", reference.marker());

        Optional<MatchedEntry> existingEntry = findExistingEntry(reference);
        if (existingEntry.isPresent()) {
            LOGGER.debug("Found existing entry for reference '{}' with confidence {}",
                    reference.marker(), existingEntry.get().confidence());
            return new ResolvedEntry(
                    existingEntry.get().entry(),
                    false,
                    existingEntry.get().confidence(),
                    existingEntry.get().matchType()
            );
        }

        LOGGER.debug("No existing entry found, creating new entry for reference '{}'", reference.marker());
        BibEntry newEntry = createEntryFromReference(reference);
        return new ResolvedEntry(newEntry, true, 1.0, MatchType.NEW_ENTRY);
    }

    public Optional<MatchedEntry> findExistingEntry(ReferenceEntry reference) {
        Optional<MatchedEntry> doiMatch = findByDoi(reference);
        if (doiMatch.isPresent()) {
            return doiMatch;
        }

        Optional<MatchedEntry> citationKeyMatch = findByCitationKey(reference);
        if (citationKeyMatch.isPresent()) {
            return citationKeyMatch;
        }

        Optional<MatchedEntry> titleMatch = findByTitle(reference);
        if (titleMatch.isPresent()) {
            return titleMatch;
        }

        Optional<MatchedEntry> authorYearMatch = findByAuthorAndYear(reference);
        if (authorYearMatch.isPresent()) {
            return authorYearMatch;
        }

        Optional<MatchedEntry> duplicateMatch = findByDuplicateCheck(reference);
        if (duplicateMatch.isPresent()) {
            return duplicateMatch;
        }

        return Optional.empty();
    }

    private Optional<MatchedEntry> findByDoi(ReferenceEntry reference) {
        if (reference.doi().isEmpty()) {
            return Optional.empty();
        }

        String referenceDoi = reference.doi().get();
        Optional<DOI> parsedDoi = DOI.parse(referenceDoi);

        for (BibEntry entry : database.getEntries()) {
            Optional<DOI> entryDoi = entry.getDOI();
            if (entryDoi.isPresent()) {
                if (parsedDoi.isPresent() && parsedDoi.get().equals(entryDoi.get())) {
                    return Optional.of(new MatchedEntry(entry, DOI_MATCH_CONFIDENCE, MatchType.DOI));
                }
                String entryDoiStr = entryDoi.get().asString();
                if (referenceDoi.equalsIgnoreCase(entryDoiStr) ||
                        referenceDoi.contains(entryDoiStr) ||
                        entryDoiStr.contains(referenceDoi)) {
                    return Optional.of(new MatchedEntry(entry, DOI_MATCH_CONFIDENCE, MatchType.DOI));
                }
            }
        }

        return Optional.empty();
    }

    private Optional<MatchedEntry> findByCitationKey(ReferenceEntry reference) {
        Optional<String> generatedKey = reference.generateCitationKey();
        if (generatedKey.isPresent()) {
            Optional<BibEntry> entry = database.getEntryByCitationKey(generatedKey.get());
            if (entry.isPresent()) {
                return Optional.of(new MatchedEntry(entry.get(), EXACT_CITATION_KEY_CONFIDENCE, MatchType.CITATION_KEY));
            }
        }

        String normalizedMarker = reference.getNormalizedMarker();
        if (!normalizedMarker.isEmpty() && !normalizedMarker.matches("\\d+")) {
            Optional<BibEntry> entry = database.getEntryByCitationKey(normalizedMarker);
            if (entry.isPresent()) {
                return Optional.of(new MatchedEntry(entry.get(), EXACT_CITATION_KEY_CONFIDENCE, MatchType.CITATION_KEY));
            }

            String lowerMarker = normalizedMarker.toLowerCase(Locale.ROOT);
            for (BibEntry entry2 : database.getEntries()) {
                Optional<String> entryKey = entry2.getCitationKey();
                if (entryKey.isPresent() && entryKey.get().toLowerCase(Locale.ROOT).equals(lowerMarker)) {
                    return Optional.of(new MatchedEntry(entry2, EXACT_CITATION_KEY_CONFIDENCE * 0.95, MatchType.CITATION_KEY));
                }
            }
        }

        return Optional.empty();
    }

    private Optional<MatchedEntry> findByTitle(ReferenceEntry reference) {
        if (reference.title().isEmpty()) {
            return Optional.empty();
        }

        String referenceTitle = reference.title().get().toLowerCase(Locale.ROOT);
        BibEntry bestMatch = null;
        double bestSimilarity = 0;

        for (BibEntry entry : database.getEntries()) {
            Optional<String> entryTitle = entry.getTitle();
            if (entryTitle.isEmpty()) {
                continue;
            }

            String entryTitleLower = entryTitle.get().toLowerCase(Locale.ROOT);
            double similarity = stringSimilarity.similarity(referenceTitle, entryTitleLower);

            if (similarity > bestSimilarity && similarity >= TITLE_SIMILARITY_THRESHOLD) {
                bestSimilarity = similarity;
                bestMatch = entry;
            }
        }

        if (bestMatch != null) {
            double confidence = bestSimilarity >= 0.95 ? HIGH_TITLE_SIMILARITY_CONFIDENCE :
                                bestSimilarity >= 0.85 ? MEDIUM_TITLE_SIMILARITY_CONFIDENCE :
                                bestSimilarity * 0.8;
            return Optional.of(new MatchedEntry(bestMatch, confidence, MatchType.TITLE));
        }

        return Optional.empty();
    }

    private Optional<MatchedEntry> findByAuthorAndYear(ReferenceEntry reference) {
        if (reference.authors().isEmpty() || reference.year().isEmpty()) {
            return Optional.empty();
        }

        String referenceAuthor = extractFirstAuthorLastName(reference.authors().get()).toLowerCase(Locale.ROOT);
        String referenceYear = reference.year().get();

        BibEntry bestMatch = null;
        double bestScore = 0;

        for (BibEntry entry : database.getEntries()) {
            double score = calculateAuthorYearMatchScore(referenceAuthor, referenceYear, reference.title(), entry);
            if (score > bestScore && score >= 0.6) {
                bestScore = score;
                bestMatch = entry;
            }
        }

        if (bestMatch != null) {
            return Optional.of(new MatchedEntry(bestMatch, bestScore * AUTHOR_YEAR_TITLE_CONFIDENCE, MatchType.AUTHOR_YEAR));
        }

        return Optional.empty();
    }

    private double calculateAuthorYearMatchScore(String referenceAuthor, String referenceYear,
                                                 Optional<String> referenceTitle, BibEntry entry) {
        double score = 0;
        int matchedFields = 0;

        Optional<String> entryYear = entry.getField(StandardField.YEAR);
        if (entryYear.isPresent() && entryYear.get().equals(referenceYear)) {
            score += 0.3;
            matchedFields++;
        }

        Optional<String> entryAuthor = entry.getField(StandardField.AUTHOR);
        if (entryAuthor.isPresent()) {
            String entryFirstAuthor = extractFirstAuthorLastName(entryAuthor.get()).toLowerCase(Locale.ROOT);
            double authorSimilarity = stringSimilarity.similarity(referenceAuthor, entryFirstAuthor);
            if (authorSimilarity >= AUTHOR_SIMILARITY_THRESHOLD) {
                score += 0.4 * authorSimilarity;
                matchedFields++;
            }
        }

        if (referenceTitle.isPresent()) {
            Optional<String> entryTitle = entry.getTitle();
            if (entryTitle.isPresent()) {
                double titleSimilarity = stringSimilarity.similarity(
                        referenceTitle.get().toLowerCase(Locale.ROOT),
                        entryTitle.get().toLowerCase(Locale.ROOT)
                );
                if (titleSimilarity >= 0.5) {
                    score += 0.3 * titleSimilarity;
                    matchedFields++;
                }
            }
        }

        return matchedFields >= 2 ? score : 0;
    }

    private Optional<MatchedEntry> findByDuplicateCheck(ReferenceEntry reference) {
        if (!reference.hasMinimalMetadata()) {
            return Optional.empty();
        }

        BibEntry referenceEntry = reference.toBibEntry();
        Optional<BibEntry> duplicate = duplicateCheck.containsDuplicate(database, referenceEntry, databaseMode);

        return duplicate.map(entry -> new MatchedEntry(entry, DUPLICATE_CHECK_CONFIDENCE, MatchType.DUPLICATE_CHECK));
    }

    public BibEntry createEntryFromReference(ReferenceEntry reference) {
        return reference.toBibEntry();
    }

    public boolean entryExists(ReferenceEntry reference) {
        return findExistingEntry(reference).isPresent();
    }

    public boolean addEntryIfNotExists(BibEntry entry) {
        Optional<String> citationKey = entry.getCitationKey();
        if (citationKey.isPresent() && database.getEntryByCitationKey(citationKey.get()).isPresent()) {
            LOGGER.debug("Entry with citation key '{}' already exists", citationKey.get());
            return false;
        }

        Optional<DOI> doi = entry.getDOI();
        if (doi.isPresent()) {
            for (BibEntry existing : database.getEntries()) {
                if (existing.getDOI().equals(doi)) {
                    LOGGER.debug("Entry with DOI '{}' already exists", doi.get());
                    return false;
                }
            }
        }

        if (duplicateCheck.containsDuplicate(database, entry, databaseMode).isPresent()) {
            LOGGER.debug("Duplicate entry found");
            return false;
        }

        database.insertEntry(entry);
        LOGGER.debug("Added new entry: {}", entry.getCitationKey().orElse("(no key)"));
        return true;
    }

    public ResolvedEntry resolveAndAdd(ReferenceEntry reference) {
        ResolvedEntry resolved = resolveReference(reference);

        if (resolved.isNew()) {
            database.insertEntry(resolved.entry());
            LOGGER.info("Added new entry from reference: {}", resolved.entry().getCitationKey().orElse("(no key)"));
        }

        return resolved;
    }

    private String extractFirstAuthorLastName(String authors) {
        if (authors == null || authors.isBlank()) {
            return "";
        }

        try {
            AuthorList authorList = authorListParser.parse(authors);
            if (!authorList.isEmpty()) {
                Author firstAuthor = authorList.getAuthor(0);
                return firstAuthor.getFamilyName().orElse("");
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to parse author list '{}', falling back to simple extraction", authors);
        }

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

    public Optional<BibEntry> findEntryByMarker(String citationMarker) {
        if (citationMarker == null || citationMarker.isBlank()) {
            return Optional.empty();
        }

        String normalizedMarker = citationMarker
                .replaceAll("[\\[\\](){}]", "")
                .trim();

        java.util.regex.Pattern authorYearPattern = java.util.regex.Pattern.compile(
                "([A-Za-z'\\-]+)(?:\\s+(?:et\\s+al\\.?|and|&)\\s+[A-Za-z'\\-]+)*\\s*(\\d{4})[a-z]?"
        );
        java.util.regex.Matcher matcher = authorYearPattern.matcher(normalizedMarker);

        if (matcher.find()) {
            String markerAuthor = matcher.group(1).toLowerCase(Locale.ROOT);
            String markerYear = matcher.group(2);

            BibEntry bestMatch = null;
            double bestScore = 0;

            for (BibEntry entry : database.getEntries()) {
                Optional<String> entryAuthor = entry.getField(StandardField.AUTHOR);
                Optional<String> entryYear = entry.getField(StandardField.YEAR);

                if (entryYear.isEmpty() || !entryYear.get().equals(markerYear)) {
                    continue;
                }

                if (entryAuthor.isPresent()) {
                    String firstAuthor = extractFirstAuthorLastName(entryAuthor.get()).toLowerCase(Locale.ROOT);
                    double similarity = stringSimilarity.similarity(markerAuthor, firstAuthor);

                    if (similarity > bestScore && similarity >= 0.7) {
                        bestScore = similarity;
                        bestMatch = entry;
                    }
                }
            }

            if (bestMatch != null) {
                LOGGER.debug("Found library match for marker '{}': {} (score: {})",
                        citationMarker, bestMatch.getCitationKey().orElse("unknown"), bestScore);
                return Optional.of(bestMatch);
            }
        }

        Optional<BibEntry> keyMatch = database.getEntryByCitationKey(normalizedMarker);
        if (keyMatch.isPresent()) {
            return keyMatch;
        }

        String lowerMarker = normalizedMarker.toLowerCase(Locale.ROOT);
        for (BibEntry entry : database.getEntries()) {
            Optional<String> entryKey = entry.getCitationKey();
            if (entryKey.isPresent() && entryKey.get().toLowerCase(Locale.ROOT).equals(lowerMarker)) {
                return Optional.of(entry);
            }
        }

        return Optional.empty();
    }

    public record ResolvedEntry(
            BibEntry entry,
            boolean newEntry,
            double confidence,
            MatchType matchType
    ) {
        public boolean isNew() {
            return newEntry;
        }

        public boolean isExisting() {
            return !newEntry;
        }

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

    record MatchedEntry(
            BibEntry entry,
            double confidence,
            MatchType matchType
    ) {
    }

    public enum MatchType {
        DOI,
        CITATION_KEY,
        TITLE,
        AUTHOR_YEAR,
        DUPLICATE_CHECK,

        NEW_ENTRY
    }
}

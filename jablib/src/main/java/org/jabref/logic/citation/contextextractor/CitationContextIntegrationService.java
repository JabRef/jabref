package org.jabref.logic.citation.contextextractor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.importer.fileformat.pdf.PdfSectionExtractor;
import org.jabref.model.citation.CitationContext;
import org.jabref.model.citation.CitationContextList;
import org.jabref.model.citation.ReferenceEntry;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.pdf.PdfDocumentSections;
import org.jabref.model.pdf.PdfSection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CitationContextIntegrationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CitationContextIntegrationService.class);

    private final PdfSectionExtractor sectionExtractor;
    private final CitationContextExtractor contextExtractor;
    private final PdfReferenceParser referenceParser;
    private final CitationMatcher citationMatcher;
    private final LibraryEntryResolver entryResolver;

    public CitationContextIntegrationService(
            BibDatabase database,
            BibDatabaseMode databaseMode,
            BibEntryTypesManager entryTypesManager,
            String username
    ) {
        this.sectionExtractor = new PdfSectionExtractor();
        this.contextExtractor = new CitationContextExtractor();
        this.referenceParser = new PdfReferenceParser();
        this.citationMatcher = new CitationMatcher();
        this.entryResolver = new LibraryEntryResolver(database, databaseMode, entryTypesManager);
    }

    public List<MatchedContext> previewDocument(Path pdfPath, String sourceCitationKey) throws IOException {
        Objects.requireNonNull(pdfPath, "PDF path cannot be null");
        Objects.requireNonNull(sourceCitationKey, "Source citation key cannot be null");

        List<MatchedContext> results = new ArrayList<>();

        PdfDocumentSections sections = sectionExtractor.extractSections(pdfPath);
        LOGGER.info("Extracted {} sections from PDF: {}", sections.sections().size(),
                sections.sections().stream().map(PdfSection::name).toList());

        List<ReferenceEntry> references = extractReferences(sections);
        LOGGER.info("Extracted {} references from PDF", references.size());

        CitationContextList contexts = extractCitationContexts(sections, sourceCitationKey);
        LOGGER.info("Extracted {} citation contexts from PDF", contexts.size());

        for (CitationContext context : contexts.getContexts()) {
            Optional<ReferenceEntry> matchedRef = citationMatcher.matchMarkerToReference(
                    context.citationMarker(), references);

            if (matchedRef.isPresent()) {
                LibraryEntryResolver.ResolvedEntry resolved = entryResolver.resolveReference(matchedRef.get());
                results.add(new MatchedContext(
                        context,
                        resolved.entry(),
                        resolved.isNew()
                ));
            } else {
                Optional<BibEntry> directMatch = tryDirectLibraryMatch(context.citationMarker());
                if (directMatch.isPresent()) {
                    LOGGER.debug("Found direct library match for marker '{}'", context.citationMarker());
                    results.add(new MatchedContext(
                            context,
                            directMatch.get(),
                            false
                    ));
                } else {
                    results.add(new MatchedContext(context, null, false));
                }
            }
        }

        return results;
    }

    private Optional<BibEntry> tryDirectLibraryMatch(String citationMarker) {
        if (citationMarker == null || citationMarker.isBlank()) {
            return Optional.empty();
        }

        return entryResolver.findEntryByMarker(citationMarker);
    }

    private List<ReferenceEntry> extractReferences(PdfDocumentSections sections) {
        Optional<PdfSection> referencesSection = sections.getReferencesSection();

        if (referencesSection.isEmpty()) {
            LOGGER.warn("No references section found in PDF. Available sections: {}",
                    sections.sections().stream().map(PdfSection::name).toList());

            String fullText = sections.fullText();
            int refIndex = fullText.toLowerCase().lastIndexOf("references");
            if (refIndex == -1) {
                refIndex = fullText.toLowerCase().lastIndexOf("bibliography");
            }

            if (refIndex >= 0) {
                LOGGER.info("Found 'References' or 'Bibliography' keyword at position {} in full text, attempting extraction", refIndex);
                String referencesText = fullText.substring(refIndex);
                List<ReferenceEntry> references = referenceParser.parseReferences(referencesText);
                LOGGER.info("Extracted {} references from full text fallback", references.size());
                return references;
            }

            return List.of();
        }

        String referencesText = referencesSection.get().content();
        LOGGER.debug("References section '{}' has {} characters", referencesSection.get().name(), referencesText.length());
        List<ReferenceEntry> references = referenceParser.parseReferences(referencesText);
        LOGGER.debug("Parsed {} references from section", references.size());

        return references;
    }

    private CitationContextList extractCitationContexts(PdfDocumentSections sections, String sourceCitationKey) {
        CitationContextList result = new CitationContextList(sourceCitationKey);

        List<PdfSection> relevantSections = sections.getCitationRelevantSections();

        if (!relevantSections.isEmpty()) {
            LOGGER.debug("Found {} citation-relevant sections", relevantSections.size());
            for (PdfSection section : relevantSections) {
                CitationContextList sectionContexts = contextExtractor.extractContexts(
                        section.content(), sourceCitationKey);
                result.addAll(sectionContexts.getContexts());
            }
        }

        if (result.isEmpty()) {
            LOGGER.debug("No citation-relevant sections found, extracting from full text");
            CitationContextList fullTextContexts = contextExtractor.extractContexts(
                    sections.fullText(), sourceCitationKey);
            result.addAll(fullTextContexts.getContexts());
        }

        return result;
    }

    public record MatchedContext(
            CitationContext context,
            BibEntry libraryEntry,
            boolean isNewEntry
    ) {
        public boolean isMatched() {
            return libraryEntry != null;
        }
    }
}

package org.jabref.logic.citation.contextextractor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.ai.AiPreferences;
import org.jabref.logic.ai.AiService;
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

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CitationContextIntegrationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CitationContextIntegrationService.class);

    private final PdfSectionExtractor sectionExtractor;
    private final CitationContextExtractor regexContextExtractor;
    @Nullable
    private final LlmCitationContextExtractor llmContextExtractor;
    private final PdfReferenceParser referenceParser;
    private final CitationMatcher citationMatcher;
    private final LibraryEntryResolver entryResolver;

    public CitationContextIntegrationService(
            BibDatabase database,
            BibDatabaseMode databaseMode,
            BibEntryTypesManager entryTypesManager,
            String username,
            AiService aiService,
            AiPreferences aiPreferences
    ) {
        this.sectionExtractor = new PdfSectionExtractor();
        this.regexContextExtractor = new CitationContextExtractor();
        this.referenceParser = new PdfReferenceParser();
        this.citationMatcher = new CitationMatcher();
        this.entryResolver = new LibraryEntryResolver(database, databaseMode, entryTypesManager);

        if (aiService != null && aiPreferences != null && aiPreferences.getEnableAi()) {
            this.llmContextExtractor = new LlmCitationContextExtractor(
                    aiService.getTemplatesService(),
                    aiService.getChatLanguageModel()
            );
        } else {
            this.llmContextExtractor = null;
        }
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
        String textToAnalyze;

        if (!relevantSections.isEmpty()) {
            LOGGER.debug("Found {} citation-relevant sections", relevantSections.size());
            StringBuilder sb = new StringBuilder();
            for (PdfSection section : relevantSections) {
                sb.append(section.content()).append("\n\n");
            }
            textToAnalyze = sb.toString();
        } else {
            LOGGER.debug("No citation-relevant sections found, using full text");
            textToAnalyze = sections.fullText();
        }

        if (llmContextExtractor != null) {
            LOGGER.info("Using LLM-based citation context extraction");
            try {
                CitationContextList llmContexts = llmContextExtractor.extractContexts(textToAnalyze, sourceCitationKey);
                result.addAll(llmContexts.getContexts());
                LOGGER.info("LLM extracted {} citation contexts", llmContexts.size());

                if (llmContexts.isEmpty()) {
                    LOGGER.info("LLM returned no results, falling back to regex extraction");
                    extractWithRegex(textToAnalyze, sourceCitationKey, result);
                }
            } catch (Exception e) {
                LOGGER.warn("LLM extraction failed, falling back to regex", e);
                extractWithRegex(textToAnalyze, sourceCitationKey, result);
            }
        } else {
            LOGGER.info("Using regex-based citation context extraction (AI not enabled)");
            extractWithRegex(textToAnalyze, sourceCitationKey, result);
        }

        return result;
    }

    private void extractWithRegex(String text, String sourceCitationKey, CitationContextList result) {
        CitationContextList regexContexts = regexContextExtractor.extractContexts(text, sourceCitationKey);
        result.addAll(regexContexts.getContexts());
    }

    public record MatchedContext(
            CitationContext context,
            @Nullable BibEntry libraryEntry,
            boolean isNewEntry
    ) {
        public boolean isMatched() {
            return libraryEntry != null;
        }
    }
}

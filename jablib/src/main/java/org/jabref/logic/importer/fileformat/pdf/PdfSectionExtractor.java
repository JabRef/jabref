package org.jabref.logic.importer.fileformat.pdf;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import org.jabref.logic.pdf.InterruptablePDFTextStripper;
import org.jabref.logic.xmp.XmpUtilReader;
import org.jabref.model.pdf.PdfDocumentSections;
import org.jabref.model.pdf.PdfSection;

import org.apache.pdfbox.pdmodel.PDDocument;

public class PdfSectionExtractor {

    private static final List<String> KNOWN_SECTIONS = List.of(
            "abstract",
            "introduction",
            "related work",
            "related works",
            "literature review",
            "background",
            "previous work",
            "prior work",
            "state of the art",
            "methodology",
            "methods",
            "materials and methods",
            "approach",
            "proposed method",
            "proposed approach",
            "system design",
            "system overview",
            "implementation",
            "experiments",
            "experimental setup",
            "experimental results",
            "results",
            "results and discussion",
            "evaluation",
            "discussion",
            "analysis",
            "conclusion",
            "conclusions",
            "concluding remarks",
            "summary",
            "future work",
            "limitations",
            "limitations and future work",
            "acknowledgments",
            "acknowledgements",
            "references",
            "bibliography",
            "appendix",
            "supplementary material"
    );

    private static final Pattern NUMBERED_HEADING_PATTERN = Pattern.compile(
            "^\\s*([0-9]+\\.?(?:\\s*[0-9]+)*\\.?)\\s+([A-Z][A-Za-z\\s]+)$"
    );

    private static final Pattern ROMAN_HEADING_PATTERN = Pattern.compile(
            "^\\s*([IVXLC]+)\\.?\\s+([A-Z][A-Za-z\\s]+)$"
    );

    private final ReadOnlyBooleanProperty shutdownSignal;

    public PdfSectionExtractor() {
        this(new SimpleBooleanProperty(false));
    }

    public PdfSectionExtractor(ReadOnlyBooleanProperty shutdownSignal) {
        this.shutdownSignal = shutdownSignal;
    }

    public PdfDocumentSections extractSections(Path pdfPath) throws IOException {
        try (PDDocument document = new XmpUtilReader().loadWithAutomaticDecryption(pdfPath)) {
            return extractSections(document);
        }
    }

    public PdfDocumentSections extractSections(PDDocument document) throws IOException {
        int pageCount = document.getNumberOfPages();
        String fullText = extractFullText(document);

        if (shutdownSignal.get()) {
            return new PdfDocumentSections("", List.of(), pageCount);
        }

        List<PdfSection> sections = identifySections(fullText, document);

        return new PdfDocumentSections(fullText, sections, pageCount);
    }

    private String extractFullText(PDDocument document) throws IOException {
        StringWriter writer = new StringWriter();
        InterruptablePDFTextStripper stripper = new InterruptablePDFTextStripper(shutdownSignal);
        stripper.setStartPage(1);
        stripper.setEndPage(document.getNumberOfPages());
        stripper.writeText(document, writer);
        return writer.toString();
    }

    private List<PdfSection> identifySections(String fullText, PDDocument document) {
        List<PdfSection> sections = new ArrayList<>();
        List<SectionMarker> markers = findSectionMarkers(fullText);

        for (int i = 0; i < markers.size(); i++) {
            if (shutdownSignal.get()) {
                break;
            }

            SectionMarker current = markers.get(i);
            int contentStart = current.endPosition();
            int contentEnd = (i + 1 < markers.size())
                             ? markers.get(i + 1).startPosition()
                             : fullText.length();

            String content = fullText.substring(contentStart, contentEnd).trim();

            int startPage = estimatePage(current.startPosition(), fullText.length(), document.getNumberOfPages());
            int endPage = estimatePage(contentEnd, fullText.length(), document.getNumberOfPages());

            sections.add(new PdfSection(current.name(), content, startPage, endPage));
        }

        return sections;
    }

    private List<SectionMarker> findSectionMarkers(String text) {
        List<SectionMarker> markers = new ArrayList<>();
        String[] lines = text.split("\\r?\\n");
        int position = 0;

        for (String line : lines) {
            String trimmedLine = line.trim();
            Optional<String> sectionName = identifySectionHeading(trimmedLine);

            if (sectionName.isPresent()) {
                markers.add(new SectionMarker(
                        sectionName.get(),
                        position,
                        position + line.length()
                ));
            }

            position += line.length() + 1;
        }

        return markers;
    }

    private Optional<String> identifySectionHeading(String line) {
        if (line.isEmpty() || line.length() > 80) {
            return Optional.empty();
        }

        String trimmedLine = line.trim();

        if (looksLikeSentence(trimmedLine)) {
            return Optional.empty();
        }

        java.util.regex.Matcher numberedMatcher = NUMBERED_HEADING_PATTERN.matcher(trimmedLine);
        if (numberedMatcher.matches()) {
            String headingText = numberedMatcher.group(2).trim();
            if (isKnownSection(headingText)) {
                return Optional.of(normalizeHeading(headingText));
            }
        }

        java.util.regex.Matcher romanMatcher = ROMAN_HEADING_PATTERN.matcher(trimmedLine);
        if (romanMatcher.matches()) {
            String headingText = romanMatcher.group(2).trim();
            if (isKnownSection(headingText)) {
                return Optional.of(normalizeHeading(headingText));
            }
        }

        String cleanedLine = trimmedLine.replaceAll("^[0-9]+\\.?(?:\\s*[0-9]+)*\\.?\\s*", "")
                                        .replaceAll("^[IVXLC]+\\.?\\s*", "")
                                        .trim();

        if (isKnownSection(cleanedLine)) {
            return Optional.of(normalizeHeading(cleanedLine));
        }

        if (looksLikeAllCapsHeading(cleanedLine)) {
            return Optional.of(normalizeHeading(cleanedLine));
        }

        return Optional.empty();
    }

    private boolean isKnownSection(String text) {
        String lowerText = text.toLowerCase().trim();
        for (String knownSection : KNOWN_SECTIONS) {
            if (lowerText.equals(knownSection)) {
                return true;
            }
        }
        return false;
    }

    private boolean looksLikeSentence(String text) {
        if (text.endsWith(".") || text.endsWith(",") || text.endsWith(";") ||
                text.endsWith(":") || text.endsWith(")")) {
            return true;
        }

        String[] words = text.split("\\s+");
        if (words.length > 6) {
            return true;
        }

        String lowerText = text.toLowerCase();
        if (lowerText.contains(" the ") || lowerText.contains(" a ") ||
                lowerText.contains(" an ") || lowerText.contains(" is ") ||
                lowerText.contains(" are ") || lowerText.contains(" was ") ||
                lowerText.contains(" were ") || lowerText.contains(" have ") ||
                lowerText.contains(" has ") || lowerText.contains(" this ") ||
                lowerText.contains(" that ") || lowerText.contains(" these ") ||
                lowerText.contains(" we ") || lowerText.contains(" in ") ||
                lowerText.contains(" on ") || lowerText.contains(" for ") ||
                lowerText.contains(" to ") || lowerText.contains(" of ")) {
            String cleanedText = text.replaceAll("^[0-9IVXLC]+\\.?\\s*", "").trim();
            if (!isKnownSection(cleanedText)) {
                return true;
            }
        }

        return false;
    }

    private boolean looksLikeAllCapsHeading(String text) {
        if (!text.equals(text.toUpperCase())) {
            return false;
        }

        if (!text.matches("^[A-Z][A-Z\\s]+$")) {
            return false;
        }

        if (text.length() < 3 || text.length() > 35) {
            return false;
        }

        String[] words = text.split("\\s+");
        if (words.length > 4) {
            return false;
        }

        return true;
    }

    private String normalizeHeading(String heading) {
        if (heading.equals(heading.toUpperCase())) {
            return toTitleCase(heading);
        }
        return heading;
    }

    private String toTitleCase(String text) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : text.toLowerCase().toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    private int estimatePage(int position, int totalLength, int totalPages) {
        if (totalLength == 0 || totalPages == 0) {
            return 1;
        }
        double ratio = (double) position / totalLength;
        int page = (int) Math.ceil(ratio * totalPages);
        return Math.max(1, Math.min(page, totalPages));
    }

    private record SectionMarker(String name, int startPosition, int endPosition) {
    }
}

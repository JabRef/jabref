package org.jabref.logic.pdf.search.indexing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jabref.gui.LibraryTab;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.strings.StringUtil;
import org.jabref.preferences.FilePreferences;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.model.pdf.search.SearchFieldConstants.ANNOTATIONS;
import static org.jabref.model.pdf.search.SearchFieldConstants.CONTENT;
import static org.jabref.model.pdf.search.SearchFieldConstants.MODIFIED;
import static org.jabref.model.pdf.search.SearchFieldConstants.PAGE_NUMBER;
import static org.jabref.model.pdf.search.SearchFieldConstants.PATH;

/**
 * Utility class for reading the data from LinkedFiles of a BibEntry for Lucene.
 */
public final class DocumentReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(LibraryTab.class);

    private static final Pattern HYPHEN_LINEBREAK_PATTERN = Pattern.compile("\\-\n");
    private static final Pattern LINEBREAK_WITHOUT_PERIOD_PATTERN = Pattern.compile("([^\\\\.])\\n");

    private final BibEntry entry;
    private final FilePreferences filePreferences;

    /**
     * Creates a new DocumentReader using a BibEntry.
     *
     * @param bibEntry Must not be null and must have at least one LinkedFile.
     */
    public DocumentReader(BibEntry bibEntry, FilePreferences filePreferences) {
        this.filePreferences = filePreferences;
        if (bibEntry.getFiles().isEmpty()) {
            throw new IllegalStateException("There are no linked PDF files to this BibEntry!");
        }

        this.entry = bibEntry;
    }

    /**
     * Reads a LinkedFile of a BibEntry and converts it into a Lucene Document which is then returned.
     *
     * @return An Optional of a Lucene Document with the (meta)data. Can be empty if there is a problem reading the LinkedFile.
     */
    public Optional<List<Document>> readLinkedPdf(BibDatabaseContext databaseContext, LinkedFile pdf) {
        Optional<Path> pdfPath = pdf.findIn(databaseContext, filePreferences);
        if (pdfPath.isPresent()) {
            try {
                return Optional.of(readPdfContents(pdf, pdfPath.get()));
            } catch (IOException e) {
                LOGGER.error("Could not read pdf file {}!", pdf.getLink(), e);
            }
        }
        return Optional.empty();
    }

    /**
     * Reads each LinkedFile of a BibEntry and converts them into Lucene Documents which are then returned.
     *
     * @return A List of Documents with the (meta)data. Can be empty if there is a problem reading the LinkedFile.
     */
    public List<Document> readLinkedPdfs(BibDatabaseContext databaseContext) {
        return entry.getFiles().stream()
                    .map((pdf) -> readLinkedPdf(databaseContext, pdf))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
    }

    private List<Document> readPdfContents(LinkedFile pdf, Path resolvedPdfPath) throws IOException {
        try (PDDocument pdfDocument = PDDocument.load(resolvedPdfPath.toFile())) {
            List<Document> pages = new ArrayList<>();

            for (int pageNumber = 0; pageNumber < pdfDocument.getNumberOfPages(); pageNumber++) {
                Document newDocument = new Document();
                addIdentifiers(newDocument, pdf.getLink());
                addMetaData(newDocument, resolvedPdfPath, pageNumber);
                addContentIfNotEmpty(pdfDocument, newDocument, pageNumber);
                pages.add(newDocument);
            }
            return pages;
        }
    }

    private void addMetaData(Document newDocument, Path resolvedPdfPath, int pageNumber) {
        try {
            BasicFileAttributes attributes = Files.readAttributes(resolvedPdfPath, BasicFileAttributes.class);
            addStringField(newDocument, MODIFIED, String.valueOf(attributes.lastModifiedTime().to(TimeUnit.SECONDS)));
        } catch (IOException e) {
            LOGGER.error("Could not read timestamp for {}", resolvedPdfPath, e);
        }
        addStringField(newDocument, PAGE_NUMBER, String.valueOf(pageNumber));
    }

    private void addStringField(Document newDocument, String field, String value) {
        if (!isValidField(value)) {
            return;
        }
        newDocument.add(new StringField(field, value, Field.Store.YES));
    }

    private boolean isValidField(String value) {
        return !(StringUtil.isNullOrEmpty(value));
    }

    public static String mergeLines(String text) {
        String mergedHyphenNewlines = HYPHEN_LINEBREAK_PATTERN.matcher(text).replaceAll("");
        return LINEBREAK_WITHOUT_PERIOD_PATTERN.matcher(mergedHyphenNewlines).replaceAll("$1 ");
    }

    private void addContentIfNotEmpty(PDDocument pdfDocument, Document newDocument, int pageNumber) {
        try {
            PDFTextStripper pdfTextStripper = new PDFTextStripper();
            pdfTextStripper.setLineSeparator("\n");
            pdfTextStripper.setStartPage(pageNumber);
            pdfTextStripper.setEndPage(pageNumber);

            String pdfContent = pdfTextStripper.getText(pdfDocument);
            if (StringUtil.isNotBlank(pdfContent)) {
                newDocument.add(new TextField(CONTENT, mergeLines(pdfContent), Field.Store.YES));
            }
            PDPage page = pdfDocument.getPage(pageNumber);
            List<String> annotations = page.getAnnotations().stream().filter((annotation) -> annotation.getContents() != null).map(PDAnnotation::getContents).collect(Collectors.toList());
            if (annotations.size() > 0) {
                newDocument.add(new TextField(ANNOTATIONS, annotations.stream().collect(Collectors.joining("\n")), Field.Store.YES));
            }
        } catch (IOException e) {
            LOGGER.info("Could not read contents of PDF document \"{}\"", pdfDocument.toString(), e);
        }
    }

    private void addIdentifiers(Document newDocument, String path) {
        newDocument.add(new StringField(PATH, path, Field.Store.YES));
    }
}

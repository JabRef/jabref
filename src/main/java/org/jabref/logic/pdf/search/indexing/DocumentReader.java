package org.jabref.logic.pdf.search.indexing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
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
import static org.jabref.model.pdf.search.SearchFieldConstants.PATH;

/**
 * Utility class for reading the data from LinkedFiles of a BibEntry for Lucene.
 */
public final class DocumentReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(LibraryTab.class);

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
    public Optional<Document> readLinkedPdf(BibDatabaseContext databaseContext, LinkedFile pdf) {
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
                    .collect(Collectors.toList());
    }

    private Document readPdfContents(LinkedFile pdf, Path resolvedPdfPath) throws IOException {
        try (PDDocument pdfDocument = PDDocument.load(resolvedPdfPath.toFile())) {
            Document newDocument = new Document();
            addIdentifiers(newDocument, pdf.getLink());
            addContentIfNotEmpty(pdfDocument, newDocument);
            addMetaData(newDocument, resolvedPdfPath);
            return newDocument;
        }
    }

    private void addMetaData(Document newDocument, Path resolvedPdfPath) {
        try {
            BasicFileAttributes attributes = Files.readAttributes(resolvedPdfPath, BasicFileAttributes.class);
            addStringField(newDocument, MODIFIED, String.valueOf(attributes.lastModifiedTime().to(TimeUnit.SECONDS)));
        } catch (IOException e) {
            LOGGER.error("Could not read timestamp for {}", resolvedPdfPath, e);
        }
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

    private void addContentIfNotEmpty(PDDocument pdfDocument, Document newDocument) {
        try {
            PDFTextStripper pdfTextStripper = new PDFTextStripper();
            pdfTextStripper.setLineSeparator("\n");

            String pdfContent = pdfTextStripper.getText(pdfDocument);
            if (StringUtil.isNotBlank(pdfContent)) {
                newDocument.add(new TextField(CONTENT, pdfContent, Field.Store.YES));
            }
            for (PDPage page : pdfDocument.getPages()) {
                for (PDAnnotation annotation : page.getAnnotations(annotation -> {
                    if (annotation.getContents() == null) {
                        return false;
                    }
                    return annotation.getSubtype().equals("Text") || annotation.getSubtype().equals("Highlight");
                })) {
                    newDocument.add(new TextField(ANNOTATIONS, annotation.getContents(), Field.Store.YES));
                }
            }
        } catch (IOException e) {
            LOGGER.info("Could not read contents of PDF document \"{}\"", pdfDocument.toString(), e);
        }
    }

    private void addIdentifiers(Document newDocument, String path) {
        newDocument.add(new StringField(PATH, path, Field.Store.YES));
    }
}

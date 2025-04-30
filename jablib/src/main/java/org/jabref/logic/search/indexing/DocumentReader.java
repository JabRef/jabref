package org.jabref.logic.search.indexing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.jabref.model.strings.StringUtil;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.model.search.LinkedFilesConstants.ANNOTATIONS;
import static org.jabref.model.search.LinkedFilesConstants.CONTENT;
import static org.jabref.model.search.LinkedFilesConstants.MODIFIED;
import static org.jabref.model.search.LinkedFilesConstants.PAGE_NUMBER;
import static org.jabref.model.search.LinkedFilesConstants.PATH;

/**
 * Utility class for reading the data from LinkedFiles of a BibEntry for Lucene.
 */
public final class DocumentReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentReader.class);
    private static final Pattern HYPHEN_LINEBREAK_PATTERN = Pattern.compile("\\-\n");
    private static final Pattern LINEBREAK_WITHOUT_PERIOD_PATTERN = Pattern.compile("([^\\\\.])\\n");

    public List<Document> readPdfContents(String fileLink, Path resolvedPdfPath) {
        List<Document> pages = new ArrayList<>();
        try (PDDocument pdfDocument = Loader.loadPDF(resolvedPdfPath.toFile())) {
            int numberOfPages = pdfDocument.getNumberOfPages();
            LOGGER.debug("Reading file {} content with {} pages", resolvedPdfPath.toAbsolutePath(), numberOfPages);
            for (int pageNumber = 1; pageNumber <= numberOfPages; pageNumber++) {
                Document newDocument = new Document();
                addIdentifiers(newDocument, fileLink);
                addMetaData(newDocument, resolvedPdfPath, pageNumber);
                addContentIfNotEmpty(pdfDocument, newDocument, resolvedPdfPath, pageNumber);

                pages.add(newDocument);
            }
        } catch (IOException e) {
            LOGGER.warn("Could not read {}", resolvedPdfPath.toAbsolutePath(), e);
            return pages;
        }
        if (pages.isEmpty()) {
            Document newDocument = new Document();
            addIdentifiers(newDocument, fileLink);
            addMetaData(newDocument, resolvedPdfPath, 1);
            pages.add(newDocument);
        }
        return pages;
    }

    private void addStringField(Document newDocument, String field, String value) {
        if (!isValidField(value)) {
            return;
        }
        newDocument.add(new StringField(field, value, Field.Store.YES));
    }

    private boolean isValidField(String value) {
        return !StringUtil.isNullOrEmpty(value);
    }

    public static String mergeLines(String text) {
        String mergedHyphenNewlines = HYPHEN_LINEBREAK_PATTERN.matcher(text).replaceAll("");
        return LINEBREAK_WITHOUT_PERIOD_PATTERN.matcher(mergedHyphenNewlines).replaceAll("$1 ");
    }

    private void addMetaData(Document newDocument, Path resolvedPdfPath, int pageNumber) {
        try {
            long modifiedTime = Files.getLastModifiedTime(resolvedPdfPath).to(TimeUnit.SECONDS);
            addStringField(newDocument, MODIFIED.toString(), String.valueOf(modifiedTime));
        } catch (IOException e) {
            LOGGER.error("Could not read timestamp for {}", resolvedPdfPath, e);
        }
        addStringField(newDocument, PAGE_NUMBER.toString(), String.valueOf(pageNumber));
    }

    private void addContentIfNotEmpty(PDDocument pdfDocument, Document newDocument, Path resolvedPath, int pageNumber) {
        PDFTextStripper pdfTextStripper = new PDFTextStripper();
        pdfTextStripper.setLineSeparator("\n");
        pdfTextStripper.setStartPage(pageNumber);
        pdfTextStripper.setEndPage(pageNumber);

        try {
            String pdfContent = pdfTextStripper.getText(pdfDocument);
            if (StringUtil.isNotBlank(pdfContent)) {
                newDocument.add(new TextField(CONTENT.toString(), mergeLines(pdfContent), Field.Store.YES));
            }

            // Apache PDFTextStripper is 1-based. See {@link org.apache.pdfbox.text.PDFTextStripper.processPages}
            PDPage page = pdfDocument.getPage(pageNumber - 1);
            List<String> annotations = page.getAnnotations()
                                           .stream()
                                           .map(PDAnnotation::getContents)
                                           .filter(Objects::nonNull)
                                           .toList();

            if (!annotations.isEmpty()) {
                newDocument.add(new TextField(ANNOTATIONS.toString(), String.join("\n", annotations), Field.Store.YES));
            }
        } catch (IOException e) {
            LOGGER.warn("Could not read page {} of  {}", pageNumber, resolvedPath.toAbsolutePath(), e);
        }
    }

    private void addIdentifiers(Document newDocument, String path) {
        newDocument.add(new StringField(PATH.toString(), path, Field.Store.YES));
    }
}

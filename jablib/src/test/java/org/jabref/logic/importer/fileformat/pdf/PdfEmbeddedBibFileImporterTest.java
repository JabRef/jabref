package org.jabref.logic.importer.fileformat.pdf;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import javafx.collections.FXCollections;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.apache.pdfbox.cos.COSInputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.apache.pdfbox.pdmodel.common.filespecification.PDSimpleFileSpecification;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationFileAttachment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PdfEmbeddedBibFileImporterTest {

    private PdfEmbeddedBibFileImporter importer;

    @BeforeEach
    void setUp() {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.fieldPreferences().getNonWrappableFields()).thenReturn(FXCollections.emptyObservableList());
        importer = new PdfEmbeddedBibFileImporter(importFormatPreferences);
    }

    @Test
    void doesNotHandleEncryptedPdfs() throws URISyntaxException {
        Path file = Path.of(PdfEmbeddedBibFileImporter.class.getResource("/pdfs/encrypted.pdf").toURI());
        List<BibEntry> result = importer.importDatabase(file).getDatabase().getEntries();
        assertEquals(List.of(), result);
    }

    @Test
    void importWorksAsExpected() throws URISyntaxException {
        Path file = Path.of(PdfEmbeddedBibFileImporterTest.class.getResource("mixedMetadata.pdf").toURI());
        List<BibEntry> result = importer.importDatabase(file).getDatabase().getEntries();

        BibEntry expected = new BibEntry(StandardEntryType.Misc)
                .withCitationKey("jabreftext2021")
                .withField(StandardField.AUTHOR, "Someone embedded")
                .withField(StandardField.TITLE, "I like beds")
                .withField(StandardField.DOI, "10.1002/9781118257517")
                .withField(StandardField.COMMENT, "From embedded bib");

        assertEquals(List.of(expected), result);
    }

    @Test
    void skipsAttachmentsWithoutEmbeddedFiles() throws IOException, ParseException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            page.getAnnotations().add(new PDAnnotationFileAttachment());
            PDAnnotationFileAttachment attachment = new PDAnnotationFileAttachment();
            attachment.setFile(new PDComplexFileSpecification());
            page.getAnnotations().add(attachment);

            assertEquals(List.of(), importer.importDatabase(Path.of("test.pdf"), document).getDatabase().getEntries());
        }
    }

    @Test
    void prefersUnicodeEmbeddedFile() throws IOException, ParseException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            PDComplexFileSpecification fileSpec = new PDComplexFileSpecification();
            fileSpec.setEmbeddedFileUnicode(new PDEmbeddedFile(document, new ByteArrayInputStream("@misc{unicode}".getBytes(StandardCharsets.UTF_8))));
            fileSpec.setEmbeddedFile(new PDEmbeddedFile(document, new ByteArrayInputStream("@misc{fallback}".getBytes(StandardCharsets.UTF_8))));
            PDAnnotationFileAttachment attachment = new PDAnnotationFileAttachment();
            attachment.setFile(fileSpec);
            page.getAnnotations().add(attachment);

            assertEquals(List.of(new BibEntry(StandardEntryType.Misc).withCitationKey("unicode")),
                    importer.importDatabase(Path.of("test.pdf"), document).getDatabase().getEntries());
        }
    }

    @Test
    void importsFilesInNestedNameTree() throws IOException, ParseException {
        try (PDDocument document = new PDDocument()) {
            PDComplexFileSpecification fileSpec = new PDComplexFileSpecification();
            fileSpec.setEmbeddedFile(new PDEmbeddedFile(document, new ByteArrayInputStream("@misc{nested}".getBytes(StandardCharsets.UTF_8))));
            PDEmbeddedFilesNameTreeNode leaf = new PDEmbeddedFilesNameTreeNode();
            leaf.setNames(Map.of("nested.bib", fileSpec));
            PDEmbeddedFilesNameTreeNode intermediate = new PDEmbeddedFilesNameTreeNode();
            intermediate.setKids(List.of(leaf));
            PDEmbeddedFilesNameTreeNode root = new PDEmbeddedFilesNameTreeNode();
            root.setKids(List.of(intermediate));
            PDDocumentNameDictionary names = new PDDocumentNameDictionary(document.getDocumentCatalog());
            names.setEmbeddedFiles(root);
            document.getDocumentCatalog().setNames(names);

            assertEquals(List.of(new BibEntry(StandardEntryType.Misc).withCitationKey("nested")),
                    importer.importDatabase(Path.of("test.pdf"), document).getDatabase().getEntries());
        }
    }

    @Test
    void skipsSimpleFileSpecifications() throws IOException, ParseException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            PDSimpleFileSpecification fileSpec = new PDSimpleFileSpecification();
            fileSpec.setFile("external.bib");
            PDAnnotationFileAttachment attachment = new PDAnnotationFileAttachment();
            attachment.setFile(fileSpec);
            page.getAnnotations().add(attachment);

            assertEquals(List.of(), importer.importDatabase(Path.of("test.pdf"), document).getDatabase().getEntries());
        }
    }

    @Test
    void closesEmbeddedFileStream() throws IOException, ParseException {
        try (PDDocument owner = new PDDocument()) {
            PDEmbeddedFile realFile = new PDEmbeddedFile(owner, new ByteArrayInputStream("@misc{stream}".getBytes(StandardCharsets.UTF_8)));
            COSInputStream stream = spy(realFile.createInputStream());
            PDEmbeddedFile embeddedFile = mock(PDEmbeddedFile.class);
            when(embeddedFile.createInputStream()).thenReturn(stream);
            PDComplexFileSpecification fileSpec = mock(PDComplexFileSpecification.class);
            when(fileSpec.getEmbeddedFileUnicode()).thenReturn(embeddedFile);
            PDAnnotationFileAttachment attachment = mock(PDAnnotationFileAttachment.class);
            when(attachment.getFile()).thenReturn(fileSpec);
            PDPage page = mock(PDPage.class);
            when(page.getAnnotations()).thenReturn(List.of(attachment));
            PDPageTree pages = mock(PDPageTree.class);
            when(pages.iterator()).thenReturn(List.of(page).iterator());
            PDDocument document = mock(PDDocument.class);
            when(document.getDocumentCatalog()).thenReturn(mock(PDDocumentCatalog.class));
            when(document.getPages()).thenReturn(pages);

            assertEquals(List.of(new BibEntry(StandardEntryType.Misc).withCitationKey("stream")),
                    importer.importDatabase(Path.of("test.pdf"), document).getDatabase().getEntries());
            verify(stream).close();
        }
    }
}

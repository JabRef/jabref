package org.jabref.logic.importer.fileformat.pdf;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.entry.BibEntry;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDNameTreeNode;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationFileAttachment;
import org.jspecify.annotations.NullMarked;

/// Imports an embedded Bib-File from the PDF.
@NullMarked
public class PdfEmbeddedBibFileImporter extends PdfImporter {

    private final BibtexParser bibtexParser;

    public PdfEmbeddedBibFileImporter(ImportFormatPreferences importFormatPreferences) {
        bibtexParser = new BibtexParser(importFormatPreferences);
    }

    /// Extraction of embedded files in pdfs adapted from:
    /// <a href="https://svn.apache.org/repos/asf/pdfbox/trunk/examples/src/main/java/org/apache/pdfbox/examples/pdmodel/ExtractEmbeddedFiles.javaj">...</a>
    public ParserResult importDatabase(Path filePath, PDDocument document) throws IOException, ParseException {
        List<BibEntry> allParsedEntries = new ArrayList<>();
        PDDocumentNameDictionary nameDictionary = document.getDocumentCatalog().getNames();
        if (nameDictionary != null) {
            PDEmbeddedFilesNameTreeNode efTree = nameDictionary.getEmbeddedFiles();
            if (efTree != null) {
                allParsedEntries.addAll(extractAndParseNameTree(efTree));
            }
        }
        // extract files from annotations
        for (PDPage page : document.getPages()) {
            for (PDAnnotation annotation : page.getAnnotations()) {
                if (annotation instanceof PDAnnotationFileAttachment annotationFileAttachment) {
                    allParsedEntries.addAll(extractAndParseAttachment(annotationFileAttachment));
                }
            }
        }
        return new ParserResult(allParsedEntries);
    }

    /// External file references and attachments without an embedded file do not contain importable entries.
    private List<BibEntry> extractAndParseAttachment(PDAnnotationFileAttachment attachment) throws IOException, ParseException {
        if (!(attachment.getFile() instanceof PDComplexFileSpecification fileSpec)) {
            return List.of();
        }
        Optional<PDEmbeddedFile> embeddedFile = searchEmbeddedFile(fileSpec);
        if (embeddedFile.isEmpty()) {
            return List.of();
        }
        return extractAndParseFile(embeddedFile.orElseThrow());
    }

    /// Name-tree nodes can contain names, child nodes, or neither; visit all descendants to find embedded files.
    private List<BibEntry> extractAndParseNameTree(PDNameTreeNode<PDComplexFileSpecification> node) throws IOException, ParseException {
        List<BibEntry> entries = new ArrayList<>();
        Map<String, PDComplexFileSpecification> names = node.getNames();
        if (names != null) {
            entries.addAll(extractAndParseFiles(names));
        }
        List<PDNameTreeNode<PDComplexFileSpecification>> kids = node.getKids();
        if (kids != null) {
            for (PDNameTreeNode<PDComplexFileSpecification> child : kids) {
                entries.addAll(extractAndParseNameTree(child));
            }
        }
        return entries;
    }

    private List<BibEntry> extractAndParseFiles(Map<String, PDComplexFileSpecification> names) throws IOException, ParseException {
        List<BibEntry> allParsedEntries = new ArrayList<>();
        for (Map.Entry<String, PDComplexFileSpecification> entry : names.entrySet()) {
            String filename = entry.getKey();
            if (FileUtil.isBibFile(Path.of(filename))) {
                Optional<PDEmbeddedFile> embeddedFile = searchEmbeddedFile(entry.getValue());
                if (embeddedFile.isPresent()) {
                    allParsedEntries.addAll(extractAndParseFile(embeddedFile.orElseThrow()));
                }
            }
        }
        return allParsedEntries;
    }

    private List<BibEntry> extractAndParseFile(PDEmbeddedFile embeddedFile) throws IOException, ParseException {
        try (InputStream inputStream = embeddedFile.createInputStream()) {
            return bibtexParser.parseEntries(inputStream);
        }
    }

    /// Prefer the Unicode variant, followed by platform-specific variants and the generic embedded file.
    /// Later variants are only read when earlier ones are absent.
    private static Optional<PDEmbeddedFile> searchEmbeddedFile(PDComplexFileSpecification fileSpec) {
        return Optional.ofNullable(fileSpec.getEmbeddedFileUnicode())
                       .or(() -> Optional.ofNullable(fileSpec.getEmbeddedFileDos()))
                       .or(() -> Optional.ofNullable(fileSpec.getEmbeddedFileMac()))
                       .or(() -> Optional.ofNullable(fileSpec.getEmbeddedFileUnix()))
                       .or(() -> Optional.ofNullable(fileSpec.getEmbeddedFile()));
    }

    @Override
    public String getId() {
        return "pdfEmbeddedBibFile";
    }

    @Override
    public String getName() {
        return Localization.lang("Embedded BIB-file in PDF");
    }

    @Override
    public String getDescription() {
        return Localization.lang("Imports a BibTeX file found inside a PDF.");
    }
}

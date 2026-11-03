package org.jabref.logic.importer.fileformat.pdf;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
                Map<String, PDComplexFileSpecification> names = efTree.getNames();
                if (names != null) {
                    allParsedEntries.addAll(extractAndParseFiles(names));
                } else {
                    List<PDNameTreeNode<PDComplexFileSpecification>> kids = efTree.getKids();
                    if (kids != null) {
                        for (PDNameTreeNode<PDComplexFileSpecification> node : kids) {
                            names = node.getNames();
                            allParsedEntries.addAll(extractAndParseFiles(names));
                        }
                    }
                }
            }
        }
        // extract files from annotations
        for (PDPage page : document.getPages()) {
            for (PDAnnotation annotation : page.getAnnotations()) {
                if (annotation instanceof PDAnnotationFileAttachment annotationFileAttachment) {
                    Optional<PDEmbeddedFile> embeddedFile = Optional.ofNullable((PDComplexFileSpecification) annotationFileAttachment.getFile())
                                                                    .flatMap(PdfEmbeddedBibFileImporter::searchEmbeddedFile);

                    // Using traditional `if` statement because of exceptions.
                    if (embeddedFile.isPresent()) {
                        allParsedEntries.addAll(extractAndParseFile(embeddedFile.get()));
                    }
                }
            }
        }
        return new ParserResult(allParsedEntries);
    }

    private List<BibEntry> extractAndParseFiles(Map<String, PDComplexFileSpecification> names) throws IOException, ParseException {
        List<BibEntry> allParsedEntries = new ArrayList<>();
        for (Map.Entry<String, PDComplexFileSpecification> entry : names.entrySet()) {
            String filename = entry.getKey();
            FileUtil.getFileExtension(filename);
            if (FileUtil.isBibFile(Path.of(filename))) {
                Optional<PDEmbeddedFile> embeddedFile = searchEmbeddedFile(entry.getValue());

                // Using traditional `if` statement because of exceptions.
                if (embeddedFile.isPresent()) {
                    allParsedEntries.addAll(extractAndParseFile(embeddedFile.get()));
                }
            }
        }
        return allParsedEntries;
    }

    private List<BibEntry> extractAndParseFile(PDEmbeddedFile embeddedFile) throws IOException, ParseException {
        return bibtexParser.parseEntries(embeddedFile.createInputStream());
    }

    private static Optional<PDEmbeddedFile> searchEmbeddedFile(PDComplexFileSpecification fileSpec) {
        List<PDEmbeddedFile> places = Arrays.asList(
                fileSpec.getEmbeddedFileUnicode(),
                fileSpec.getEmbeddedFileDos(),
                fileSpec.getEmbeddedFileMac(),
                fileSpec.getEmbeddedFileUnix(),
                fileSpec.getEmbeddedFile()
        );

        // `fileSpec.getEmbedded*()` may return `null`, regardless of what IntelliJ might say here.
        return places.stream().filter(Objects::nonNull).findFirst();
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

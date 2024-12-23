package org.jabref.logic.importer.fileformat.pdf;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
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

/**
 * Imports an embedded Bib-File from the PDF.
 */
public class PdfEmbeddedBibFileImporter extends PdfImporter {

    private final BibtexParser bibtexParser;

    public PdfEmbeddedBibFileImporter(ImportFormatPreferences importFormatPreferences) {
        bibtexParser = new BibtexParser(importFormatPreferences);
    }

    /**
     * Extraction of embedded files in pdfs adapted from:
     * <a href="https://svn.apache.org/repos/asf/pdfbox/trunk/examples/src/main/java/org/apache/pdfbox/examples/pdmodel/ExtractEmbeddedFiles.javaj">...</a>
     */
    public List<BibEntry> importDatabase(Path filePath, PDDocument document) throws IOException, ParseException {
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
                    PDComplexFileSpecification fileSpec = (PDComplexFileSpecification) annotationFileAttachment.getFile();
                    allParsedEntries.addAll(extractAndParseFile(getEmbeddedFile(fileSpec)));
                }
            }
        }
        return allParsedEntries;
    }

    private List<BibEntry> extractAndParseFiles(Map<String, PDComplexFileSpecification> names) throws IOException, ParseException {
        List<BibEntry> allParsedEntries = new ArrayList<>();
        for (Map.Entry<String, PDComplexFileSpecification> entry : names.entrySet()) {
            String filename = entry.getKey();
            FileUtil.getFileExtension(filename);
            if (FileUtil.isBibFile(Path.of(filename))) {
                PDComplexFileSpecification fileSpec = entry.getValue();
                allParsedEntries.addAll(extractAndParseFile(getEmbeddedFile(fileSpec)));
            }
        }
        return allParsedEntries;
    }

    private List<BibEntry> extractAndParseFile(PDEmbeddedFile embeddedFile) throws IOException, ParseException {
        return bibtexParser.parseEntries(embeddedFile.createInputStream());
    }

    private static PDEmbeddedFile getEmbeddedFile(PDComplexFileSpecification fileSpec) {
        // search for the first available alternative of the embedded file
        PDEmbeddedFile embeddedFile = null;
        if (fileSpec != null) {
            embeddedFile = fileSpec.getEmbeddedFileUnicode();
            if (embeddedFile == null) {
                embeddedFile = fileSpec.getEmbeddedFileDos();
            }
            if (embeddedFile == null) {
                embeddedFile = fileSpec.getEmbeddedFileMac();
            }
            if (embeddedFile == null) {
                embeddedFile = fileSpec.getEmbeddedFileUnix();
            }
            if (embeddedFile == null) {
                embeddedFile = fileSpec.getEmbeddedFile();
            }
        }
        return embeddedFile;
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

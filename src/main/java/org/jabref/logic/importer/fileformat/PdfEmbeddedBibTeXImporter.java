package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jabref.gui.externalfiles.FileFilterUtils;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.logic.xmp.EncryptedPdfsNotSupportedException;
import org.jabref.logic.xmp.XmpUtilReader;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDNameTreeNode;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationFileAttachment;
import org.apache.pdfbox.text.PDFTextStripper;

/**
 * PdfContentImporter imports a verbatim BibTeX entry from the first page of the PDF.
 */
public class PdfEmbeddedBibTeXImporter extends Importer {

    private final ImportFormatPreferences importFormatPreferences;
    private final BibtexParser bibtexParser;

    public PdfEmbeddedBibTeXImporter(ImportFormatPreferences importFormatPreferences) {
        this.importFormatPreferences = importFormatPreferences;
        bibtexParser = new BibtexParser(importFormatPreferences, new DummyFileUpdateMonitor());
    }

    @Override
    public boolean isRecognizedFormat(BufferedReader input) throws IOException {
        return input.readLine().startsWith("%PDF");
    }

    @Override
    public ParserResult importDatabase(BufferedReader reader) throws IOException {
        Objects.requireNonNull(reader);
        throw new UnsupportedOperationException("PdfEmbeddedBibTeXImporter does not support importDatabase(BufferedReader reader)."
                + "Instead use importDatabase(Path filePath, Charset defaultEncoding).");
    }

    @Override
    public ParserResult importDatabase(String data) throws IOException {
        Objects.requireNonNull(data);
        throw new UnsupportedOperationException("PdfEmbeddedBibTeXImporter does not support importDatabase(String data)."
                + "Instead use importDatabase(Path filePath, Charset defaultEncoding).");
    }

    @Override
    public ParserResult importDatabase(Path filePath, Charset defaultEncoding) {
        try (PDDocument document = XmpUtilReader.loadWithAutomaticDecryption(filePath)) {
            return new ParserResult(getEmbeddedBibTeXEntries(document));
        } catch (EncryptedPdfsNotSupportedException e) {
            return ParserResult.fromErrorMessage(Localization.lang("Decryption not supported."));
        } catch (IOException | ParseException e) {
            return ParserResult.fromError(e);
        }
    }

    /**
     * Extraction of embedded files in pdfs adapted from:
     * Adapted from https://svn.apache.org/repos/asf/pdfbox/trunk/examples/src/main/java/org/apache/pdfbox/examples/pdmodel/ExtractEmbeddedFiles.javaj
     */

    private List<BibEntry> getEmbeddedBibTeXEntries(PDDocument document) throws IOException, ParseException {
        List<BibEntry> allParsedEntries = new ArrayList<>();
        PDDocumentNameDictionary nameDictionary = document.getDocumentCatalog().getNames();
        PDEmbeddedFilesNameTreeNode efTree = nameDictionary.getEmbeddedFiles();
        if (efTree != null) {
            Map<String, PDComplexFileSpecification> names = efTree.getNames();
            System.out.println("Found files: " + names.keySet().toString());
            if (names != null) {
                allParsedEntries.addAll(extractAndParseFiles(names));
            } else {
                List<PDNameTreeNode<PDComplexFileSpecification>> kids = efTree.getKids();
                for (PDNameTreeNode<PDComplexFileSpecification> node : kids) {
                    names = node.getNames();
                    allParsedEntries.addAll(extractAndParseFiles(names));
                }
            }
        }
        // extract files from annotations
        for (PDPage page : document.getPages()) {
            for (PDAnnotation annotation : page.getAnnotations()) {
                if (annotation instanceof PDAnnotationFileAttachment) {
                    System.out.println("Found embedded file: " + annotation.getContents());
                    PDAnnotationFileAttachment annotationFileAttachment = (PDAnnotationFileAttachment) annotation;
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
        System.out.println(new String(embeddedFile.toByteArray(), StandardCharsets.UTF_8));
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
    public String getName() {
        return "PDFembeddedbibtex";
    }

    @Override
    public StandardFileType getFileType() {
        return StandardFileType.PDF;
    }

    @Override
    public String getDescription() {
        return "PdfEmbeddedBibTeXImporter imports an embedded BibTeX entry from the PDF.";
    }

}

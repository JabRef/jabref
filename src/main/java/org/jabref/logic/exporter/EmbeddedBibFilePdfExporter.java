package org.jabref.logic.exporter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jabref.logic.bibtex.BibEntryWriter;
import org.jabref.logic.bibtex.FieldWriter;
import org.jabref.logic.bibtex.FieldWriterPreferences;
import org.jabref.logic.util.OS;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;

/**
 * A custom exporter to write bib entries to an embedded bib file.
 */
public class EmbeddedBibFilePdfExporter extends Exporter {

    public static String EMBEDDED_FILE_NAME = "main.bib";

    private final BibDatabaseMode bibDatabaseMode;
    private final BibEntryTypesManager bibEntryTypesManager;
    private final FieldWriterPreferences fieldWriterPreferences;

    public EmbeddedBibFilePdfExporter(BibDatabaseMode bibDatabaseMode, BibEntryTypesManager bibEntryTypesManager, FieldWriterPreferences fieldWriterPreferences) {
        super("bib", "Embedded BibTeX", StandardFileType.PDF);
        this.bibDatabaseMode = bibDatabaseMode;
        this.bibEntryTypesManager = bibEntryTypesManager;
        this.fieldWriterPreferences = fieldWriterPreferences;
    }

    /**
     * @param databaseContext the database to export from
     * @param file            the file to write to. If it contains "split", then the output is split into different files
     * @param encoding        the encoding to use
     * @param entries         a list containing all entries that should be exported
     */
    @Override
    public void export(BibDatabaseContext databaseContext, Path file, Charset encoding, List<BibEntry> entries) throws Exception {
        Objects.requireNonNull(databaseContext);
        Objects.requireNonNull(file);
        Objects.requireNonNull(entries);

        String bibString = getBibString(entries);
        embedBibTex(bibString, file, encoding);
    }

    private void embedBibTex(String bibTeX, Path file, Charset encoding) throws IOException {
        if (!Files.exists(file) || !StandardFileType.PDF.getExtensions().contains(FileUtil.getFileExtension(file).orElse(""))) {
            return;
        }
        try (PDDocument document = PDDocument.load(file.toFile())) {
            PDDocumentNameDictionary nameDictionary = document.getDocumentCatalog().getNames();
            PDEmbeddedFilesNameTreeNode efTree;
            Map<String, PDComplexFileSpecification> names;

            if (nameDictionary == null) {
                efTree = new PDEmbeddedFilesNameTreeNode();
                names = new HashMap<>();
                nameDictionary = new PDDocumentNameDictionary(document.getDocumentCatalog());
                nameDictionary.setEmbeddedFiles(efTree);
                document.getDocumentCatalog().setNames(nameDictionary);
            } else {
                efTree = nameDictionary.getEmbeddedFiles();
                names = efTree.getNames();
            }

            if (efTree != null) {
                PDComplexFileSpecification fileSpecification = new PDComplexFileSpecification();
                fileSpecification.setFile(EMBEDDED_FILE_NAME);
                InputStream inputStream = new ByteArrayInputStream(bibTeX.getBytes(encoding));
                PDEmbeddedFile embeddedFile = new PDEmbeddedFile(document, inputStream);
                embeddedFile.setSubtype("text/x-bibtex");
                embeddedFile.setSize(bibTeX.length());
                fileSpecification.setEmbeddedFile(embeddedFile);

                names.put(EMBEDDED_FILE_NAME, fileSpecification);
                efTree.setNames(names);
                nameDictionary.setEmbeddedFiles(efTree);
                document.getDocumentCatalog().setNames(nameDictionary);
            }
            document.save(file.toFile());
        }
    }

    private String getBibString(List<BibEntry> entries) throws IOException {
        StringWriter stringWriter = new StringWriter();
        BibWriter bibWriter = new BibWriter(stringWriter, OS.NEWLINE);
        FieldWriter fieldWriter = FieldWriter.buildIgnoreHashes(fieldWriterPreferences);
        BibEntryWriter bibEntryWriter = new BibEntryWriter(fieldWriter, bibEntryTypesManager);
        for (BibEntry entry : entries) {
            bibEntryWriter.write(entry, bibWriter, bibDatabaseMode);
        }
        return stringWriter.toString();
    }
}

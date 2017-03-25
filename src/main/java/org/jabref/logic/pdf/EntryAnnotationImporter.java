package org.jabref.logic.pdf;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.FileField;
import org.jabref.model.entry.ParsedFileField;
import org.jabref.model.pdf.FileAnnotation;
import org.jabref.preferences.JabRefPreferences;


/**
 * Here all PDF files attached to a BibEntry are scanned for annotations using a PdfAnnotationImporter.
 */
public class EntryAnnotationImporter {

    private final BibEntry entry;

    /**
     * @param entry The BibEntry whose attached files are scanned for annotations.
     */
    public EntryAnnotationImporter(BibEntry entry) {
        this.entry = entry;
    }

    /**
     * Filter files with a web address containing "www."
     *
     * @return a list of file parsed files
     */
    public List<ParsedFileField> getFilteredFileList() {
        return FileField.parse(this.entry.getField(FieldName.FILE).get()).stream()
                .filter(parsedFileField -> parsedFileField.getLink().toLowerCase(Locale.ROOT).endsWith(".pdf"))
                .filter(parsedFileField -> !parsedFileField.getLink().contains("www.")).collect(Collectors.toList());
    }

    /**
     * Reads the annotations from the files that are attached to a BibEntry.
     *
     * @param databaseContext The context is needed for the importer.
     * @return Map from each PDF to a list of file annotations
     */
    public Map<String, List<FileAnnotation>> importAnnotationsFromFiles(BibDatabaseContext databaseContext) {
        Map<String, List<FileAnnotation>> annotations = new HashMap<>();
        AnnotationImporter importer = new PdfAnnotationImporter();

        //import annotationsOfFiles if the selected files are valid which is checked in getFilteredFileList()
        for (ParsedFileField parsedFileField : this.getFilteredFileList()) {
            Optional<File> expandedFileName = FileUtil.expandFilename(databaseContext, parsedFileField.getLink(),
                    JabRefPreferences.getInstance().getFileDirectoryPreferences());
            expandedFileName.ifPresent(file -> annotations.put(file.toString(), importer.importAnnotations(file.toPath())));
        }
        return annotations;
    }
}

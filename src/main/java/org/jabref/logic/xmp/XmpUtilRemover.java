package org.jabref.logic.xmp;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.TransformerException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class XmpUtilRemover {

    private static final String XMP_BEGIN_END_TAG = "?xpacket";

    private static final Logger LOGGER = LoggerFactory.getLogger(XmpUtilWriter.class);

    public static void deleteXmp(Path file, BibEntry entry,
                                 BibDatabase database, XmpPreferences xmpPreferences) throws IOException, TransformerException {
        List<BibEntry> bibEntryList = new ArrayList<>();
        bibEntryList.add(entry);
        XmpUtilRemover.deleteXmp(file, bibEntryList, database, xmpPreferences);
    }

    public static void deleteXmp(Path path,
                                 List<BibEntry> bibtexEntries, BibDatabase database,
                                 XmpPreferences xmpPreferences) throws IOException, TransformerException {

        List<BibEntry> resolvedEntries;
        if (database == null) {
            resolvedEntries = bibtexEntries;
        } else {
            resolvedEntries = database.resolveForStrings(bibtexEntries, false);
        }

        try (PDDocument document = Loader.loadPDF(path.toFile())) {

            if (document.isEncrypted()) {
                throw new EncryptedPdfsNotSupportedException();
            }

            // Write schemas (PDDocumentInformation and DublinCoreSchema) to the document metadata
            if (resolvedEntries.size() > 0) {
                XmpUtilRemover.deleteDocumentInformation(document, resolvedEntries.get(0), null, xmpPreferences);
            }

            // Save
            try {
                document.save(path.toFile());
            } catch (IOException e) {
                //lang
                LOGGER.debug("Could not delete XMP metadata", e);
                throw new TransformerException("Could not delete XMP metadata: " + e.getLocalizedMessage(), e);
            }
        }
    }

    private static void deleteDocumentInformation(PDDocument document,
                                                  BibEntry entry, BibDatabase database, XmpPreferences xmpPreferences) {

        PDDocumentInformation di = document.getDocumentInformation();

        BibEntry resolvedEntry = XmpUtilRemover.getDefaultOrDatabaseEntry(entry, database);

        // Query privacy filter settings
        boolean useXmpPrivacyFilter = xmpPreferences.shouldUseXmpPrivacyFilter();

        // Set all the values including key and entryType
        for (Map.Entry<Field, String> fieldValuePair : resolvedEntry.getFieldMap().entrySet()) {
            Field field = fieldValuePair.getKey();
            String fieldContent = fieldValuePair.getValue();

            if (useXmpPrivacyFilter && xmpPreferences.getSelectAllFields().getValue()) {
                // if delete all, no need to check if field is contained in xmp preference
                deleteField(di, field);
            } else if (useXmpPrivacyFilter && xmpPreferences.getXmpPrivacyFilter().contains(field)) {
                // erase field instead of adding it
                deleteField(di, field);
            }
        }
    }

    private static void deleteField(PDDocumentInformation di, Field field) {
        if (StandardField.AUTHOR.equals(field)) {
            di.setAuthor(null);
        } else if (StandardField.TITLE.equals(field)) {
            di.setTitle(null);
        } else if (StandardField.KEYWORDS.equals(field)) {
            di.setKeywords(null);
        } else if (StandardField.ABSTRACT.equals(field)) {
            di.setSubject(null);
        } else {
            di.setCustomMetadataValue("bibtex/" + field, null);
        }
    }

    private static BibEntry getDefaultOrDatabaseEntry(BibEntry defaultEntry, BibDatabase database) {
        if (database == null) {
            return defaultEntry;
        } else {
            return database.resolveForStrings(defaultEntry, false);
        }
    }
}

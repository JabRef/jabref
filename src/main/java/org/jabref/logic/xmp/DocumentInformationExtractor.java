package org.jabref.logic.xmp;

import java.util.Map;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryTypeFactory;

import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;

public class DocumentInformationExtractor {

    private final PDDocumentInformation documentInformation;

    private final BibEntry bibEntry;

    public DocumentInformationExtractor(PDDocumentInformation documentInformation) {
        this.documentInformation = documentInformation;

        this.bibEntry = new BibEntry();
    }

    private void extractAuthor() {
        String s = documentInformation.getAuthor();
        if (s != null) {
            bibEntry.setField(StandardField.AUTHOR, s);
        }
    }

    private void extractTitle() {
        String s = documentInformation.getTitle();
        if (s != null) {
            bibEntry.setField(StandardField.TITLE, s);
        }
    }

    private void extractKeywords() {
        String s = documentInformation.getKeywords();
        if (s != null) {
            bibEntry.setField(StandardField.KEYWORDS, s);
        }
    }

    private void extractSubject() {
        String s = documentInformation.getSubject();
        if (s != null) {
            bibEntry.setField(StandardField.ABSTRACT, s);
        }
    }

    private void extractOtherFields() {
        COSDictionary dict = documentInformation.getCOSObject();
        for (Map.Entry<COSName, COSBase> o : dict.entrySet()) {
            String key = o.getKey().getName();
            if (key.startsWith("bibtex/")) {
                String value = dict.getString(key);
                key = key.substring("bibtex/".length());
                Field field = FieldFactory.parseField(key);
                if (InternalField.TYPE_HEADER.equals(field)) {
                    bibEntry.setType(EntryTypeFactory.parse(value));
                } else {
                    bibEntry.setField(field, value);
                }
            }
        }
    }

    /**
     * Function for retrieving a BibEntry from the
     * PDDocumentInformation in a PDF file.
     *
     * To understand how to get hold of a PDDocumentInformation have a look in
     * the test cases for XMPUtilTest.
     *
     * The BibEntry is build by mapping individual fields in the document
     * information (like author, title, keywords) to fields in a bibtex entry.
     *
     * @return The bibtex entry found in the document information.
     */
    public Optional<BibEntry> extractBibtexEntry() {

        bibEntry.setType(BibEntry.DEFAULT_TYPE);

        this.extractAuthor();
        this.extractTitle();
        this.extractKeywords();
        this.extractSubject();
        this.extractOtherFields();

        if (bibEntry.getFields().isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(bibEntry);
        }
    }
}

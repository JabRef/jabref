package org.jabref.logic.xmp;

import java.util.Map;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;

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
            bibEntry.setField(FieldName.AUTHOR, s);
        }
    }

    private void extractTitle() {
        String s = documentInformation.getTitle();
        if (s != null) {
            bibEntry.setField(FieldName.TITLE, s);
        }
    }

    private void extractKeywords() {
        String s = documentInformation.getKeywords();
        if (s != null) {
            bibEntry.setField(FieldName.KEYWORDS, s);
        }
    }

    private void extractSubject() {
        String s = documentInformation.getSubject();
        if (s != null) {
            bibEntry.setField(FieldName.ABSTRACT, s);
        }
    }

    private void extractOtherFields() {
        COSDictionary dict = documentInformation.getCOSObject();
        for (Map.Entry<COSName, COSBase> o : dict.entrySet()) {
            String key = o.getKey().getName();
            if (key.startsWith("bibtex/")) {
                String value = dict.getString(key);
                key = key.substring("bibtex/".length());
                if (BibEntry.TYPE_HEADER.equals(key)) {
                    bibEntry.setType(value);
                } else {
                    bibEntry.setField(key, value);
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
     * @param di The document information from which to build a BibEntry.
     * @return The bibtex entry found in the document information.
     */
    public Optional<BibEntry> extractBibtexEntry() {

        bibEntry.setType(BibEntry.DEFAULT_TYPE);

        this.extractAuthor();
        this.extractTitle();
        this.extractKeywords();
        this.extractSubject();
        this.extractOtherFields();

        if (bibEntry.getFieldNames().isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(bibEntry);
        }
    }
}

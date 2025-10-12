package org.jabref.logic.xmp;

import java.util.Map;
import java.util.Optional;

import org.jabref.logic.importer.AuthorListParser;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Month;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryTypeFactory;

import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;

/**
 * Related class: {@link org.jabref.logic.xmp.DublinCoreExtractor}
 */
public class DocumentInformationExtractor {

    private static final Map<COSName, Field> FIELD_MAPPING = Map.ofEntries(
            Map.entry(COSName.TITLE, StandardField.TITLE),
            Map.entry(COSName.SUBJECT, StandardField.ABSTRACT),
            Map.entry(COSName.KEYWORDS, StandardField.KEYWORDS),
            Map.entry(COSName.DATE, StandardField.DATE),
            Map.entry(COSName.COLLECTION, StandardField.BOOKTITLE),
            Map.entry(COSName.PAGES, StandardField.PAGES),
            Map.entry(COSName.PAGE, StandardField.PAGES),
            Map.entry(COSName.URL, StandardField.URL),
            Map.entry(COSName.VOLUME, StandardField.VOLUME),
            Map.entry(COSName.VERSION, StandardField.VERSION),
            Map.entry(COSName.ISSUER, StandardField.EDITOR)
    );

    private final PDDocumentInformation documentInformation;

    private final BibEntry bibEntry;

    public DocumentInformationExtractor(PDDocumentInformation documentInformation) {
        this.documentInformation = documentInformation;
        this.bibEntry = new BibEntry();
    }

    private void extractAuthor() {
        String s = documentInformation.getAuthor();
        if (s != null) {
            s = AuthorListParser.normalizeSimply(s).orElse(s);
            bibEntry.setField(StandardField.AUTHOR, s);
        }
    }

    private void extractOtherFields() {
        COSDictionary dict = documentInformation.getCOSObject();
        for (Map.Entry<COSName, COSBase> o : dict.entrySet()) {
            String key = o.getKey().getName();

            if (FIELD_MAPPING.containsKey(o.getKey())) {
                Field field = FIELD_MAPPING.get(o.getKey());
                bibEntry.setField(field, dict.getString(key));
            } else if (key.startsWith(XmpUtilShared.BIBTEX_DI_FIELD_NAME_PREFIX)) {
                String value = dict.getString(key);

                String fieldName = key.substring(XmpUtilShared.BIBTEX_DI_FIELD_NAME_PREFIX.length());
                Field field = FieldFactory.parseField(fieldName);
                switch (field) {
                    case InternalField.TYPE_HEADER ->
                            bibEntry.setType(EntryTypeFactory.parse(value));
                    case StandardField.MONTH -> {
                        value = Month.parse(value).map(Month::getJabRefFormat).orElse(value);
                        bibEntry.setField(StandardField.MONTH, value);
                    }
                    default ->
                            bibEntry.setField(field, value);
                }
            }
        }
    }

    /**
     * Function for retrieving a BibEntry from the
     * PDDocumentInformation in a PDF file.
     * <p>
     * To understand how to get hold of a PDDocumentInformation have a look in
     * the test cases for XMPUtilTest.
     * <p>
     * The BibEntry is build by mapping individual fields in the document
     * information (like author, title, keywords) to fields in a bibtex entry.
     *
     * @return The bibtex entry found in the document information.
     */
    public Optional<BibEntry> extractBibtexEntry() {
        this.extractAuthor();
        this.extractOtherFields();

        if (bibEntry.getFields().isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(bibEntry);
        }
    }
}

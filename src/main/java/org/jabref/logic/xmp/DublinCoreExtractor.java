package org.jabref.logic.xmp;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import org.jabref.logic.TypedBibEntry;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.Month;

import org.apache.xmpbox.DateConverter;
import org.apache.xmpbox.schema.DublinCoreSchema;

public class DublinCoreExtractor {

    private final DublinCoreSchema dcSchema;
    private final XMPPreferences xmpPreferences;

    private final BibEntry bibEntry;

    public DublinCoreExtractor(DublinCoreSchema dcSchema, XMPPreferences xmpPreferences, BibEntry resolvedEntry) {
        this.dcSchema = dcSchema;
        this.xmpPreferences = xmpPreferences;

        this.bibEntry = resolvedEntry;
    }

    /**
     * Editor in BibTex - Contributor in DublinCore
     *
     * @param bibEntry The BibEntry object, which is filled during metadata extraction.
     * @param dcSchema Metadata in DublinCore format.
     */
    private void extractEditor() {
        List<String> contributors = dcSchema.getContributors();
        if ((contributors != null) && !contributors.isEmpty()) {
            bibEntry.setField(FieldName.EDITOR, String.join(" and ", contributors));
        }
    }

    /**
     * Author in BibTex - Creator in DublinCore
     *
     * @param bibEntry The BibEntry object, which is filled during metadata extraction.
     * @param dcSchema Metadata in DublinCore format.
     */
    private void extractAuthor() {
        List<String> creators = dcSchema.getCreators();
        if ((creators != null) && !creators.isEmpty()) {
            bibEntry.setField(FieldName.AUTHOR, String.join(" and ", creators));
        }
    }

    /**
     * Year + Month in BibTex - Date in DublinCore is a combination of year and month information
     *
     * @param bibEntry The BibEntry object, which is filled during metadata extraction.
     * @param dcSchema Metadata in DublinCore format.
     */
    private void extractYearAndMonth() {
        List<String> dates = dcSchema.getUnqualifiedSequenceValueList("date");
        if ((dates != null) && !dates.isEmpty()) {
            String date = dates.get(0).trim();
            Calendar c = null;
            try {
                c = DateConverter.toCalendar(date);
            } catch (IOException ignored) {
                // Ignored
            }
            if (c != null) {
                bibEntry.setField(FieldName.YEAR, String.valueOf(c.get(Calendar.YEAR)));
                if (date.length() > 4) {
                    Optional<Month> month = Month.getMonthByNumber(c.get(Calendar.MONTH) + 1);
                    month.ifPresent(bibEntry::setMonth);
                }
            }
        }
    }

    /**
     * Abstract in BibTex - Description in DublinCore
     *
     * @param bibEntry The BibEntry object, which is filled during metadata extraction.
     * @param dcSchema Metadata in DublinCore format.
     */
    private void extractAbstract() {
        String s = dcSchema.getDescription();
        if (s != null) {
            bibEntry.setField(FieldName.ABSTRACT, s);
        }
    }

    /**
     * DOI in BibTex - Identifier in DublinCore
     *
     * @param bibEntry The BibEntry object, which is filled during metadata extraction.
     * @param dcSchema Metadata in DublinCore format.
     */
    private void extractDOI() {
        String s = dcSchema.getIdentifier();
        if (s != null) {
            bibEntry.setField(FieldName.DOI, s);
        }
    }

    /**
     * Publisher are equivalent in both formats (BibTex and DublinCore)
     *
     * @param bibEntry The BibEntry object, which is filled during metadata extraction.
     * @param dcSchema Metadata in DublinCore format.
     */
    private void extractPublisher() {
        List<String> publishers = dcSchema.getPublishers();
        if ((publishers != null) && !publishers.isEmpty()) {
            bibEntry.setField(FieldName.PUBLISHER, String.join(" and ", publishers));
        }
    }

    /**
     * This method sets all fields, which are custom in bibtext and therefore supported by jabref, but which are not included in the DublinCore format.
     * <p/>
     * The relation attribute of DublinCore is abused to insert these custom fields.
     *
     * @param bibEntry The BibEntry object, which is filled during metadata extraction.
     * @param dcSchema Metadata in DublinCore format.
     */
    private void extractBibTexFields() {
        List<String> relationships = dcSchema.getRelations();
        if (relationships != null) {
            for (String r : relationships) {
                if (r.startsWith("bibtex/")) {
                    r = r.substring("bibtex/".length());
                    int i = r.indexOf('/');
                    if (i != -1) {
                        bibEntry.setField(r.substring(0, i), r.substring(i + 1));
                    }
                }
            }
        }
    }

    /**
     * Rights are equivalent in both formats (BibTex and DublinCore)
     *
     * @param bibEntry The BibEntry object, which is filled during metadata extraction.
     * @param dcSchema Metadata in DublinCore format.
     */
    private void extractRights() {
        String s = dcSchema.getRights();
        if (s != null) {
            bibEntry.setField("rights", s);
        }
    }

    /**
     * Source is equivalent in both formats (BibTex and DublinCore)
     *
     * @param bibEntry The BibEntry object, which is filled during metadata extraction.
     * @param dcSchema Metadata in DublinCore format.
     */
    private void extractSource() {
        String s = dcSchema.getSource();
        if (s != null) {
            bibEntry.setField("source", s);
        }
    }

    /**
     * Keywords in BibTex - Subjects in DublinCore
     * @param bibEntry The BibEntry object, which is filled during metadata extraction.
     * @param dcSchema Metadata in DublinCore format.
     */
    private void extractSubject() {
        List<String> subjects = dcSchema.getSubjects();
        if (subjects != null) {
            bibEntry.addKeywords(subjects, xmpPreferences.getKeywordSeparator());
        }
    }

    /**
     * Title is equivalent in both formats (BibTex and DublinCore)
     *
     * @param bibEntry The BibEntry object, which is filled during metadata extraction.
     * @param dcSchema Metadata in DublinCore format.
     */
    private void extractTitle() {
        String s = dcSchema.getTitle();
        if (s != null) {
            bibEntry.setField(FieldName.TITLE, s);
        }
    }

    /**
     * Type is equivalent in both formats (BibTex and DublinCore)
     *
     * @param bibEntry The BibEntry object, which is filled during metadata extraction.
     * @param dcSchema Metadata in DublinCore format.
     */
    private void extractType() {
        List<String> l = dcSchema.getTypes();
        if ((l != null) && !l.isEmpty()) {
            String s = l.get(0);
            if (s != null) {
                bibEntry.setType(s);
            }
        }
    }

    /**
     * Helper function for retrieving a BibEntry from the DublinCore metadata
     * in a PDF file.
     *
     * To understand how to get hold of a DublinCore have a look in the
     * test cases for XMPUtil.
     *
     * The BibEntry is build by mapping individual fields in the dublin core
     * (like creator, title, subject) to fields in a bibtex bibEntry.
     *
     * @param dcSchema The document information from which to build a BibEntry.
     * @return The bibtex bibEntry found in the document information.
     */
    public Optional<BibEntry> extractBibtexEntry() {

        this.extractEditor();
        this.extractAuthor();
        this.extractYearAndMonth();
        this.extractAbstract();
        this.extractDOI();
        this.extractPublisher();
        this.extractBibTexFields();
        this.extractRights();
        this.extractSource();
        this.extractSubject();
        this.extractTitle();
        this.extractType();

        if (bibEntry.getType() == null) {
            bibEntry.setType(BibEntry.DEFAULT_TYPE);
        }

        return Optional.of(bibEntry);
    }

    /**
     * Bibtex-Fields used: editor, Field: 'dc:contributor'
     *
     * @param authors
     */
    private void fillContributor(String authors) {
        AuthorList list = AuthorList.parse(authors);
        for (Author author : list.getAuthors()) {
            dcSchema.addContributor(author.getFirstLast(false));
        }
    }

    /**
     * Bibtex-Fields used: author, Field: 'dc:creator'
     *
     * @param creators
     */
    private void fillCreator(String creators) {
        AuthorList list = AuthorList.parse(creators);

        for (Author author : list.getAuthors()) {
            dcSchema.addCreator(author.getFirstLast(false));
        }
    }

    /**
     * Bibtex-Fields used: year, month, Field: 'dc:date'
     */
    private void fillDate() {
        bibEntry.getFieldOrAlias(FieldName.DATE)
                .ifPresent(publicationDate -> dcSchema.addUnqualifiedSequenceValue("date", publicationDate));
    }

    /**
     * Bibtex-Fields used: abstract, Field: 'dc:description'
     *
     * @param description
     */
    private void fillDescription(String description) {
        dcSchema.setDescription(description);
    }

    /**
     * Bibtex-Fields used: doi, Field: 'dc:identifier'
     *
     * @param identifier
     */
    private void fillIdentifier(String identifier) {
        dcSchema.setIdentifier(identifier);
    }

    /**
     * Bibtex-Fields used: publisher, Field: dc:publisher
     *
     * @param publisher
     */
    private void fillPublisher(String publisher) {
        dcSchema.addPublisher(publisher);
    }

    /**
     * Bibtex-Fields used: keywords, Field: 'dc:subject'
     *
     * @param value
     */
    private void fillKeywords(String value) {
        String[] keywords = value.split(",");
        for (String keyword : keywords) {
            dcSchema.addSubject(keyword.trim());
        }
    }

    /**
     * Bibtex-Fields used: title, Field: 'dc:title'
     *
     * @param title
     */
    private void fillTitle(String title) {
        dcSchema.setTitle(title);
    }

    /**
     * All others (+ bibtex key) get packaged in the relation attribute
     *
     * @param key Key of the metadata attribute
     * @param value Value of the metadata attribute
     */
    private void fillCustomField(String key, String value) {
        dcSchema.addRelation("bibtex/" + key + '/' + value);
    }

    public void fillDublinCoreSchema() {

        // Query privacy filter settings
        boolean useXmpPrivacyFilter = xmpPreferences.isUseXMPPrivacyFilter();
        // Fields for which not to write XMP data later on:
        Set<String> filters = new TreeSet<>(xmpPreferences.getXmpPrivacyFilter());

        for (Entry<String, String> field : bibEntry.getFieldMap().entrySet()) {

            if (useXmpPrivacyFilter && filters.contains(field.getKey())) {
                continue;
            }

            if (FieldName.EDITOR.equals(field.getKey())) {
                this.fillContributor(field.getValue());
            } else if (FieldName.AUTHOR.equals(field.getKey())) {
                this.fillCreator(field.getValue());
            } else if (FieldName.YEAR.equals(field.getKey())) {
                this.fillDate();
            } else if (FieldName.ABSTRACT.equals(field.getKey())) {
                this.fillDescription(field.getValue());
            } else if (FieldName.DOI.equals(field.getKey())) {
                this.fillIdentifier(field.getValue());
            } else if (FieldName.PUBLISHER.equals(field.getKey())) {
                this.fillPublisher(field.getValue());
            } else if (FieldName.KEYWORDS.equals(field.getKey())) {
                this.fillKeywords(field.getValue());
            } else if (FieldName.TITLE.equals(field.getKey())) {
                this.fillTitle(field.getValue());
            } else {
                this.fillCustomField(field.getKey(), field.getValue());
            }
        }

        dcSchema.setFormat("application/pdf");

        // Bibtex-Fields used: entrytype, Field: 'dc:type'
        TypedBibEntry typedEntry = new TypedBibEntry(bibEntry, BibDatabaseMode.BIBTEX);
        String o = typedEntry.getTypeForDisplay();
        if (!o.isEmpty()) {
            dcSchema.addType(o);
        }
    }
}

package org.jabref.logic.xmp;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.jabref.logic.TypedBibEntry;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.Month;
import org.jabref.model.strings.StringUtil;

import org.apache.xmpbox.DateConverter;
import org.apache.xmpbox.schema.DublinCoreSchema;

public class DublinCoreExtractor {

    private final DublinCoreSchema dcSchema;
    private final XmpPreferences xmpPreferences;

    private final BibEntry bibEntry;

    public DublinCoreExtractor(DublinCoreSchema dcSchema, XmpPreferences xmpPreferences, BibEntry resolvedEntry) {
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
     * Year in BibTex - Date in DublinCore is only the year information, because dc interprets empty months as January.
     * Tries to extract the month as well.
     * In JabRef the bibtex/month/value is prioritized.
     * <br/>
     * The problem is the default value of the calendar, which is always January, also if there is no month information in
     * the xmp metdata. The idea is, to reject all information with YYYY-01-01. In cases, where xmp is written with JabRef
     * the month property filled with jan will override this behavior and no data is lost. In the cases, where xmp
     * is written by another service, the assumption is, that the 1st January is not a publication date at all.
     *
     * @param bibEntry The BibEntry object, which is filled during metadata extraction.
     * @param dcSchema Metadata in DublinCore format.
     */
    private void extractYearAndMonth() {
        List<String> dates = dcSchema.getUnqualifiedSequenceValueList("date");
        if ((dates != null) && !dates.isEmpty()) {
            String date = dates.get(0).trim();
            Calendar calender = null;
            try {
                calender = DateConverter.toCalendar(date);
            } catch (IOException ignored) {
                // Ignored
            }
            if (calender != null) {
                bibEntry.setField(FieldName.YEAR, String.valueOf(calender.get(Calendar.YEAR)));
                // not the 1st of January
                if (!((calender.get(Calendar.MONTH) == 0) && (calender.get(Calendar.DAY_OF_MONTH) == 1))) {
                    Optional<Month> month = Month.getMonthByNumber(calender.get(Calendar.MONTH) + 1);
                    if (month.isPresent()) {
                        bibEntry.setField(FieldName.MONTH, month.get().getShortName());
                    }
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
        String description = dcSchema.getDescription();
        if (!StringUtil.isNullOrEmpty(description)) {
            bibEntry.setField(FieldName.ABSTRACT, description);
        }
    }

    /**
     * DOI in BibTex - Identifier in DublinCore
     *
     * @param bibEntry The BibEntry object, which is filled during metadata extraction.
     * @param dcSchema Metadata in DublinCore format.
     */
    private void extractDOI() {
        String identifier = dcSchema.getIdentifier();
        if (!StringUtil.isNullOrEmpty(identifier)) {
            bibEntry.setField(FieldName.DOI, identifier);
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

        Predicate<String> isBibTeXElement = s -> s.startsWith("bibtex/");

        Consumer<String> splitBibTeXElement = s -> {
            // the default pattern is bibtex/key/value, but some fields contains url etc.
            // so the value property contains additional slashes, which makes the usage of
            // String#split complicated.
            String temp = s.substring("bibtex/".length());
            int i = temp.indexOf('/');
            if (i != -1) {
                String key = temp.substring(0, i);
                String value = temp.substring(i + 1);
                bibEntry.setField(key, value);

                // only for month field - override value
                // workaround, because the date value of the xmp component of pdf box is corrupted
                // see also DublinCoreExtractor#extractYearAndMonth
                if ("month".equals(key)) {
                    Optional<Month> parsedMonth = Month.parse(value);
                    if (parsedMonth.isPresent()) {
                        bibEntry.setField(key, parsedMonth.get().getShortName());
                    }
                }
            }

        };
        if (relationships != null) {
            relationships.stream()
                    .filter(isBibTeXElement)
                    .forEach(splitBibTeXElement);
        }
    }

    /**
     * Rights are equivalent in both formats (BibTex and DublinCore)
     *
     * @param bibEntry The BibEntry object, which is filled during metadata extraction.
     * @param dcSchema Metadata in DublinCore format.
     */
    private void extractRights() {
        String rights = dcSchema.getRights();
        if (!StringUtil.isNullOrEmpty(rights)) {
            bibEntry.setField("rights", rights);
        }
    }

    /**
     * Source is equivalent in both formats (BibTex and DublinCore)
     *
     * @param bibEntry The BibEntry object, which is filled during metadata extraction.
     * @param dcSchema Metadata in DublinCore format.
     */
    private void extractSource() {
        String source = dcSchema.getSource();
        if (!StringUtil.isNullOrEmpty(source)) {
            bibEntry.setField("source", source);
        }
    }

    /**
     * Keywords in BibTex - Subjects in DublinCore
     * @param bibEntry The BibEntry object, which is filled during metadata extraction.
     * @param dcSchema Metadata in DublinCore format.
     */
    private void extractSubject() {
        List<String> subjects = dcSchema.getSubjects();
        if ((subjects != null) && !subjects.isEmpty()) {
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
        String title = dcSchema.getTitle();
        if (!StringUtil.isNullOrEmpty(title)) {
            bibEntry.setField(FieldName.TITLE, title);
        }
    }

    /**
     * Type is equivalent in both formats (BibTex and DublinCore)
     *
     * @param bibEntry The BibEntry object, which is filled during metadata extraction.
     * @param dcSchema Metadata in DublinCore format.
     */
    private void extractType() {
        List<String> types = dcSchema.getTypes();
        if ((types != null) && !types.isEmpty()) {
            String type = types.get(0);
            if (!StringUtil.isNullOrEmpty(type)) {
                bibEntry.setType(type);
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

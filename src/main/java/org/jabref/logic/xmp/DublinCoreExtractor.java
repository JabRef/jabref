package org.jabref.logic.xmp;

import java.io.IOException;
import java.util.Calendar;
import java.util.Comparator;
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
import org.jabref.model.entry.Month;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.EntryTypeFactory;
import org.jabref.model.strings.StringUtil;

import org.apache.xmpbox.DateConverter;
import org.apache.xmpbox.schema.DublinCoreSchema;

public class DublinCoreExtractor {

    private final DublinCoreSchema dcSchema;
    private final XmpPreferences xmpPreferences;

    private final BibEntry bibEntry;

    /**
     * @param dcSchema      Metadata in DublinCore format.
     * @param resolvedEntry The BibEntry object, which is filled during metadata extraction.
     */
    public DublinCoreExtractor(DublinCoreSchema dcSchema, XmpPreferences xmpPreferences, BibEntry resolvedEntry) {
        this.dcSchema = dcSchema;
        this.xmpPreferences = xmpPreferences;

        this.bibEntry = resolvedEntry;
    }

    /**
     * Editor in BibTex - Contributor in DublinCore
     */
    private void extractEditor() {
        List<String> contributors = dcSchema.getContributors();
        if ((contributors != null) && !contributors.isEmpty()) {
            bibEntry.setField(StandardField.EDITOR, String.join(" and ", contributors));
        }
    }

    /**
     * Author in BibTex - Creator in DublinCore
     */
    private void extractAuthor() {
        List<String> creators = dcSchema.getCreators();
        if ((creators != null) && !creators.isEmpty()) {
           bibEntry.setField(StandardField.AUTHOR, String.join(" and ", creators));
        }
    }

    /**
     * Year in BibTex - Date in DublinCore is only the year information, because dc interprets empty months as January.
     * Tries to extract the month as well. In JabRef the bibtex/month/value is prioritized. <br/> The problem is the
     * default value of the calendar, which is always January, also if there is no month information in the xmp metdata.
     * The idea is, to reject all information with YYYY-01-01. In cases, where xmp is written with JabRef the month
     * property filled with jan will override this behavior and no data is lost. In the cases, where xmp is written by
     * another service, the assumption is, that the 1st January is not a publication date at all.
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
                bibEntry.setField(StandardField.YEAR, String.valueOf(calender.get(Calendar.YEAR)));
                int monthNumber = calender.get(Calendar.MONTH) + 1;
                // not the 1st of January
                if (!((monthNumber == 1) && (calender.get(Calendar.DAY_OF_MONTH) == 1))) {
                    Month.getMonthByNumber(monthNumber)
                         .ifPresent(month -> bibEntry.setMonth(month));
                }
            }
        }
    }

    /**
     * Abstract in BibTex - Description in DublinCore
     */
    private void extractAbstract() {
        String description = dcSchema.getDescription();
        if (!StringUtil.isNullOrEmpty(description)) {
            bibEntry.setField(StandardField.ABSTRACT, description);
        }
    }

    /**
     * DOI in BibTex - Identifier in DublinCore
     */
    private void extractDOI() {
        String identifier = dcSchema.getIdentifier();
        if (!StringUtil.isNullOrEmpty(identifier)) {
            bibEntry.setField(StandardField.DOI, identifier);
        }
    }

    /**
     * Publisher are equivalent in both formats (BibTex and DublinCore)
     */
    private void extractPublisher() {
        List<String> publishers = dcSchema.getPublishers();
        if ((publishers != null) && !publishers.isEmpty()) {
            bibEntry.setField(StandardField.PUBLISHER, String.join(" and ", publishers));
        }
    }

    /**
     * This method sets all fields, which are custom in BibTeX and therefore supported by JabRef, but which are not
     * included in the DublinCore format.
     * <p>
     * The relation attribute of DublinCore is abused to insert these custom fields.
     */
    private void extractBibTexFields() {
        Predicate<String> isBibTeXElement = s -> s.startsWith("bibtex/");
        Consumer<String> splitBibTeXElement = s -> {
            // the default pattern is bibtex/key/value, but some fields contains url etc.
            // so the value property contains additional slashes, which makes the usage of
            // String#split complicated.
            String temp = s.substring("bibtex/".length());
            int i = temp.indexOf('/');
            if (i != -1) {
                Field key = FieldFactory.parseField(temp.substring(0, i));
                String value = temp.substring(i + 1);
                bibEntry.setField(key, value);

                // only for month field - override value
                // workaround, because the date value of the xmp component of pdf box is corrupted
                // see also DublinCoreExtractor#extractYearAndMonth
                if (StandardField.MONTH.equals(key)) {
                    Optional<Month> parsedMonth = Month.parse(value);
                    parsedMonth.ifPresent(bibEntry::setMonth);
                }
            }
        };
        List<String> relationships = dcSchema.getRelations();
        if (relationships != null) {
            relationships.stream()
                         .filter(isBibTeXElement)
                         .forEach(splitBibTeXElement);
        }
    }

    /**
     * Rights are equivalent in both formats (BibTex and DublinCore)
     */
    private void extractRights() {
        String rights = dcSchema.getRights();
        if (!StringUtil.isNullOrEmpty(rights)) {
            bibEntry.setField(new UnknownField("rights"), rights);
        }
    }

    /**
     * Source is equivalent in both formats (BibTex and DublinCore)
     */
    private void extractSource() {
        String source = dcSchema.getSource();
        if (!StringUtil.isNullOrEmpty(source)) {
            bibEntry.setField(new UnknownField("source"), source);
        }
    }

    /**
     * Keywords in BibTex - Subjects in DublinCore
     */
    private void extractSubject() {
        List<String> subjects = dcSchema.getSubjects();
        if ((subjects != null) && !subjects.isEmpty()) {
            bibEntry.addKeywords(subjects, xmpPreferences.getKeywordSeparator());
        }
    }

    /**
     * Title is equivalent in both formats (BibTex and DublinCore)
     */
    private void extractTitle() {
        String title = dcSchema.getTitle();
        if (!StringUtil.isNullOrEmpty(title)) {
            bibEntry.setField(StandardField.TITLE, title);
        }
    }

    /**
     * Type is equivalent in both formats (BibTex and DublinCore)
     */
    private void extractType() {
        List<String> types = dcSchema.getTypes();
        if ((types != null) && !types.isEmpty()) {
            String type = types.get(0);
            if (!StringUtil.isNullOrEmpty(type)) {
                bibEntry.setType(EntryTypeFactory.parse(type));
            }
        }
    }

    /**
     * Helper function for retrieving a BibEntry from the DublinCore metadata in a PDF file.
     * <p>
     * To understand how to get hold of a DublinCore have a look in the test cases for XMPUtil.
     * <p>
     * The BibEntry is build by mapping individual fields in the dublin core (like creator, title, subject) to fields in
     * a bibtex bibEntry. In case special "bibtex/" entries are contained, the normal dublin core fields take
     * precedence. For instance, the dublin core date takes precedence over bibtex/month.
     *
     * @return The bibEntry extracted from the document information.
     */
    public Optional<BibEntry> extractBibtexEntry() {
        // first extract "bibtex/" entries
        this.extractBibTexFields();

        // then extract all "standard" dublin core entries
        this.extractEditor();
        this.extractAuthor();
        this.extractYearAndMonth();
        this.extractAbstract();
        this.extractDOI();
        this.extractPublisher();
        this.extractRights();
        this.extractSource();
        this.extractSubject();
        this.extractTitle();
        this.extractType();

        // we pass a new BibEntry in the constructor which is never empty as it already consists of "@misc"
        if (bibEntry.getFieldMap().isEmpty()) {
            return Optional.empty();
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
        bibEntry.getFieldOrAlias(StandardField.DATE)
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
     * All others (+ citation key) get packaged in the relation attribute
     *
     * @param field Key of the metadata attribute
     * @param value Value of the metadata attribute
     */
    private void fillCustomField(Field field, String value) {
        dcSchema.addRelation("bibtex/" + field.getName() + '/' + value);
    }

    public void fillDublinCoreSchema() {
        // Query privacy filter settings
        boolean useXmpPrivacyFilter = xmpPreferences.shouldUseXmpPrivacyFilter();

        Set<Entry<Field, String>> fieldValues = new TreeSet<>(Comparator.comparing(fieldStringEntry -> fieldStringEntry.getKey().getName()));
        fieldValues.addAll(bibEntry.getFieldMap().entrySet());
        for (Entry<Field, String> field : fieldValues) {
            if (useXmpPrivacyFilter && xmpPreferences.getXmpPrivacyFilter().contains(field.getKey())) {
                continue;
            }

            if (StandardField.EDITOR.equals(field.getKey())) {
                this.fillContributor(field.getValue());
            } else if (StandardField.AUTHOR.equals(field.getKey())) {
                this.fillCreator(field.getValue());
            } else if (StandardField.YEAR.equals(field.getKey())) {
                this.fillDate();
            } else if (StandardField.ABSTRACT.equals(field.getKey())) {
                this.fillDescription(field.getValue());
            } else if (StandardField.DOI.equals(field.getKey())) {
                this.fillIdentifier(field.getValue());
            } else if (StandardField.PUBLISHER.equals(field.getKey())) {
                this.fillPublisher(field.getValue());
            } else if (StandardField.KEYWORDS.equals(field.getKey())) {
                this.fillKeywords(field.getValue());
            } else if (StandardField.TITLE.equals(field.getKey())) {
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

package org.jabref.logic.xmp;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.jabref.logic.TypedBibEntry;
import org.jabref.logic.formatter.casechanger.UnprotectTermsFormatter;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Date;
import org.jabref.model.entry.Month;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.EntryTypeFactory;
import org.jabref.model.strings.StringUtil;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.xmpbox.schema.DublinCoreSchema;
import org.apache.xmpbox.type.BadFieldValueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used for <em>both</em> conversion from Dublin Core to BibTeX and conversion form BibTeX to Dublin Core
 */
public class DublinCoreExtractor {

    public static final String DC_COVERAGE = "coverage";
    public static final String DC_RIGHTS = "rights";
    public static final String DC_SOURCE = "source";

    private static final Logger LOGGER = LoggerFactory.getLogger(DublinCoreExtractor.class);

    private final DublinCoreSchema dcSchema;
    private final XmpPreferences xmpPreferences;

    private final BibEntry bibEntry;

    private final UnprotectTermsFormatter unprotectTermsFormatter = new UnprotectTermsFormatter();

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
     * Editor in BibTeX - Contributor in DublinCore
     */
    private void extractEditor() {
        List<String> contributors = dcSchema.getContributors();
        if ((contributors != null) && !contributors.isEmpty()) {
            bibEntry.setField(StandardField.EDITOR, String.join(" and ", contributors));
        }
    }

    /**
     * Author in BibTeX - Creator in DublinCore
     */
    private void extractAuthor() {
        List<String> creators = dcSchema.getCreators();
        if ((creators != null) && !creators.isEmpty()) {
           bibEntry.setField(StandardField.AUTHOR, String.join(" and ", creators));
        }
    }

    /**
     * BibTeX-Fields : year, [month], [day] - 'dc:date' in DublinCore
     */
    private void extractDate() {
        List<String> dates = dcSchema.getUnqualifiedSequenceValueList("date");
        if ((dates != null) && !dates.isEmpty()) {
            String date = dates.get(0).trim();
            Date.parse(date)
                    .ifPresent(dateValue -> {
                        dateValue.getDay().ifPresent(day -> bibEntry.setField(StandardField.DAY, Integer.toString(day)));
                        dateValue.getMonth().ifPresent(bibEntry::setMonth);
                        dateValue.getYear().ifPresent(year -> bibEntry.setField(StandardField.YEAR, Integer.toString(year)));
                    });
        }
    }

    /**
     * Abstract in BibTeX - Description in DublinCore
     */
    private void extractAbstract() {
        String description = null;
        try {
            description = dcSchema.getDescription();
        } catch (BadFieldValueException e) {
            LOGGER.warn("Could not get abstract", e);
        }
        if (!StringUtil.isNullOrEmpty(description)) {
            bibEntry.setField(StandardField.ABSTRACT, description);
        }
    }

    /**
     * DOI in BibTeX - Identifier in DublinCore
     */
    private void extractDOI() {
        String identifier = dcSchema.getIdentifier();
        if (!StringUtil.isNullOrEmpty(identifier)) {
            bibEntry.setField(StandardField.DOI, identifier);
        }
    }

    /**
     * Publisher are equivalent in both formats (BibTeX and DublinCore)
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
     * The relation attribute of DublinCore is abused to store these custom fields. The prefix <code>bibtex</code> is used.
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
     * Rights are equivalent in both formats (BibTeX and DublinCore)
     */
    private void extractRights() {
        String rights = null;
        try {
            rights = dcSchema.getRights();
        } catch (BadFieldValueException e) {
           LOGGER.warn("Could not extract rights", e);
        }
        if (!StringUtil.isNullOrEmpty(rights)) {
            bibEntry.setField(new UnknownField(DC_RIGHTS), rights);
        }
    }

    /**
     * Source is equivalent in both formats (BibTeX and DublinCore)
     */
    private void extractSource() {
        String source = dcSchema.getSource();
        if (!StringUtil.isNullOrEmpty(source)) {
            bibEntry.setField(new UnknownField(DC_SOURCE), source);
        }
    }

    /**
     * Keywords in BibTeX - Subjects in DublinCore
     */
    private void extractSubject() {
        List<String> subjects = dcSchema.getSubjects();
        if ((subjects != null) && !subjects.isEmpty()) {
            bibEntry.addKeywords(subjects, xmpPreferences.getKeywordSeparator());
        }
    }

    /**
     * Title is equivalent in both formats (BibTeX and DublinCore)
     */
    private void extractTitle() {
        String title = null;
        try {
            title = dcSchema.getTitle();
        } catch (BadFieldValueException e) {
            LOGGER.warn("Could not extract title", e);
        }
        if (!StringUtil.isNullOrEmpty(title)) {
            bibEntry.setField(StandardField.TITLE, title);
        }
    }

    /**
     * Type is equivalent in both formats (BibTeX and DublinCore)
     * <p>Opposite method: {@link DublinCoreExtractor#fillType()}
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
     * No Equivalent in BibTeX. Will create an Unknown "Coverage" Field
     */
    private void extractCoverage() {
        String coverage = dcSchema.getCoverage();
        if (!StringUtil.isNullOrEmpty(coverage)) {
            bibEntry.setField(FieldFactory.parseField(DC_COVERAGE), coverage);
        }
    }

    /**
     *  Language is equivalent in both formats (BibTeX and DublinCore)
     */
    private void extractLanguages() {
        StringBuilder builder = new StringBuilder();

        List<String> languages = dcSchema.getLanguages();
        if (languages != null && !languages.isEmpty()) {
            languages.forEach(language -> builder.append(",").append(language));
            bibEntry.setField(StandardField.LANGUAGE, builder.substring(1));
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
     * <p>
     * The opposite method is {@link DublinCoreExtractor#fillDublinCoreSchema()}
     * </p>
     *
     * @return The bibEntry extracted from the document information.
     */
    public Optional<BibEntry> extractBibtexEntry() {
        // first extract "bibtex/" entries
        this.extractBibTexFields();

        // then extract all "standard" dublin core entries
        this.extractType();
        this.extractEditor();
        this.extractAuthor();
        this.extractDate();
        this.extractAbstract();
        this.extractDOI();
        this.extractPublisher();
        this.extractRights();
        this.extractSource();
        this.extractSubject();
        this.extractTitle();
        this.extractCoverage();
        this.extractLanguages();

        // we pass a new BibEntry in the constructor which is never empty as it already consists of "@misc"
        if (bibEntry.getFieldMap().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(bibEntry);
    }

    /**
     * BibTeX: editor; DC: 'dc:contributor'
     */
    private void fillContributor(String authors) {
        AuthorList list = AuthorList.parse(authors);
        for (Author author : list.getAuthors()) {
            dcSchema.addContributor(author.getFirstLast(false));
        }
    }

    /**
     * BibTeX: author; DC: 'dc:creator'
     */
    private void fillCreator(String creators) {
        AuthorList list = AuthorList.parse(creators);
        for (Author author : list.getAuthors()) {
            dcSchema.addCreator(author.getFirstLast(false));
        }
    }

    /**
     * BibTeX: year, month; DC: 'dc:date'
     */
    private void fillDate() {
        bibEntry.getFieldOrAlias(StandardField.DATE)
                .ifPresent(publicationDate -> dcSchema.addUnqualifiedSequenceValue("date", publicationDate));
    }

    /**
     * BibTeX: abstract; DC: 'dc:description'
     */
    private void fillDescription(String description) {
        dcSchema.setDescription(description);
    }

    /**
     * BibTeX:doi; DC: 'dc:identifier'
     */
    private void fillIdentifier(String identifier) {
        dcSchema.setIdentifier(identifier);
    }

    /**
     * BibTeX: publisher, DC: dc:publisher
     */
    private void fillPublisher(String publisher) {
        dcSchema.addPublisher(publisher);
    }

    /**
     * BibTeX: keywords; DC: 'dc:subject'
     */
    private void fillKeywords(String value) {
        String[] keywords = value.split(xmpPreferences.getKeywordSeparator().toString());
        for (String keyword : keywords) {
            dcSchema.addSubject(keyword.trim());
        }
    }

    /**
     * BibTeX: title; DC: 'dc:title'
     */
    private void fillTitle(String title) {
        dcSchema.setTitle(title);
    }

    /**
     * BibTeX: Coverage (Custom Field); DC Field : Coverage
     */
    private void fillCoverage(String coverage) {
        dcSchema.setCoverage(coverage);
    }

    /**
     * BibTeX: language; DC: dc:language
     */
    private void fillLanguages(String languages) {
        Arrays.stream(languages.split(","))
                .forEach(dcSchema::addLanguage);
    }

    /**
     * BibTeX: Rights (Custom Field); DC: dc:rights
     */
    private void fillRights(String rights) {
        dcSchema.addRights(null, rights.split(",")[0]);
    }

    /**
     * BibTeX: Source (Custom Field); DC: Source
     */
    private void fillSource(String source) {
        dcSchema.setSource(source);
    }

    /**
     * All others (+ citation key) get packaged in the dc:relation attribute with <code>bibtex/</code> prefix in the content.
     * The value of the given field is fetched from the class variable {@link DublinCoreExtractor#bibEntry}.
     */
    private void fillCustomField(Field field) {
        // We write the plain content of the field, because this is a custom DC field content with the semantics that
        // BibTeX data is stored. Thus, we do not need to get rid of BibTeX, but can keep it.
        String value = bibEntry.getField(field).get();
        dcSchema.addRelation("bibtex/" + field.getName() + '/' + value);
    }

    /**
     * Opposite method: {@link DublinCoreExtractor#extractType()}
     */
    private void fillType() {
        // BibTeX: entry type; DC: 'dc:type'
        TypedBibEntry typedEntry = new TypedBibEntry(bibEntry, BibDatabaseMode.BIBTEX);
        String o = typedEntry.getTypeForDisplay();
        if (!o.isEmpty()) {
            dcSchema.addType(o);
        }
    }

    /**
     * Converts the content of the bibEntry to dublin core.
     * <p>
     * The opposite method is {@link DublinCoreExtractor#extractBibtexEntry()}.
     * <p>
     *     A similar method for writing the DocumentInformationItem (DII) is {@link XmpUtilWriter#writeDocumentInformation(PDDocument, BibEntry, BibDatabase, XmpPreferences)}
     * </p>
     */
    public void fillDublinCoreSchema() {
        // Query privacy filter settings
        boolean useXmpPrivacyFilter = xmpPreferences.shouldUseXmpPrivacyFilter();

        Set<Field> fields = bibEntry.getFields();
        for (Field field : fields) {
            if (useXmpPrivacyFilter && xmpPreferences.getXmpPrivacyFilter().contains(field)) {
                continue;
            }

            dcSchema.setFormat("application/pdf");
            fillType();

            String value = unprotectTermsFormatter.format(bibEntry.getField(field).get());
            if (field instanceof StandardField standardField) {
                switch (standardField) {
                    case EDITOR ->
                            this.fillContributor(value);
                    case AUTHOR ->
                            this.fillCreator(value);
                    case YEAR ->
                            this.fillDate();
                    case ABSTRACT ->
                            this.fillDescription(value);
                    case DOI ->
                            this.fillIdentifier(value);
                    case PUBLISHER ->
                            this.fillPublisher(value);
                    case KEYWORDS ->
                            this.fillKeywords(value);
                    case TITLE ->
                            this.fillTitle(value);
                    case LANGUAGE ->
                            this.fillLanguages(value);
                    case FILE -> {
                        // we do not write the "file" field, because the file is the PDF itself
                    }
                    case DAY, MONTH -> {
                        // we do not write day and month separately if dc:year can be used
                        if (!bibEntry.hasField(StandardField.YEAR)) {
                            this.fillCustomField(field);
                        }
                    }
                    default ->
                            this.fillCustomField(field);
                }
            } else {
                if (DC_COVERAGE.equals(field.getName())) {
                    this.fillCoverage(value);
                } else if (DC_RIGHTS.equals(field.getName())) {
                    this.fillRights(value);
                } else if (DC_SOURCE.equals(field.getName())) {
                    this.fillSource(value);
                } else {
                    this.fillCustomField(field);
                }
            }
        }
    }
}

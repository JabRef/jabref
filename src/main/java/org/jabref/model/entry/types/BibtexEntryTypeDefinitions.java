package org.jabref.model.entry.types;

import java.util.Arrays;
import java.util.List;

import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypeBuilder;
import org.jabref.model.entry.field.OrFields;
import org.jabref.model.entry.field.StandardField;

/**
 * This class represents all supported BibTex entry types.
 * <p>
 * Article, Book, Booklet, Conference, Inbook, Incollection, Inproceedings,
 * Manual, Mastersthesis, Misc, Phdthesis, Proceedings, Techreport, Unpublished
 */
public class BibtexEntryTypeDefinitions {
    /**
     * An article from a journal or magazine.
     * <p>
     * Required fields: author, title, journal, year.
     * Optional fields: volume, number, pages, month, note.
     */
    private static final BibEntryType ARTICLE = new BibEntryTypeBuilder()
            .withType(StandardEntryType.Article)
            .withRequiredFields(StandardField.AUTHOR, StandardField.TITLE, StandardField.JOURNAL, StandardField.YEAR)
            .withImportantFields(StandardField.VOLUME, StandardField.NUMBER, StandardField.PAGES, StandardField.MONTH, StandardField.ISSN, StandardField.NOTE)
            .build();

    /**
     * A book with an explicit publisher.
     * <p>
     * Required fields: author or editor, title, publisher, year.
     * Optional fields: volume or number, series, address, edition, month, note.
     */
    private static final BibEntryType BOOK = new BibEntryTypeBuilder()
            .withType(StandardEntryType.Book)
            .withRequiredFields(new OrFields(StandardField.AUTHOR, StandardField.EDITOR), StandardField.TITLE, StandardField.PUBLISHER, StandardField.YEAR)
            .withImportantFields(StandardField.VOLUME, StandardField.NUMBER, StandardField.SERIES, StandardField.ADDRESS, StandardField.EDITION, StandardField.MONTH, StandardField.ISBN, StandardField.NOTE)
            .build();

    /**
     * A work that is printed and bound, but without a named publisher or sponsoring institution.
     * <p>
     * Required field: title.
     * Optional fields: author, howpublished, address, month, year, note.
     */
    private static final BibEntryType BOOKLET = new BibEntryTypeBuilder()
            .withType(StandardEntryType.Booklet)
            .withRequiredFields(StandardField.TITLE)
            .withImportantFields(StandardField.AUTHOR, StandardField.HOWPUBLISHED, StandardField.ADDRESS, StandardField.MONTH, StandardField.YEAR, StandardField.NOTE)
            .build();

    /**
     * An article in a conference proceedings.
     * <p>
     * Required fields: author, title, booktitle, year.
     * Optional fields: editor, volume or number, series, pages, address, month, organization, publisher, note.
     */
    private static final BibEntryType CONFERENCE = new BibEntryTypeBuilder()
            .withType(StandardEntryType.Conference)
            .withRequiredFields(StandardField.AUTHOR, StandardField.TITLE, StandardField.BOOKTITLE, StandardField.YEAR)
            .withImportantFields(StandardField.EDITOR, StandardField.VOLUME, StandardField.NUMBER, StandardField.SERIES, StandardField.PAGES, StandardField.ADDRESS, StandardField.MONTH, StandardField.ORGANIZATION, StandardField.PUBLISHER, StandardField.NOTE)
            .build();

    /**
     * A part of a book, which may be a chapter (or section or whatever) and/or a range of pages.
     * <p>
     * Required fields: author or editor, title, chapter and/or pages, publisher, year.
     * Optional fields: volume or number, series, type, address, edition, month, note.
     */
    private static final BibEntryType INBOOK = new BibEntryTypeBuilder()
            .withType(StandardEntryType.InBook)
            .withRequiredFields(Arrays.asList(new OrFields(StandardField.CHAPTER, StandardField.PAGES), new OrFields(StandardField.AUTHOR, StandardField.EDITOR)), StandardField.TITLE, StandardField.PUBLISHER, StandardField.YEAR)
            .withImportantFields(StandardField.VOLUME, StandardField.NUMBER, StandardField.SERIES, StandardField.TYPE, StandardField.ADDRESS, StandardField.EDITION, StandardField.MONTH, StandardField.ISBN, StandardField.NOTE)
            .build();

    /**
     * A part of a book having its own title.
     * Required fields: author, title, booktitle, publisher, year.
     * Optional fields: editor, volume or number, series, type, chapter, pages, address, edition, month, note.
     */
    private static final BibEntryType INCOLLECTION = new BibEntryTypeBuilder()
            .withType(StandardEntryType.InCollection)
            .withRequiredFields(StandardField.AUTHOR, StandardField.TITLE, StandardField.BOOKTITLE, StandardField.PUBLISHER, StandardField.YEAR)
            .withImportantFields(StandardField.EDITOR, StandardField.VOLUME, StandardField.NUMBER, StandardField.SERIES, StandardField.TYPE, StandardField.CHAPTER, StandardField.PAGES, StandardField.ADDRESS, StandardField.EDITION, StandardField.MONTH, StandardField.ISBN, StandardField.NOTE)
            .build();

    /**
     * An article in a conference proceedings.
     * <p>
     * Required fields: author, title, booktitle, year.
     * Optional fields: editor, volume or number, series, pages, address, month, organization, publisher, note.
     */
    private static final BibEntryType INPROCEEDINGS = new BibEntryTypeBuilder()
            .withType(StandardEntryType.InProceedings)
            .withRequiredFields(StandardField.AUTHOR, StandardField.TITLE, StandardField.BOOKTITLE, StandardField.YEAR)
            .withImportantFields(StandardField.EDITOR, StandardField.VOLUME, StandardField.NUMBER, StandardField.SERIES, StandardField.PAGES, StandardField.ADDRESS, StandardField.MONTH, StandardField.ORGANIZATION, StandardField.PUBLISHER, StandardField.NOTE)
            .build();

    /**
     * Technical documentation.
     * Required field: title.
     * Optional fields: author, organization, address, edition, month, year, note.
     */
    private static final BibEntryType MANUAL = new BibEntryTypeBuilder().withRequiredFields(StandardField.TITLE).withImportantFields(StandardField.AUTHOR, StandardField.ORGANIZATION, StandardField.ADDRESS, StandardField.EDITION, StandardField.MONTH, StandardField.YEAR, StandardField.ISBN, StandardField.NOTE).withType(StandardEntryType.Manual)
                                                                        .build();

    /**
     * A Master's thesis.
     * <p>
     * Required fields: author, title, school, year.
     * Optional fields: type, address, month, note.
     */
    private static final BibEntryType MASTERSTHESIS = new BibEntryTypeBuilder()
            .withType(StandardEntryType.MastersThesis)
            .withRequiredFields(StandardField.AUTHOR, StandardField.TITLE, StandardField.SCHOOL, StandardField.YEAR)
            .withImportantFields(StandardField.TYPE, StandardField.ADDRESS, StandardField.MONTH, StandardField.NOTE)
            .build();

    /**
     * Use this type when nothing else fits.
     * <p>
     * Required fields: none.
     * Optional fields: author, title, howpublished, month, year, note.
     */
    private static final BibEntryType MISC = new BibEntryTypeBuilder()
            .withType(StandardEntryType.Misc)
            .withImportantFields(StandardField.AUTHOR, StandardField.TITLE, StandardField.HOWPUBLISHED, StandardField.MONTH, StandardField.YEAR, StandardField.NOTE)
            .build();

    /**
     * A PhD thesis.
     * <p>
     * Required fields: author, title, school, year.
     * Optional fields: type, address, month, note.
     */
    private static final BibEntryType PHDTHESIS = new BibEntryTypeBuilder()
            .withType(StandardEntryType.PhdThesis)
            .withRequiredFields(StandardField.AUTHOR, StandardField.TITLE, StandardField.SCHOOL, StandardField.YEAR)
            .withImportantFields(StandardField.TYPE, StandardField.ADDRESS, StandardField.MONTH, StandardField.NOTE)
            .build();

    /**
     * The proceedings of a conference.
     * <p>
     * Required fields: title, year.
     * Optional fields: editor, volume or number, series, address, month, organization, publisher, note.
     */
    private static final BibEntryType PROCEEDINGS = new BibEntryTypeBuilder()
            .withType(StandardEntryType.Proceedings)
            .withRequiredFields(StandardField.TITLE, StandardField.YEAR)
            .withImportantFields(StandardField.EDITOR, StandardField.VOLUME, StandardField.NUMBER, StandardField.SERIES, StandardField.ADDRESS, StandardField.PUBLISHER, StandardField.MONTH, StandardField.ORGANIZATION, StandardField.ISBN, StandardField.NOTE)
            .build();

    /**
     * A report published by a school or other institution, usually numbered within a series.
     * <p>
     * Required fields: author, title, institution, year.
     * Optional fields: type, number, address, month, note.
     */
    private static final BibEntryType TECHREPORT = new BibEntryTypeBuilder()
            .withType(StandardEntryType.TechReport)
            .withRequiredFields(StandardField.AUTHOR, StandardField.TITLE, StandardField.INSTITUTION, StandardField.YEAR)
            .withImportantFields(StandardField.TYPE, StandardField.NUMBER, StandardField.ADDRESS, StandardField.MONTH, StandardField.NOTE)
            .build();

    /**
     * A document having an author and title, but not formally published.
     * <p>
     * Required fields: author, title, note.
     * Optional fields: month, year.
     */
    private static final BibEntryType UNPUBLISHED = new BibEntryTypeBuilder()
            .withType(StandardEntryType.Unpublished)
            .withRequiredFields(StandardField.AUTHOR, StandardField.TITLE, StandardField.NOTE)
            .withImportantFields(StandardField.MONTH, StandardField.YEAR)
            .build();

    public static final List<BibEntryType> ALL = Arrays.asList(ARTICLE, INBOOK, BOOK, BOOKLET, INCOLLECTION, CONFERENCE,
            INPROCEEDINGS, PROCEEDINGS, MANUAL, MASTERSTHESIS, PHDTHESIS, TECHREPORT, UNPUBLISHED, MISC);

    public static final List<BibEntryType> RECOMMENDED = Arrays.asList(ARTICLE, BOOK, INPROCEEDINGS, TECHREPORT, MISC);

    private BibtexEntryTypeDefinitions() {
    }
}

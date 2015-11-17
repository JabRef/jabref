package net.sf.jabref.model.entry;

import net.sf.jabref.model.database.BibtexDatabase;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This class represents all supported BibTex entry types.
 *
 * Article, Book, Booklet, Conference, Inbook, Incollection, Inproceedings,
 * Manual, Mastersthesis, Misc, Phdthesis, Proceedings, Techreport, Unpublished
 */
public class BibtexEntryTypes {
    /**
     * An article from a journal or magazine.
     *
     * Required fields: author, title, journal, year.
     * Optional fields: volume, number, pages, month, note.
     */
    public static final EntryType ARTICLE =
            new BibtexEntryType() {

                {
                    addAllRequired("author", "title", "journal", "year");
                    addAllOptional("volume", "number", "pages", "month", "note");
                }

                @Override
                public String getName() {
                    return "Article";
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return entry.allFieldsPresent(new String[]{
                            "author", "title", "journal", "year", "bibtexkey"
                    }, database);
                }
            };

    /**
     * A book with an explicit publisher.
     *
     * Required fields: author or editor, title, publisher, year.
     * Optional fields: volume or number, series, address, edition, month, note.
     */
    public static final EntryType BOOK =
            new BibtexEntryType() {

                private List<String> requiredFieldsForCustomization = Collections.unmodifiableList(Arrays.asList(new String[]
                        {"title", "publisher", "year", "author/editor"}));

                {
                    addAllRequired("title", "publisher", "year", "editor", "author");
                    addAllOptional("volume", "number", "series", "address", "edition", "month",
                            "note");
                }

                @Override
                public String getName() {
                    return "Book";
                }


                @Override
                public List<String> getRequiredFieldsForCustomization() {
                    return requiredFieldsForCustomization;
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return entry.allFieldsPresent(new String[]
                            {
                                    "title", "publisher", "year", "bibtexkey"
                            }, database) &&
                            entry.atLeastOnePresent(new String[]{"author", "editor"}, database);

                }
            };

    /**
     * A work that is printed and bound, but without a named publisher or sponsoring institution.
     *
     * Required field: title.
     * Optional fields: author, howpublished, address, month, year, note.
     */
    public static final EntryType BOOKLET =
            new BibtexEntryType() {

                {
                    addAllRequired("title");
                    addAllOptional("author", "howpublished", "address", "month", "year", "note");
                }

                @Override
                public String getName() {
                    return "Booklet";
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return entry.allFieldsPresent(new String[]{"title", "bibtexkey"}, database);
                }
            };

    /**
     * An article in a conference proceedings.
     *
     * Required fields: author, title, booktitle, year.
     * Optional fields: editor, volume or number, series, pages, address, month, organization, publisher, note.
     */
    public static final EntryType CONFERENCE =
            new BibtexEntryType() {

                {
                    addAllOptional("editor", "volume", "number", "series", "pages",
                            "address", "month", "organization", "publisher", "note");
                    addAllRequired("author", "title", "booktitle", "year");
                }

                @Override
                public String getName() {
                    return "Conference";
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return entry.allFieldsPresent(new String[]
                            {
                                    "author", "title", "booktitle", "year", "bibtexkey"
                            }, database);
                }
            };

    /**
     * A part of a book, which may be a chapter (or section or whatever) and/or a range of pages.
     *
     * Required fields: author or editor, title, chapter and/or pages, publisher, year.
     * Optional fields: volume or number, series, type, address, edition, month, note.
     */
    public static final EntryType INBOOK =
            new BibtexEntryType() {

                private List<String> requiredFieldsForCustomization = Collections.unmodifiableList(Arrays.asList(new String[]{"author/editor", "title", "chapter/pages", "year", "publisher"}));

                {
                    addAllOptional("volume", "number", "series", "type", "address", "edition",
                            "month", "note");
                    addAllRequired("chapter", "pages", "title", "publisher", "year", "editor",
                            "author");
                }

                @Override
                public String getName() {
                    return "InBook";
                }

                @Override
                public List<String> getRequiredFieldsForCustomization() {
                    return requiredFieldsForCustomization;
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return entry.allFieldsPresent(new String[]
                            {
                                    "title", "publisher", "year", "bibtexkey"
                            }, database) &&
                            entry.atLeastOnePresent(new String[]{"author", "editor"}, database) &&
                            entry.atLeastOnePresent(new String[]{"chapter", "pages"}, database);
                }
            };

    /**
     * A part of a book having its own title.
     * Required fields: author, title, booktitle, publisher, year.
     * Optional fields: editor, volume or number, series, type, chapter, pages, address, edition, month, note.
     */
    public static final EntryType INCOLLECTION =
            new BibtexEntryType() {

                {
                    addAllRequired("author", "title", "booktitle", "publisher", "year");
                    addAllOptional("editor", "volume", "number", "series", "type", "chapter",
                            "pages", "address", "edition", "month", "note");
                }

                @Override
                public String getName() {
                    return "InCollection";
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return entry.allFieldsPresent(new String[]
                            {
                                    "author", "title", "booktitle", "publisher", "year",
                                    "bibtexkey"

                            }, database);
                }
            };

    /**
     * An article in a conference proceedings.
     *
     * Required fields: author, title, booktitle, year.
     * Optional fields: editor, volume or number, series, pages, address, month, organization, publisher, note.
     */
    public static final EntryType INPROCEEDINGS =
            new BibtexEntryType() {

                {
                    addAllOptional("editor", "volume", "number", "series", "pages",
                            "address", "month", "organization", "publisher", "note");
                    addAllRequired("author", "title", "booktitle", "year");
                }

                @Override
                public String getName() {
                    return "InProceedings";
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return entry.allFieldsPresent(new String[]
                            {
                                    "author", "title", "booktitle", "year", "bibtexkey"
                            }, database);
                }
            };

    /**
     * Technical documentation.
     * Required field: title.
     * Optional fields: author, organization, address, edition, month, year, note.
     */
    public static final EntryType MANUAL =
            new BibtexEntryType() {

                {
                    addAllOptional("author", "organization", "address", "edition",
                            "month", "year", "note");
                    addAllRequired("title");
                }

                @Override
                public String getName() {
                    return "Manual";
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return entry.allFieldsPresent(new String[]
                            {
                                    "title", "bibtexkey"
                            }, database);
                }
            };

    /**
     * A Master's thesis.
     *
     * Required fields: author, title, school, year.
     * Optional fields: type, address, month, note.
     */
    public static final EntryType MASTERSTHESIS =
            new BibtexEntryType() {

                {
                    addAllOptional("type", "address", "month", "note");
                    addAllRequired("author", "title", "school", "year");
                }

                @Override
                public String getName() {
                    return "MastersThesis";
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return entry.allFieldsPresent(new String[]
                            {
                                    "author", "title", "school", "year", "bibtexkey"
                            }, database);
                }
            };

    /**
     * Use this type when nothing else fits.
     *
     * Required fields: none.
     * Optional fields: author, title, howpublished, month, year, note.
     */
    public static final EntryType MISC =
            new BibtexEntryType() {

                {
                    addAllOptional("author", "title", "howpublished", "month", "year", "note");
                }

                @Override
                public String getName() {
                    return "Misc";
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return entry.allFieldsPresent(new String[]
                            {
                                    "bibtexkey"
                            }, database);
                }
            };

    /**
     * A PhD thesis.
     *
     * Required fields: author, title, school, year.
     * Optional fields: type, address, month, note.
     */
    public static final EntryType PHDTHESIS =
            new BibtexEntryType() {

                {
                    addAllOptional("type", "address", "month", "note");
                    addAllRequired("author", "title", "school", "year");
                }

                @Override
                public String getName() {
                    return "PhdThesis";
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return entry.allFieldsPresent(new String[]
                            {
                                    "author", "title", "school", "year", "bibtexkey"
                            }, database);
                }
            };

    /**
     * The proceedings of a conference.
     *
     * Required fields: title, year.
     * Optional fields: editor, volume or number, series, address, month, organization, publisher, note.
     */
    public static final EntryType PROCEEDINGS =
            new BibtexEntryType() {

                {
                    addAllOptional("editor", "volume", "number", "series", "address",
                            "publisher", "note", "month", "organization");
                    addAllRequired("title", "year");
                }

                @Override
                public String getName() {
                    return "Proceedings";
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return entry.allFieldsPresent(new String[]
                            {
                                    "title", "year", "bibtexkey"
                            }, database);
                }
            };

    /**
     * A report published by a school or other institution, usually numbered within a series.
     *
     * Required fields: author, title, institution, year.
     * Optional fields: type, number, address, month, note.
     */
    public static final EntryType TECHREPORT =
            new BibtexEntryType() {

                {
                    addAllOptional("type", "number", "address", "month", "note");
                    addAllRequired("author", "title", "institution", "year");
                }

                @Override
                public String getName() {
                    return "TechReport";
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return entry.allFieldsPresent(new String[]
                            {
                                    "author", "title", "institution", "year",
                                    "bibtexkey"
                            }, database);
                }
            };

    /**
     * A document having an author and title, but not formally published.
     *
     * Required fields: author, title, note.
     * Optional fields: month, year.
     */
    public static final EntryType UNPUBLISHED =
            new BibtexEntryType() {

                {
                    addAllOptional("month", "year");
                    addAllRequired("author", "title", "note");
                }

                @Override
                public String getName() {
                    return "Unpublished";
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return entry.allFieldsPresent(new String[]
                            {
                                    "author", "title", "note", "bibtexkey"
                            }, database);
                }
            };


    /**
     * TODO: internal type
     */
    public static final EntryType OTHER =
            new BibtexEntryType() {

                @Override
                public String getName() {
                    return "Other";
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return true;
                }
            };

    /**
     * This type is provided as an emergency choice if the user makes
     * customization changes that remove the type of an entry.
     *
     * TODO: internal type, merge with @Other?
     */
    public static final EntryType TYPELESS =
            new BibtexEntryType() {

                @Override
                public String getName() {
                    return "Typeless";
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return false;
                }
            };
}

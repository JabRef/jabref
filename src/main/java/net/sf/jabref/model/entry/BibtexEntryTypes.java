package net.sf.jabref.model.entry;

import net.sf.jabref.model.database.BibtexDatabase;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BibtexEntryTypes {

    public static final BibtexEntryType OTHER =
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
     * An article from a journal or magazine.
     *
     * Required fields: author, title, journal, year.
     * Optional fields: volume, number, pages, month, note.
     */
    public static final BibtexEntryType ARTICLE =
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
     * A work that is printed and bound, but without a named publisher or sponsoring institution.
     *
     * Required field: title.
     * Optional fields: author, howpublished, address, month, year, note.
     */
    public static final BibtexEntryType BOOKLET =
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
     * A part of a book, which may be a chapter (or section or whatever) and/or a range of pages.
     *
     * Required fields: author or editor, title, chapter and/or pages, publisher, year.
     * Optional fields: volume or number, series, type, address, edition, month, note.
     */
    public static final BibtexEntryType INBOOK =
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
     * A book with an explicit publisher.
     *
     * Required fields: author or editor, title, publisher, year.
     * Optional fields: volume or number, series, address, edition, month, note.
     */
    public static final BibtexEntryType BOOK =
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
     * A part of a book having its own title.
     * Required fields: author, title, booktitle, publisher, year.
     * Optional fields: editor, volume or number, series, type, chapter, pages, address, edition, month, note.
     */
    public static final BibtexEntryType INCOLLECTION =
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
    public static final BibtexEntryType CONFERENCE =
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
     * An article in a conference proceedings.
     *
     * Required fields: author, title, booktitle, year.
     * Optional fields: editor, volume or number, series, pages, address, month, organization, publisher, note.
     */
    public static final BibtexEntryType INPROCEEDINGS =
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
     * The proceedings of a conference.
     *
     * Required fields: title, year.
     * Optional fields: editor, volume or number, series, address, month, organization, publisher, note.
     */
    public static final BibtexEntryType PROCEEDINGS =
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
     * Technical documentation.
     * Required field: title.
     * Optional fields: author, organization, address, edition, month, year, note.
     */
    public static final BibtexEntryType MANUAL =
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
     * A report published by a school or other institution, usually numbered within a series.
     *
     * Required fields: author, title, institution, year.
     * Optional fields: type, number, address, month, note.
     */
    public static final BibtexEntryType TECHREPORT =
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
     * A Master's thesis.
     *
     * Required fields: author, title, school, year.
     * Optional fields: type, address, month, note.
     */
    public static final BibtexEntryType MASTERSTHESIS =
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
     * A PhD thesis.
     *
     * Required fields: author, title, school, year.
     * Optional fields: type, address, month, note.
     */
    public static final BibtexEntryType PHDTHESIS =
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
     * A document having an author and title, but not formally published.
     *
     * Required fields: author, title, note.
     * Optional fields: month, year.
     */
    public static final BibtexEntryType UNPUBLISHED =
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
     * TODO: unknown
     */
    public static final BibtexEntryType PERIODICAL =
            new BibtexEntryType() {

                {
                    addAllOptional("editor", "language", "series", "volume", "number", "organization", "month", "note", "url");
                    addAllRequired("title", "year");
                }

                @Override
                public String getName() {
                    return "Periodical";
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
     * TODO: unknown
     */
    public static final BibtexEntryType PATENT =
            new BibtexEntryType() {

                {
                    addAllOptional("author", "title", "language", "assignee", "address", "type", "number", "day", "dayfiled", "month", "monthfiled", "note", "url");
                    addAllRequired("nationality", "number", "year", "yearfiled");
                }

                @Override
                public String getName() {
                    return "Patent";
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return entry.allFieldsPresent(new String[]
                            {
                                    "number", "bibtexkey"
                            }, database) &&
                            entry.atLeastOnePresent(new String[]{"year", "yearfiled"}, database);

                }
            };

    /**
     * TODO: unknown
     */
    public static final BibtexEntryType STANDARD =
            new BibtexEntryType() {

                private List<String> requiredFieldsForCustomization = Collections.unmodifiableList(Arrays.asList(new String[]{"title", "organization/institution"}));

                {
                    addAllOptional("author", "language", "howpublished", "type", "number", "revision", "address", "month", "year", "note", "url");
                    addAllRequired("title", "organization", "institution");
                }

                @Override
                public String getName() {
                    return "Standard";
                }


                @Override
                public List<String> getRequiredFieldsForCustomization() {
                    return requiredFieldsForCustomization;
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return entry.allFieldsPresent(new String[]
                            {
                                    "title", "bibtexkey"
                            }, database) &&
                            entry.atLeastOnePresent(new String[]{"organization", "institution"}, database);

                }
            };

    /**
     * TODO: unknown
     */
    public static final BibtexEntryType ELECTRONIC =
            new BibtexEntryType() {

                {
                    addAllOptional("author", "month", "year", "title", "language", "howpublished", "organization", "address", "note", "url");

                }

                @Override
                public String getName() {
                    return "Electronic";
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
     * Use this type when nothing else fits.
     *
     * Required fields: none.
     * Optional fields: author, title, howpublished, month, year, note.
     */
    public static final BibtexEntryType MISC =
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
     * This type is used for IEEEtran.bst to control various
     * be repeated or not. Not a very elegant solution, but it works...
     */
    public static final BibtexEntryType IEEETRANBSTCTL =
            new BibtexEntryType() {

                {
                    addAllOptional("ctluse_article_number", "ctluse_paper", "ctluse_forced_etal",
                            "ctlmax_names_forced_etal", "ctlnames_show_etal", "ctluse_alt_spacing",
                            "ctlalt_stretch_factor", "ctldash_repeated_names", "ctlname_format_string",
                            "ctlname_latex_cmd", "ctlname_url_prefix");
                }

                @Override
                public String getName() {
                    return "IEEEtranBSTCTL";
                }

                @Override
                public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
                    return true;
                }

                @Override
                public boolean isVisibleAtNewEntryDialog() {
                    return false;
                }
            };

    /**
     * This type is provided as an emergency choice if the user makes
     * customization changes that remove the type of an entry.
     */
    public static final BibtexEntryType TYPELESS =
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

package net.sf.jabref.model.entry;

import net.sf.jabref.model.database.BibtexDatabase;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This class represents all supported IEEETran entry types.
 * @see http://ctan.sharelatex.com/tex-archive/macros/latex/contrib/IEEEtran/bibtex/IEEEtran_bst_HOWTO.pdf
 *
 * Electronic, IEEETranBSTCTL, Periodical, Patent, Standard
 */
public class IEEETranEntryTypes {
    /**
     * Electronic entry type for internet references
     *
     * Required fields:
     * Optional fields: author, month, year, title, language, howpublished, organization, address, note, url
     */
    public static final EntryType ELECTRONIC =
            new BibtexEntryType() {

                {
                    addAllOptional("author", "month", "year", "title", "language", "howpublished", "organization", "address", "note", "url");

                }

                @Override
                public String getName() {
                    return "Electronic";
                }
            };

    /**
     * Special entry type that can be used to externally control some aspects of the bibliography style.
     */
    public static final EntryType IEEETRANBSTCTL =
            new BibtexEntryType() {

                {
                    addAllOptional("ctluse_article_number", "ctluse_paper", "ctluse_forced_etal", "ctluse_url",
                            "ctlmax_names_forced_etal", "ctlnames_show_etal", "ctluse_alt_spacing",
                            "ctlalt_stretch_factor", "ctldash_repeated_names", "ctlname_format_string",
                            "ctlname_latex_cmd", "ctlname_url_prefix");
                }

                @Override
                public String getName() {
                    return "IEEEtranBSTCTL";
                }
            };

    /**
     * The periodical entry type is used for journals and magazines.
     *
     * Required fields: title, year
     * Optional fields: editor, language, series, volume, number, organization, month, note, url
     */
    public static final EntryType PERIODICAL =
            new BibtexEntryType() {

                {
                    addAllOptional("editor", "language", "series", "volume", "number", "organization", "month", "note", "url");
                    addAllRequired("title", "year");
                }

                @Override
                public String getName() {
                    return "Periodical";
                }
            };

    /**
     * Entry type for patents.
     *
     * Required fields: nationality, number, year or yearfiled
     * Optional fields: author, title, language, assignee, address, type, number, day, dayfiled, month, monthfiled, note, url
     */
    public static final EntryType PATENT =
            new BibtexEntryType() {

                {
                    addAllOptional("author", "title", "language", "assignee", "address", "type", "number", "day", "dayfiled", "month", "monthfiled", "note", "url");
                    addAllRequired("nationality", "number", "year/yearfiled");
                }

                @Override
                public String getName() {
                    return "Patent";
                }
            };

    /**
     * The standard entry type is used for proposed or formally published standards.
     *
     * Required fields: title, organization or institution
     * Optional fields: author, language, howpublished, type, number, revision, address, month, year, note, url
     */
    public static final EntryType STANDARD =
            new BibtexEntryType() {

                private List<String> requiredFieldsForCustomization = Collections.unmodifiableList(Arrays.asList(new String[]{"title", "organization/institution"}));

                {
                    addAllOptional("author", "language", "howpublished", "type", "number", "revision", "address", "month", "year", "note", "url");
                    addAllRequired("title", "organization/institution");
                }

                @Override
                public String getName() {
                    return "Standard";
                }


                @Override
                public List<String> getRequiredFieldsForCustomization() {
                    return requiredFieldsForCustomization;
                }
            };

    public static final List<EntryType> ALL = Arrays.asList(ELECTRONIC, IEEETRANBSTCTL, PERIODICAL, PATENT, STANDARD);
}

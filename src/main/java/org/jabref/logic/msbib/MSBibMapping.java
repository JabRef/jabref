package org.jabref.logic.msbib;

import java.util.HashMap;
import java.util.Map;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.IEEETranEntryType;
import org.jabref.model.entry.types.StandardEntryType;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * Mapping between Msbib and biblatex All Fields: <a href = "https://msdn.microsoft.com/de-de/library/office/documentformat.openxml.bibliography">List
 * of all MSBib fields</a>
 */
public class MSBibMapping {

    private static final String BIBTEX_PREFIX = "BIBTEX_";
    private static final String MSBIB_PREFIX = "msbib-";

    private static final BiMap<Field, String> BIBLATEX_TO_MS_BIB = HashBiMap.create();

    // see https://learn.microsoft.com/en-us/openspecs/office_standards/ms-oe376/6c085406-a698-4e12-9d4d-c3b0ee3dbc4a
    private static final BiMap<String, Integer> LANG_TO_LCID = HashBiMap.create();

    static {
        LANG_TO_LCID.put("basque", 1609);
        LANG_TO_LCID.put("bulgarian", 1026);
        LANG_TO_LCID.put("catalan", 1027);
        LANG_TO_LCID.put("croatian", 1050);
        LANG_TO_LCID.put("czech", 1029);
        LANG_TO_LCID.put("danish", 1030);
        LANG_TO_LCID.put("dutch", 1043);
        LANG_TO_LCID.put("english", 1033); // american english
        LANG_TO_LCID.put("finnish", 1035);
        LANG_TO_LCID.put("french", 1036);
        LANG_TO_LCID.put("german", 1031);
        LANG_TO_LCID.put("austrian", 3079);
        LANG_TO_LCID.put("swissgerman", 2055);
        LANG_TO_LCID.put("greek", 1032);
        LANG_TO_LCID.put("hungarian", 1038);
        LANG_TO_LCID.put("icelandic", 1039);
        LANG_TO_LCID.put("italian", 1040);
        LANG_TO_LCID.put("latvian", 1062);
        LANG_TO_LCID.put("lithuanian", 1063);
        LANG_TO_LCID.put("marathi", 1102);
        LANG_TO_LCID.put("nynorsk", 2068);
        LANG_TO_LCID.put("polish", 1045);
        LANG_TO_LCID.put("brazil", 1046);
        LANG_TO_LCID.put("portuguese", 2070);
        LANG_TO_LCID.put("romanian", 1048);
        LANG_TO_LCID.put("russian", 1049);
        LANG_TO_LCID.put("serbian", 2074);
        LANG_TO_LCID.put("serbianc", 3098);
        LANG_TO_LCID.put("slovak", 1051);
        LANG_TO_LCID.put("slovene", 1060);
        LANG_TO_LCID.put("spanish", 3082);
        LANG_TO_LCID.put("swedish", 1053);
        LANG_TO_LCID.put("turkish", 1055);
        LANG_TO_LCID.put("ukrainian", 1058);
    }

    static {
        BIBLATEX_TO_MS_BIB.put(InternalField.KEY_FIELD, "Tag");
        BIBLATEX_TO_MS_BIB.put(StandardField.TITLE, "Title");
        BIBLATEX_TO_MS_BIB.put(StandardField.YEAR, "Year");
        BIBLATEX_TO_MS_BIB.put(StandardField.VOLUME, "Volume");
        BIBLATEX_TO_MS_BIB.put(StandardField.LANGUAGE, "LCID");
        BIBLATEX_TO_MS_BIB.put(StandardField.EDITION, "Edition");
        BIBLATEX_TO_MS_BIB.put(StandardField.PUBLISHER, "Publisher");
        BIBLATEX_TO_MS_BIB.put(StandardField.BOOKTITLE, "BookTitle");
        BIBLATEX_TO_MS_BIB.put(StandardField.SHORTTITLE, "ShortTitle");
        BIBLATEX_TO_MS_BIB.put(StandardField.NOTE, "Comments");
        BIBLATEX_TO_MS_BIB.put(StandardField.VOLUMES, "NumberVolumes");

        BIBLATEX_TO_MS_BIB.put(StandardField.CHAPTER, "ChapterNumber");

        BIBLATEX_TO_MS_BIB.put(StandardField.ISSUE, "Issue");
        BIBLATEX_TO_MS_BIB.put(StandardField.SCHOOL, "Department");
        BIBLATEX_TO_MS_BIB.put(StandardField.INSTITUTION, "Institution");
        BIBLATEX_TO_MS_BIB.put(StandardField.DOI, "DOI");
        BIBLATEX_TO_MS_BIB.put(StandardField.URL, "URL");
        // BibTeX/Biblatex only fields

        BIBLATEX_TO_MS_BIB.put(StandardField.SERIES, BIBTEX_PREFIX + "Series");
        BIBLATEX_TO_MS_BIB.put(StandardField.ABSTRACT, BIBTEX_PREFIX + "Abstract");
        BIBLATEX_TO_MS_BIB.put(StandardField.KEYWORDS, BIBTEX_PREFIX + "KeyWords");
        BIBLATEX_TO_MS_BIB.put(StandardField.CROSSREF, BIBTEX_PREFIX + "CrossRef");
        BIBLATEX_TO_MS_BIB.put(StandardField.HOWPUBLISHED, BIBTEX_PREFIX + "HowPublished");
        BIBLATEX_TO_MS_BIB.put(StandardField.PUBSTATE, BIBTEX_PREFIX + "Pubstate");
        BIBLATEX_TO_MS_BIB.put(new UnknownField("affiliation"), BIBTEX_PREFIX + "Affiliation");
        BIBLATEX_TO_MS_BIB.put(new UnknownField("contents"), BIBTEX_PREFIX + "Contents");
        BIBLATEX_TO_MS_BIB.put(new UnknownField("copyright"), BIBTEX_PREFIX + "Copyright");
        BIBLATEX_TO_MS_BIB.put(new UnknownField("price"), BIBTEX_PREFIX + "Price");
        BIBLATEX_TO_MS_BIB.put(new UnknownField("size"), BIBTEX_PREFIX + "Size");
        BIBLATEX_TO_MS_BIB.put(new UnknownField("intype"), BIBTEX_PREFIX + "InType");
        BIBLATEX_TO_MS_BIB.put(new UnknownField("paper"), BIBTEX_PREFIX + "Paper");
        BIBLATEX_TO_MS_BIB.put(StandardField.KEY, BIBTEX_PREFIX + "Key");

        // MSBib only fields
        BIBLATEX_TO_MS_BIB.put(new UnknownField(MSBIB_PREFIX + "periodical"), "PeriodicalTitle");
        BIBLATEX_TO_MS_BIB.put(new UnknownField(MSBIB_PREFIX + StandardField.DAY), "Day");
        BIBLATEX_TO_MS_BIB.put(new UnknownField(MSBIB_PREFIX + "accessed"), "Accessed");
        BIBLATEX_TO_MS_BIB.put(new UnknownField(MSBIB_PREFIX + "medium"), "Medium");
        BIBLATEX_TO_MS_BIB.put(new UnknownField(MSBIB_PREFIX + "recordingnumber"), "RecordingNumber");
        BIBLATEX_TO_MS_BIB.put(new UnknownField(MSBIB_PREFIX + "theater"), "Theater");
        BIBLATEX_TO_MS_BIB.put(new UnknownField(MSBIB_PREFIX + "distributor"), "Distributor");
        BIBLATEX_TO_MS_BIB.put(new UnknownField(MSBIB_PREFIX + "broadcaster"), "Broadcaster");
        BIBLATEX_TO_MS_BIB.put(new UnknownField(MSBIB_PREFIX + "station"), "Station");
        BIBLATEX_TO_MS_BIB.put(new UnknownField(MSBIB_PREFIX + StandardField.TYPE), "Type");
        BIBLATEX_TO_MS_BIB.put(new UnknownField(MSBIB_PREFIX + "court"), "Court");
        BIBLATEX_TO_MS_BIB.put(new UnknownField(MSBIB_PREFIX + "reporter"), "Reporter");
        BIBLATEX_TO_MS_BIB.put(new UnknownField(MSBIB_PREFIX + "casenumber"), "CaseNumber");
        BIBLATEX_TO_MS_BIB.put(new UnknownField(MSBIB_PREFIX + "abbreviatedcasenumber"), "AbbreviatedCaseNumber");
        BIBLATEX_TO_MS_BIB.put(new UnknownField(MSBIB_PREFIX + "productioncompany"), "ProductionCompany");
    }

    private MSBibMapping() {
    }

    public static EntryType getBiblatexEntryType(String msbibType) {
        Map<String, EntryType> entryTypeMapping = new HashMap<>();

        entryTypeMapping.put("Book", StandardEntryType.Book);
        entryTypeMapping.put("BookSection", StandardEntryType.Book);
        entryTypeMapping.put("JournalArticle", StandardEntryType.Article);
        entryTypeMapping.put("ArticleInAPeriodical", IEEETranEntryType.Periodical);
        entryTypeMapping.put("ConferenceProceedings", StandardEntryType.InProceedings);
        entryTypeMapping.put("Report", StandardEntryType.TechReport);
        entryTypeMapping.put("Patent", IEEETranEntryType.Patent);
        entryTypeMapping.put("InternetSite", StandardEntryType.Online);

        return entryTypeMapping.getOrDefault(msbibType, StandardEntryType.Misc);
    }

    public static MSBibEntryType getMSBibEntryType(EntryType bibtexType) {
        Map<EntryType, MSBibEntryType> entryTypeMapping = new HashMap<>();

        entryTypeMapping.put(StandardEntryType.Book, MSBibEntryType.Book);
        entryTypeMapping.put(StandardEntryType.InBook, MSBibEntryType.BookSection);
        entryTypeMapping.put(StandardEntryType.Booklet, MSBibEntryType.BookSection);
        entryTypeMapping.put(StandardEntryType.InCollection, MSBibEntryType.BookSection);
        entryTypeMapping.put(StandardEntryType.Article, MSBibEntryType.JournalArticle);
        entryTypeMapping.put(StandardEntryType.InProceedings, MSBibEntryType.ConferenceProceedings);
        entryTypeMapping.put(StandardEntryType.Conference, MSBibEntryType.ConferenceProceedings);
        entryTypeMapping.put(StandardEntryType.Proceedings, MSBibEntryType.ConferenceProceedings);
        entryTypeMapping.put(StandardEntryType.Collection, MSBibEntryType.ConferenceProceedings);
        entryTypeMapping.put(StandardEntryType.TechReport, MSBibEntryType.Report);
        entryTypeMapping.put(StandardEntryType.Manual, MSBibEntryType.Report);
        entryTypeMapping.put(StandardEntryType.MastersThesis, MSBibEntryType.Report);
        entryTypeMapping.put(StandardEntryType.PhdThesis, MSBibEntryType.Report);
        entryTypeMapping.put(StandardEntryType.Unpublished, MSBibEntryType.Report);
        entryTypeMapping.put(IEEETranEntryType.Patent, MSBibEntryType.Patent);
        entryTypeMapping.put(StandardEntryType.Misc, MSBibEntryType.Misc);
        entryTypeMapping.put(IEEETranEntryType.Electronic, MSBibEntryType.ElectronicSource);
        entryTypeMapping.put(StandardEntryType.Online, MSBibEntryType.InternetSite);

        return entryTypeMapping.getOrDefault(bibtexType, MSBibEntryType.Misc);
    }

    /**
     * <a href="http://www.microsoft.com/globaldev/reference/lcid-all.mspx">All LCID codes</a>
     *
     * @param language The language to transform
     * @return 1033 (american english) as default. LCID otherwise.
     */
    public static int getLCID(String language) {
        return LANG_TO_LCID.getOrDefault(language, 1033);
    }

    /**
     * <a href="http://www.microsoft.com/globaldev/reference/lcid-all.mspx">All LCID codes</a>
     *
     * @param  LCID The LCID to transform
     * @return "english" as default. Corresponding language from BiMap otherwise.
     */
    public static String getLanguage(int LCID) {
        return LANG_TO_LCID.inverse().getOrDefault(LCID, "english");
    }

    public static String getMSBibField(Field field) {
        return BIBLATEX_TO_MS_BIB.get(field);
    }

    public static Field getBibTeXField(String msbibFieldName) {
        return BIBLATEX_TO_MS_BIB.inverse().get(msbibFieldName);
    }
}

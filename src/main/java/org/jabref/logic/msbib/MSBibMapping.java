package org.jabref.logic.msbib;

import java.util.Map;
import java.util.TreeMap;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BiblatexEntryTypes;
import org.jabref.model.entry.BibtexEntryTypes;
import org.jabref.model.entry.FieldName;

import com.google.common.collect.HashBiMap;

/**
 * Mapping between Msbib and biblatex All Fields: <a href = "https://msdn.microsoft.com/de-de/library/office/documentformat.openxml.bibliography">List
 * of all MSBib fields</a>
 */
public class MSBibMapping {

    private static final String BIBTEX_PREFIX = "BIBTEX_";
    private static final String MSBIB_PREFIX = "msbib-";

    private static final HashBiMap<String, String> BIBLATEX_TO_MS_BIB = HashBiMap.create();

    static {
        BIBLATEX_TO_MS_BIB.put(BibEntry.KEY_FIELD, "Tag");
        BIBLATEX_TO_MS_BIB.put(FieldName.TITLE, "Title");
        BIBLATEX_TO_MS_BIB.put(FieldName.YEAR, "Year");
        BIBLATEX_TO_MS_BIB.put(FieldName.NOTE, "Comments");
        BIBLATEX_TO_MS_BIB.put(FieldName.VOLUME, "Volume");
        BIBLATEX_TO_MS_BIB.put(FieldName.LANGUAGE, "LCID");
        BIBLATEX_TO_MS_BIB.put(FieldName.EDITION, "Edition");
        BIBLATEX_TO_MS_BIB.put(FieldName.PUBLISHER, "Publisher");
        BIBLATEX_TO_MS_BIB.put(FieldName.BOOKTITLE, "BookTitle");
        BIBLATEX_TO_MS_BIB.put("shorttitle", "ShortTitle");
        BIBLATEX_TO_MS_BIB.put(FieldName.NOTE, "Comments");
        BIBLATEX_TO_MS_BIB.put(FieldName.VOLUMES, "NumberVolumes");

        BIBLATEX_TO_MS_BIB.put(FieldName.CHAPTER, "ChapterNumber");

        BIBLATEX_TO_MS_BIB.put(FieldName.ISSUE, "Issue");
        BIBLATEX_TO_MS_BIB.put(FieldName.SCHOOL, "Department");
        BIBLATEX_TO_MS_BIB.put(FieldName.INSTITUTION, "Institution");
        BIBLATEX_TO_MS_BIB.put(FieldName.DOI, "DOI");
        BIBLATEX_TO_MS_BIB.put(FieldName.URL, "URL");
        // BibTeX/Biblatex only fields

        BIBLATEX_TO_MS_BIB.put(FieldName.SERIES, BIBTEX_PREFIX + "Series");
        BIBLATEX_TO_MS_BIB.put(FieldName.ABSTRACT, BIBTEX_PREFIX + "Abstract");
        BIBLATEX_TO_MS_BIB.put(FieldName.KEYWORDS, BIBTEX_PREFIX + "KeyWords");
        BIBLATEX_TO_MS_BIB.put(FieldName.CROSSREF, BIBTEX_PREFIX + "CrossRef");
        BIBLATEX_TO_MS_BIB.put(FieldName.HOWPUBLISHED, BIBTEX_PREFIX + "HowPublished");
        BIBLATEX_TO_MS_BIB.put(FieldName.PUBSTATE, BIBTEX_PREFIX + "Pubstate");
        BIBLATEX_TO_MS_BIB.put("affiliation", BIBTEX_PREFIX + "Affiliation");
        BIBLATEX_TO_MS_BIB.put("contents", BIBTEX_PREFIX + "Contents");
        BIBLATEX_TO_MS_BIB.put("copyright", BIBTEX_PREFIX + "Copyright");
        BIBLATEX_TO_MS_BIB.put("price", BIBTEX_PREFIX + "Price");
        BIBLATEX_TO_MS_BIB.put("size", BIBTEX_PREFIX + "Size");
        BIBLATEX_TO_MS_BIB.put("intype", BIBTEX_PREFIX + "InType");
        BIBLATEX_TO_MS_BIB.put("paper", BIBTEX_PREFIX + "Paper");
        BIBLATEX_TO_MS_BIB.put(FieldName.KEY, BIBTEX_PREFIX + "Key");

        // MSBib only fields
        BIBLATEX_TO_MS_BIB.put(MSBIB_PREFIX + "periodical", "PeriodicalTitle");
        BIBLATEX_TO_MS_BIB.put(MSBIB_PREFIX + FieldName.DAY, "Day");
        BIBLATEX_TO_MS_BIB.put(MSBIB_PREFIX + "accessed", "Accessed");
        BIBLATEX_TO_MS_BIB.put(MSBIB_PREFIX + "medium", "Medium");
        BIBLATEX_TO_MS_BIB.put(MSBIB_PREFIX + "recordingnumber", "RecordingNumber");
        BIBLATEX_TO_MS_BIB.put(MSBIB_PREFIX + "theater", "Theater");
        BIBLATEX_TO_MS_BIB.put(MSBIB_PREFIX + "distributor", "Distributor");
        BIBLATEX_TO_MS_BIB.put(MSBIB_PREFIX + "broadcaster", "Broadcaster");
        BIBLATEX_TO_MS_BIB.put(MSBIB_PREFIX + "station", "Station");
        BIBLATEX_TO_MS_BIB.put(MSBIB_PREFIX + FieldName.TYPE, "Type");
        BIBLATEX_TO_MS_BIB.put(MSBIB_PREFIX + "court", "Court");
        BIBLATEX_TO_MS_BIB.put(MSBIB_PREFIX + "reporter", "Reporter");
        BIBLATEX_TO_MS_BIB.put(MSBIB_PREFIX + "casenumber", "CaseNumber");
        BIBLATEX_TO_MS_BIB.put(MSBIB_PREFIX + "abbreviatedcasenumber", "AbbreviatedCaseNumber");
        BIBLATEX_TO_MS_BIB.put(MSBIB_PREFIX + "productioncompany", "ProductionCompany");
    }

    private MSBibMapping() {
    }

    public static String getBiblatexEntryType(String msbibType) {
        final String defaultType = BibtexEntryTypes.MISC.getName();

        Map<String, String> entryTypeMapping = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        entryTypeMapping.put("Book", BiblatexEntryTypes.BOOK.getName());
        entryTypeMapping.put("BookSection", BiblatexEntryTypes.BOOK.getName());
        entryTypeMapping.put("JournalArticle", BiblatexEntryTypes.ARTICLE.getName());
        entryTypeMapping.put("ArticleInAPeriodical", BiblatexEntryTypes.PERIODICAL.getName());
        entryTypeMapping.put("ConferenceProceedings", BiblatexEntryTypes.INPROCEEDINGS.getName());
        entryTypeMapping.put("Report", BiblatexEntryTypes.TECHREPORT.getName());
        entryTypeMapping.put("Patent", BiblatexEntryTypes.PATENT.getName());
        entryTypeMapping.put("InternetSite", BiblatexEntryTypes.ONLINE.getName());

        return entryTypeMapping.getOrDefault(msbibType, defaultType);
    }

    public static MSBibEntryType getMSBibEntryType(String bibtexType) {
        Map<String, MSBibEntryType> entryTypeMapping = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        entryTypeMapping.put("book", MSBibEntryType.Book);
        entryTypeMapping.put("inbook", MSBibEntryType.BookSection);
        entryTypeMapping.put("booklet", MSBibEntryType.BookSection);
        entryTypeMapping.put("incollection", MSBibEntryType.BookSection);
        entryTypeMapping.put("article", MSBibEntryType.JournalArticle);
        entryTypeMapping.put("inproceedings", MSBibEntryType.ConferenceProceedings);
        entryTypeMapping.put("conference", MSBibEntryType.ConferenceProceedings);
        entryTypeMapping.put("proceedings", MSBibEntryType.ConferenceProceedings);
        entryTypeMapping.put("collection", MSBibEntryType.ConferenceProceedings);
        entryTypeMapping.put("techreport", MSBibEntryType.Report);
        entryTypeMapping.put("manual", MSBibEntryType.Report);
        entryTypeMapping.put("mastersthesis", MSBibEntryType.Report);
        entryTypeMapping.put("phdthesis", MSBibEntryType.Report);
        entryTypeMapping.put("unpublished", MSBibEntryType.Report);
        entryTypeMapping.put("patent", MSBibEntryType.Patent);
        entryTypeMapping.put("misc", MSBibEntryType.Misc);
        entryTypeMapping.put("electronic", MSBibEntryType.ElectronicSource);
        entryTypeMapping.put("online", MSBibEntryType.InternetSite);

        return entryTypeMapping.getOrDefault(bibtexType, MSBibEntryType.Misc);
    }

    /**
     * Only English is supported <br>
     * <a href="http://www.microsoft.com/globaldev/reference/lcid-all.mspx">All LCID codes</a>
     *
     * @param language The language to transform
     * @return Returns 0 for English
     */
    public static int getLCID(String language) {
        // TODO: add language to LCID mapping
        // 0x0409 is American English
        return 0x0409;
    }

    /**
     * Only English is supported <br>
     * <a href="http://www.microsoft.com/globaldev/reference/lcid-all.mspx">All LCID codes</a>
     *
     * @return Returns english
     */
    public static String getLanguage(int LCID) {
        // TODO: add language to LCID mapping
        return "english";
    }

    public static String getMSBibField(String bibtexFieldName) {
        return BIBLATEX_TO_MS_BIB.get(bibtexFieldName);
    }

    public static String getBibTeXField(String msbibFieldName) {
        return BIBLATEX_TO_MS_BIB.inverse().get(msbibFieldName);
    }
}

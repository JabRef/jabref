package org.jabref.logic.msbib;

import java.util.Map;
import java.util.TreeMap;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BiblatexEntryTypes;
import org.jabref.model.entry.BibtexEntryTypes;
import org.jabref.model.entry.FieldName;

import com.google.common.collect.HashBiMap;

/**
 * Mapping between Msbib and biblatex
 * All Fields: <a href = "https://msdn.microsoft.com/de-de/library/office/documentformat.openxml.bibliography">List of all MSBib fields</a>
 *
 */
public class MSBibMapping {

    private static final String BIBTEX_PREFIX = "BIBTEX_";
    private static final String MSBIB_PREFIX = "msbib-";

    private static final HashBiMap<String, String> biblatexToMsBib = HashBiMap.create();

    static {
        biblatexToMsBib.put(BibEntry.KEY_FIELD, "Tag");
        biblatexToMsBib.put(FieldName.TITLE, "Title");
        biblatexToMsBib.put(FieldName.YEAR, "Year");
        biblatexToMsBib.put(FieldName.NOTE, "Comments");
        biblatexToMsBib.put(FieldName.VOLUME, "Volume");
        biblatexToMsBib.put(FieldName.LANGUAGE, "LCID");
        biblatexToMsBib.put(FieldName.EDITION, "Edition");
        biblatexToMsBib.put(FieldName.PUBLISHER, "Publisher");
        biblatexToMsBib.put(FieldName.BOOKTITLE, "BookTitle");
        biblatexToMsBib.put("shorttitle", "ShortTitle");
        biblatexToMsBib.put(FieldName.NOTE, "Comments");
        biblatexToMsBib.put(FieldName.VOLUMES, "NumberVolumes");

        //biblatexToMsBib.put(FieldName.BOOKTITLE, "ConferenceName");
        //biblatexToMsBib.put(FieldName.PAGES, "Pages");
        biblatexToMsBib.put(FieldName.CHAPTER, "ChapterNumber");

        biblatexToMsBib.put(FieldName.ISSUE, "Issue");
        biblatexToMsBib.put(FieldName.SCHOOL, "Department");
        biblatexToMsBib.put(FieldName.INSTITUTION, "Institution");
        biblatexToMsBib.put(FieldName.DOI, "DOI");
        biblatexToMsBib.put(FieldName.URL, "URL");
        // BibTeX/Biblatex only fields

        biblatexToMsBib.put(FieldName.SERIES, BIBTEX_PREFIX + "Series");
        biblatexToMsBib.put(FieldName.ABSTRACT, BIBTEX_PREFIX + "Abstract");
        biblatexToMsBib.put(FieldName.KEYWORDS, BIBTEX_PREFIX + "KeyWords");
        biblatexToMsBib.put(FieldName.CROSSREF, BIBTEX_PREFIX + "CrossRef");
        biblatexToMsBib.put(FieldName.HOWPUBLISHED, BIBTEX_PREFIX + "HowPublished");
        biblatexToMsBib.put(FieldName.PUBSTATE, BIBTEX_PREFIX + "Pubstate");
        biblatexToMsBib.put("affiliation", BIBTEX_PREFIX + "Affiliation");
        biblatexToMsBib.put("contents", BIBTEX_PREFIX + "Contents");
        biblatexToMsBib.put("copyright", BIBTEX_PREFIX + "Copyright");
        biblatexToMsBib.put("price", BIBTEX_PREFIX + "Price");
        biblatexToMsBib.put("size", BIBTEX_PREFIX + "Size");
        biblatexToMsBib.put("intype", BIBTEX_PREFIX + "InType");
        biblatexToMsBib.put("paper", BIBTEX_PREFIX + "Paper");
        biblatexToMsBib.put(FieldName.KEY, BIBTEX_PREFIX + "Key");

        // MSBib only fields
        biblatexToMsBib.put(MSBIB_PREFIX + "periodical", "PeriodicalTitle");
        biblatexToMsBib.put(MSBIB_PREFIX + FieldName.DAY, "Day");
        biblatexToMsBib.put(MSBIB_PREFIX + "accessed", "Accessed");
        biblatexToMsBib.put(MSBIB_PREFIX + "medium", "Medium");
        biblatexToMsBib.put(MSBIB_PREFIX + "recordingnumber", "RecordingNumber");
        biblatexToMsBib.put(MSBIB_PREFIX + "theater", "Theater");
        biblatexToMsBib.put(MSBIB_PREFIX + "distributor", "Distributor");
        biblatexToMsBib.put(MSBIB_PREFIX + "broadcaster", "Broadcaster");
        biblatexToMsBib.put(MSBIB_PREFIX + "station", "Station");
        biblatexToMsBib.put(MSBIB_PREFIX + FieldName.TYPE, "Type");
        biblatexToMsBib.put(MSBIB_PREFIX + "court", "Court");
        biblatexToMsBib.put(MSBIB_PREFIX + "reporter", "Reporter");
        biblatexToMsBib.put(MSBIB_PREFIX + "casenumber", "CaseNumber");
        biblatexToMsBib.put(MSBIB_PREFIX + "abbreviatedcasenumber", "AbbreviatedCaseNumber");
        biblatexToMsBib.put(MSBIB_PREFIX + "productioncompany", "ProductionCompany");
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
     * @param language The language to transform
     * @return Returns 0 for English
     */
    public static int getLCID(String language) {
        // TODO: add language to LCID mapping
        // 0 is English
        return 0;
    }

    /**
     * Only English is supported <br>
     * <a href="http://www.microsoft.com/globaldev/reference/lcid-all.mspx">All LCID codes</a>
     * @param language
     * @return Returns english
     */
    public static String getLanguage(int LCID) {
        // TODO: add language to LCID mapping
        return "english";
    }

    public static String getMSBibField(String bibtexFieldName) {
        return biblatexToMsBib.get(bibtexFieldName);
    }

    public static String getBibTeXField(String msbibFieldName) {
        return biblatexToMsBib.inverse().get(msbibFieldName);
    }
}

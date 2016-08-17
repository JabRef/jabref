package net.sf.jabref.logic.msbib;

import java.util.Map;
import java.util.TreeMap;

import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibLatexEntryTypes;
import net.sf.jabref.model.entry.BibtexEntryTypes;
import net.sf.jabref.model.entry.FieldName;

import com.google.common.collect.HashBiMap;

/**
 * Mapping between Msbib and Bibtex
 * All Fields: <a href = "https://msdn.microsoft.com/de-de/library/office/documentformat.openxml.bibliography">List of all MSBib fields</a>
 *
 */
public class MSBibMapping {

    private static final String BIBTEX_PREFIX = "BIBTEX_";
    private static final String MSBIB_PREFIX = "msbib-";

    private static final HashBiMap<String, String> bibtexToMSBib = HashBiMap.create();

    static {
        bibtexToMSBib.put(BibEntry.KEY_FIELD, "Tag");
        bibtexToMSBib.put(FieldName.TITLE, "Title");
        bibtexToMSBib.put(FieldName.YEAR, "Year");
        bibtexToMSBib.put(FieldName.NOTE, "Comments");
        bibtexToMSBib.put(FieldName.VOLUME, "Volume");
        bibtexToMSBib.put(FieldName.LANGUAGE, "LCID");
        bibtexToMSBib.put(FieldName.EDITION, "Edition");
        bibtexToMSBib.put(FieldName.PUBLISHER, "Publisher");
        bibtexToMSBib.put(FieldName.BOOKTITLE, "BookTitle");
        //bibtexToMSBib.put(FieldName.BOOKTITLE, "ConferenceName");
        //bibtexToMSBib.put(FieldName.PAGES, "Pages");
        bibtexToMSBib.put(FieldName.CHAPTER, "ChapterNumber");

        bibtexToMSBib.put(FieldName.ISSUE, "Issue");
        bibtexToMSBib.put(FieldName.SCHOOL, "Department");
        bibtexToMSBib.put(FieldName.INSTITUTION, "Institution");
        bibtexToMSBib.put(FieldName.DOI, "DOI");
        bibtexToMSBib.put(FieldName.URL, "URL");
        // BibTeX/Biblatex only fields
        bibtexToMSBib.put(FieldName.SERIES, BIBTEX_PREFIX + "Series");
        bibtexToMSBib.put(FieldName.ABSTRACT, BIBTEX_PREFIX + "Abstract");
        bibtexToMSBib.put(FieldName.KEYWORDS, BIBTEX_PREFIX + "KeyWords");
        bibtexToMSBib.put(FieldName.CROSSREF, BIBTEX_PREFIX + "CrossRef");
        bibtexToMSBib.put(FieldName.HOWPUBLISHED, BIBTEX_PREFIX + "HowPublished");
        bibtexToMSBib.put("affiliation", BIBTEX_PREFIX + "Affiliation");
        bibtexToMSBib.put("contents", BIBTEX_PREFIX + "Contents");
        bibtexToMSBib.put("copyright", BIBTEX_PREFIX + "Copyright");
        bibtexToMSBib.put("price", BIBTEX_PREFIX + "Price");
        bibtexToMSBib.put("size", BIBTEX_PREFIX + "Size");
        bibtexToMSBib.put("intype", BIBTEX_PREFIX + "InType");
        bibtexToMSBib.put("paper", BIBTEX_PREFIX + "Paper");
        bibtexToMSBib.put("shorttitle", "ShortTitle");
        // MSBib only fields
        bibtexToMSBib.put(MSBIB_PREFIX + "numberofvolume", "NumberVolumes");
        bibtexToMSBib.put(MSBIB_PREFIX + "periodical", "PeriodicalTitle");
        bibtexToMSBib.put(MSBIB_PREFIX + "day", "Day");
        bibtexToMSBib.put(MSBIB_PREFIX + "accessed", "Accessed");
        bibtexToMSBib.put(MSBIB_PREFIX + "medium", "Medium");
        bibtexToMSBib.put(MSBIB_PREFIX + "recordingnumber", "RecordingNumber");
        bibtexToMSBib.put(MSBIB_PREFIX + "theater", "Theater");
        bibtexToMSBib.put(MSBIB_PREFIX + "distributor", "Distributor");
        bibtexToMSBib.put(MSBIB_PREFIX + "broadcaster", "Broadcaster");
        bibtexToMSBib.put(MSBIB_PREFIX + "station", "Station");
        bibtexToMSBib.put(MSBIB_PREFIX + FieldName.TYPE, "Type");
        bibtexToMSBib.put(MSBIB_PREFIX + "court", "Court");
        bibtexToMSBib.put(MSBIB_PREFIX + "reporter", "Reporter");
        bibtexToMSBib.put(MSBIB_PREFIX + "casenumber", "CaseNumber");
        bibtexToMSBib.put(MSBIB_PREFIX + "abbreviatedcasenumber", "AbbreviatedCaseNumber");
        bibtexToMSBib.put(MSBIB_PREFIX + "productioncompany", "ProductionCompany");
    }


    public static String getBibTeXEntryType(String msbibType) {
        final String defaultType = BibtexEntryTypes.MISC.getName();

        Map<String, String> entryTypeMapping = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        entryTypeMapping.put("Book", BibLatexEntryTypes.BOOK.getName());
        entryTypeMapping.put("BookSection", BibLatexEntryTypes.BOOK.getName());
        entryTypeMapping.put("JournalArticle", BibLatexEntryTypes.ARTICLE.getName());
        entryTypeMapping.put("ArticleInAPeriodical", BibLatexEntryTypes.PERIODICAL.getName());
        entryTypeMapping.put("ConferenceProceedings", BibLatexEntryTypes.INPROCEEDINGS.getName());
        entryTypeMapping.put("Report", BibLatexEntryTypes.TECHREPORT.getName());
        entryTypeMapping.put("Patent", BibLatexEntryTypes.PATENT.getName());
        entryTypeMapping.put("InternetSite", BibLatexEntryTypes.ONLINE.getName());

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

    // http://www.microsoft.com/globaldev/reference/lcid-all.mspx
    /**
     *  Only English is supported
     * @param language
     * @return Returns 0 for English
     */
    @Deprecated
    public static int getLCID(String language) {
        // TODO: add language to LCID mapping
        // 0 is English
        return 0;
    }

    // http://www.microsoft.com/globaldev/reference/lcid-all.mspx
    /**
     * Only English is supported
     * @param language
     * @return Returns english
     */
    @Deprecated
    public static String getLanguage(int LCID) {
        // TODO: add language to LCID mapping
        return "english";
    }

    public static String getMSBibField(String bibtexFieldName) {
        return bibtexToMSBib.get(bibtexFieldName);
    }

    public static String getBibTeXField(String msbibFieldName) {
        return bibtexToMSBib.inverse().get(msbibFieldName);
    }
}

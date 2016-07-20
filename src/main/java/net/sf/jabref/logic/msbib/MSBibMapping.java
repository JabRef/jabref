package net.sf.jabref.logic.msbib;

import java.util.Map;
import java.util.TreeMap;

import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexEntryTypes;
import net.sf.jabref.model.entry.FieldName;

import com.google.common.collect.HashBiMap;

public class MSBibMapping {
    private static final String BIBTEX_PREFIX = "BIBTEX_";
    private static final String MSBIB_PREFIX = "msbib-";

    private static final HashBiMap<String, String> bibtexToMSBib = HashBiMap.create();

    static {
        bibtexToMSBib.put(BibEntry.KEY_FIELD, "Tag");
        bibtexToMSBib.put(FieldName.TITLE, "Title");
        bibtexToMSBib.put(FieldName.YEAR, "Year");
        bibtexToMSBib.put(FieldName.MONTH, "Month");
        bibtexToMSBib.put("note", "Comments");
        bibtexToMSBib.put(FieldName.VOLUME, "Volume");
        bibtexToMSBib.put("language", "LCID");
        bibtexToMSBib.put("edition", "Edition");
        bibtexToMSBib.put(FieldName.PUBLISHER, "Publisher");
        bibtexToMSBib.put("booktitle", "BookTitle");
        //bibtexToMSBib.put("booktitle", "ConferenceName");
        //bibtexToMSBib.put(FieldName.PAGES, "Pages");
        bibtexToMSBib.put("chapter", "ChapterNumber");
        bibtexToMSBib.put(FieldName.JOURNAL, "JournalName");
        bibtexToMSBib.put(FieldName.NUMBER, "Issue");
        bibtexToMSBib.put("school", "Department");
        bibtexToMSBib.put("institution", "Institution");
        bibtexToMSBib.put(FieldName.DOI, "DOI");
        bibtexToMSBib.put(FieldName.URL, "URL");
        // BibTeX/Biblatex only fields
        bibtexToMSBib.put("series", BIBTEX_PREFIX + "Series");
        bibtexToMSBib.put(FieldName.ABSTRACT, BIBTEX_PREFIX + "Abstract");
        bibtexToMSBib.put(FieldName.KEYWORDS, BIBTEX_PREFIX + "KeyWords");
        bibtexToMSBib.put(FieldName.CROSSREF, BIBTEX_PREFIX + "CrossRef");
        bibtexToMSBib.put("howpublished", BIBTEX_PREFIX + "HowPublished");
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
        //bibtexToMSBib.put(MSBIB_PREFIX + "day", "Day");
        //bibtexToMSBib.put(PREFIX + "accessed", "Accessed");
        bibtexToMSBib.put(MSBIB_PREFIX + "medium", "Medium");
        bibtexToMSBib.put(MSBIB_PREFIX + "recordingnumber", "RecordingNumber");
        bibtexToMSBib.put(MSBIB_PREFIX + "theater", "Theater");
        bibtexToMSBib.put(MSBIB_PREFIX + "distributor", "Distributor");
        bibtexToMSBib.put(MSBIB_PREFIX + "broadcaster", "Broadcaster");
        bibtexToMSBib.put(MSBIB_PREFIX + "station", "Station");
        bibtexToMSBib.put(MSBIB_PREFIX + "type", "Type");
        bibtexToMSBib.put(MSBIB_PREFIX + "patentnumber", "PatentNumber");
        bibtexToMSBib.put(MSBIB_PREFIX + "court", "Court");
        bibtexToMSBib.put(MSBIB_PREFIX + "reporter", "Reporter");
        bibtexToMSBib.put(MSBIB_PREFIX + "casenumber", "CaseNumber");
        bibtexToMSBib.put(MSBIB_PREFIX + "abbreviatedcasenumber", "AbbreviatedCaseNumber");
        bibtexToMSBib.put(MSBIB_PREFIX + "productioncompany", "ProductionCompany");
    }

    public static String getBibTeXEntryType(String msbibType) {
        final String defaultType = BibtexEntryTypes.MISC.getName();

        Map<String, String> entryTypeMapping = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        entryTypeMapping.put("Book", BibtexEntryTypes.BOOK.getName());
        entryTypeMapping.put("BookSection", BibtexEntryTypes.INBOOK.getName());
        entryTypeMapping.put("JournalArticle", BibtexEntryTypes.ARTICLE.getName());
        entryTypeMapping.put("ArticleInAPeriodical", BibtexEntryTypes.ARTICLE.getName());
        entryTypeMapping.put("ConferenceProceedings", BibtexEntryTypes.CONFERENCE.getName());
        entryTypeMapping.put("Report", BibtexEntryTypes.TECHREPORT.getName());

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
        entryTypeMapping.put("electronic", MSBibEntryType.Misc);

        return entryTypeMapping.getOrDefault(bibtexType, MSBibEntryType.Misc);
    }

    // http://www.microsoft.com/globaldev/reference/lcid-all.mspx
    public static int getLCID(String language) {
        // TODO: add language to LCID mapping
        // 0 is English
        return 0;
    }

    // http://www.microsoft.com/globaldev/reference/lcid-all.mspx
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

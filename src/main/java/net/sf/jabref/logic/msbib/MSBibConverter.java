package net.sf.jabref.logic.msbib;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.sf.jabref.logic.layout.format.LatexToUnicodeFormatter;
import net.sf.jabref.logic.layout.format.RemoveBrackets;
import net.sf.jabref.logic.mods.PageNumbers;
import net.sf.jabref.logic.mods.PersonName;
import net.sf.jabref.model.entry.BibEntry;

import com.google.common.collect.HashBiMap;

public class MSBibConverter {
    private static final String BIBTEX_PREFIX = "BIBTEX_";
    private static final String MSBIB_PREFIX = "msbib-";

    public static final Map<String, String> bibtexToMSBib = HashBiMap.create();

    static {
        bibtexToMSBib.put(BibEntry.KEY_FIELD, "Tag");
        bibtexToMSBib.put("title", "Title");
        bibtexToMSBib.put("year", "Year");
        bibtexToMSBib.put("month", "Month");
        bibtexToMSBib.put("note", "Comments");
        bibtexToMSBib.put("volume", "Volume");
        bibtexToMSBib.put("edition", "Edition");
        bibtexToMSBib.put("publisher", "Publisher");
        bibtexToMSBib.put("booktitle", "BookTitle");
        //bibtexToMSBib.put("booktitle", "ConferenceName");
        bibtexToMSBib.put("chapter", "ChapterNumber");
        bibtexToMSBib.put("journal", "JournalName");
        bibtexToMSBib.put("number", "Issue");
        bibtexToMSBib.put("school", "Department");
        bibtexToMSBib.put("institution", "Institution");
        bibtexToMSBib.put("doi", "DOI");
        bibtexToMSBib.put("url", "URL");
        // BibTeX only fields
        bibtexToMSBib.put("series", BIBTEX_PREFIX + "Series");
        bibtexToMSBib.put("abstract", BIBTEX_PREFIX + "Abstract");
        bibtexToMSBib.put("keywords", BIBTEX_PREFIX + "KeyWords");
        bibtexToMSBib.put("crossref", BIBTEX_PREFIX + "CrossRef");
        bibtexToMSBib.put("howpublished", BIBTEX_PREFIX + "HowPublished");
        bibtexToMSBib.put("affiliation", BIBTEX_PREFIX + "Affiliation");
        bibtexToMSBib.put("contents", BIBTEX_PREFIX + "Contents");
        bibtexToMSBib.put("copyright", BIBTEX_PREFIX + "Copyright");
        bibtexToMSBib.put("price", BIBTEX_PREFIX + "Price");
        bibtexToMSBib.put("size", BIBTEX_PREFIX + "Size");
        bibtexToMSBib.put("intype", BIBTEX_PREFIX + "InType");
        bibtexToMSBib.put("paper", BIBTEX_PREFIX + "Paper");
        // MSBib only fields
        //bibtexToMSBib.put(MSBIB_PREFIX + "day", "");
        bibtexToMSBib.put(MSBIB_PREFIX + "shorttitle", "ShortTitle");
        bibtexToMSBib.put(MSBIB_PREFIX + "numberofvolume", "NumberVolumes");
        bibtexToMSBib.put(MSBIB_PREFIX + "periodical", "PeriodicalTitle");
        //bibtexToMSBib.put(MSBIB_PREFIX + "accessed", "Accessed");
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

    public static MSBibEntry convert(BibEntry entry) {
        MSBibEntry result = new MSBibEntry();

        // memorize original type
        result.bibtexType = entry.getType();
        // define new type
        result.msbibType = getMSBibType(entry).name();

        for (Map.Entry<String, String> field : bibtexToMSBib.entrySet()) {
            String texField = field.getKey();
            String msField = field.getValue();

            if (entry.hasField(texField)) {
                // clean field
                String unicodeField = removeLaTeX(entry.getField(texField));

                result.fields.put(msField, unicodeField);
            }
        }

        // Duplicate: also added as BookTitle
        if (entry.hasField("booktitle")) {
            result.conferenceName = entry.getField("booktitle");
        }

        if (entry.hasField("pages")) {
            result.pages = new PageNumbers(entry.getField("pages"));
        }

        if (entry.hasField(MSBIB_PREFIX + "accessed")) {
            result.dateAccessed = entry.getField(MSBIB_PREFIX + "accessed");
        }

        if ("SoundRecording".equals(result.msbibType) && (entry.hasField("title"))) {
            result.albumTitle = entry.getField("title");
        }

        if ("Interview".equals(result.msbibType) && (entry.hasField("title"))) {
            result.broadcastTitle = entry.getField("title");
        }

        if (entry.hasField("language")) {
            result.LCID = result.getLCID(entry.getField("language"));
        }

        if (entry.hasField(MSBIB_PREFIX + "day")) {
            result.day = entry.getField(MSBIB_PREFIX + "day");
        }

        result.standardNumber = "";
        if (entry.hasField("isbn")) {
            result.standardNumber += " ISBN: " + entry.getField("isbn"); /* SM: 2010.10: lower case */
        }
        if (entry.hasField("issn")) {
            result.standardNumber += " ISSN: " + entry.getField("issn"); /* SM: 2010.10: lower case */
        }
        if (entry.hasField("lccn")) {
            result.standardNumber += " LCCN: " + entry.getField("lccn"); /* SM: 2010.10: lower case */
        }
        if (entry.hasField("mrnumber")) {
            result.standardNumber += " MRN: " + entry.getField("mrnumber");
        }
        if (entry.hasField("doi")) {
            result.standardNumber += " DOI: " + entry.getField("doi");
        }
        if (result.standardNumber.isEmpty()) {
            result.standardNumber = null;
        }

        if (entry.hasField("address")) {
            result.address = entry.getField("address");
        }

        if (entry.hasField("type")) {
            result.thesisType = entry.getField("type");
        } else {
            if ("techreport".equalsIgnoreCase(entry.getType())) {
                result.thesisType = "Tech. rep.";
            } else if ("mastersthesis".equalsIgnoreCase(entry.getType())) {
                result.thesisType = "Master's thesis";
            } else if ("phdthesis".equalsIgnoreCase(entry.getType())) {
                result.thesisType = "Ph.D. dissertation";
            } else if ("unpublished".equalsIgnoreCase(entry.getType())) {
                result.thesisType = "unpublished";
            }
        }

        if (("InternetSite".equals(result.msbibType) || "DocumentFromInternetSite".equals(result.msbibType))
                && (entry.hasField("title"))) {
            result.internetSiteTitle = entry.getField("title");
        }

        if (("ElectronicSource".equals(result.msbibType) || "Art".equals(result.msbibType) || "Misc".equals(result.msbibType))
                && (entry.hasField("title"))) {
            result.publicationTitle = entry.getField("title");
        }

        if (entry.hasField("author")) {
            result.authors = getAuthors(entry.getField("author"));
        }
        if (entry.hasField("editor")) {
            result.editors = getAuthors(entry.getField("editor"));
        }

        return result;
    }

    protected static MSBibEntryType getMSBibType(BibEntry entry) {
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

        return entryTypeMapping.getOrDefault(entry.getType(), MSBibEntryType.Misc);
    }

    private static List<PersonName> getAuthors(String authors) {
        List<PersonName> result = new ArrayList<>();

        // TODO: case-insensitive?!
        if (authors.contains(" and ")) {
            String[] names = authors.split(" and ");
            for (String name : names) {
                result.add(new PersonName(name));
            }
        } else {
            result.add(new PersonName(authors));
        }
        return result;
    }

    private static String removeLaTeX(String text) {
        // TODO: just use latex free version everywhere in the future
        String result = new RemoveBrackets().format(text);
        result = new LatexToUnicodeFormatter().format(result);

        return result;
    }
}

package net.sf.jabref.logic.msbib;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.sf.jabref.importer.fileformat.ImportFormat;
import net.sf.jabref.logic.mods.PersonName;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexEntryTypes;

public class BibTeXConverter {
    private static final String BIBTEX_PREFIX = "BIBTEX_";
    private static final String MSBIB_PREFIX = "msbib-";
    
    public static BibEntry convert(MSBibEntry entry) {
        BibEntry result;

        if (entry.tag == null) {
            result = new BibEntry(ImportFormat.DEFAULT_BIBTEXENTRY_ID, mapMSBibToBibtexType(entry.msbibType));
        } else {
            result = new BibEntry(entry.tag, mapMSBibToBibtexType(entry.msbibType)); // id assumes an existing database so don't
        }

        // Todo: add check for BibTexEntry types

        Map<String, String> hm = new HashMap<>();

        if (entry.tag != null) {
            hm.put(BibEntry.KEY_FIELD, entry.tag);
        }

        if (entry.LCID >= 0) {
            hm.put("language", entry.getLanguage(entry.LCID));
        }
        if (entry.title != null) {
            hm.put("title", entry.title);
        }
        if (entry.year != null) {
            hm.put("year", entry.year);
        }
        if (entry.shortTitle != null) {
            hm.put(MSBIB_PREFIX + "shorttitle", entry.shortTitle);
        }
        if (entry.comments != null) {
            hm.put("note", entry.comments);
        }

        addAuthor(hm, "author", entry.authors);
        addAuthor(hm, MSBIB_PREFIX + "bookauthor", entry.bookAuthors);
        addAuthor(hm, "editor", entry.editors);
        addAuthor(hm, MSBIB_PREFIX + "translator", entry.translators);
        addAuthor(hm, MSBIB_PREFIX + "producername", entry.producerNames);
        addAuthor(hm, MSBIB_PREFIX + "composer", entry.composers);
        addAuthor(hm, MSBIB_PREFIX + "conductor", entry.conductors);
        addAuthor(hm, MSBIB_PREFIX + "performer", entry.performers);
        addAuthor(hm, MSBIB_PREFIX + "writer", entry.writers);
        addAuthor(hm, MSBIB_PREFIX + "director", entry.directors);
        addAuthor(hm, MSBIB_PREFIX + "compiler", entry.compilers);
        addAuthor(hm, MSBIB_PREFIX + "interviewer", entry.interviewers);
        addAuthor(hm, MSBIB_PREFIX + "interviewee", entry.interviewees);
        addAuthor(hm, MSBIB_PREFIX + "inventor", entry.inventors);
        addAuthor(hm, MSBIB_PREFIX + "counsel", entry.counsels);

        if (entry.pages != null) {
            hm.put("pages", entry.pages.toString("--"));
        }
        if (entry.volume != null) {
            hm.put("volume", entry.volume);
        }
        if (entry.numberOfVolumes != null) {
            hm.put(MSBIB_PREFIX + "numberofvolume", entry.numberOfVolumes);
        }
        if (entry.edition != null) {
            hm.put("edition", entry.edition);
        }
        if (entry.edition != null) {
            hm.put("edition", entry.edition);
        }
        parseStandardNumber(entry.standardNumber, hm);

        if (entry.publisher != null) {
            hm.put("publisher", entry.publisher);
        }
        if (entry.publisher != null) {
            hm.put("publisher", entry.publisher);
        }
        if (entry.address != null) {
            hm.put("address", entry.address);
        }
        if (entry.bookTitle != null) {
            hm.put("booktitle", entry.bookTitle);
        }
        if (entry.chapterNumber != null) {
            hm.put("chapter", entry.chapterNumber);
        }
        if (entry.journalName != null) {
            hm.put("journal", entry.journalName);
        }
        if (entry.issue != null) {
            hm.put("number", entry.issue);
        }
        if (entry.month != null) {
            hm.put("month", entry.month);
        }
        if (entry.periodicalTitle != null) {
            hm.put("organization", entry.periodicalTitle);
        }
        if (entry.conferenceName != null) {
            hm.put("organization", entry.conferenceName);
        }
        if (entry.department != null) {
            hm.put("school", entry.department);
        }
        if (entry.institution != null) {
            hm.put("institution", entry.institution);
        }

        if (entry.dateAccessed != null) {
            hm.put(MSBIB_PREFIX + "accessed", entry.dateAccessed);
        }
        if (entry.doi != null) {
            hm.put("doi", entry.doi);
        }
        if (entry.url != null) {
            hm.put("url", entry.url);
        }
        if (entry.productionCompany != null) {
            hm.put(MSBIB_PREFIX + "productioncompany", entry.productionCompany);
        }

        if (entry.medium != null) {
            hm.put(MSBIB_PREFIX + "medium", entry.medium);
        }

        if (entry.recordingNumber != null) {
            hm.put(MSBIB_PREFIX + "recordingnumber", entry.recordingNumber);
        }
        if (entry.theater != null) {
            hm.put(MSBIB_PREFIX + "theater", entry.theater);
        }
        if (entry.distributor != null) {
            hm.put(MSBIB_PREFIX + "distributor", entry.distributor);
        }

        if (entry.broadcaster != null) {
            hm.put(MSBIB_PREFIX + "broadcaster", entry.broadcaster);
        }
        if (entry.station != null) {
            hm.put(MSBIB_PREFIX + "station", entry.station);
        }
        if (entry.type != null) {
            hm.put(MSBIB_PREFIX + "type", entry.type);
        }
        if (entry.patentNumber != null) {
            hm.put(MSBIB_PREFIX + "patentnumber", entry.patentNumber);
        }
        if (entry.court != null) {
            hm.put(MSBIB_PREFIX + "court", entry.court);
        }
        if (entry.reporter != null) {
            hm.put(MSBIB_PREFIX + "reporter", entry.reporter);
        }
        if (entry.caseNumber != null) {
            hm.put(MSBIB_PREFIX + "casenumber", entry.caseNumber);
        }
        if (entry.abbreviatedCaseNumber != null) {
            hm.put(MSBIB_PREFIX + "abbreviatedcasenumber", entry.abbreviatedCaseNumber);
        }

        if (entry.bibTexSeries != null) {
            hm.put("series", entry.bibTexSeries);
        }
        if (entry.bibTexAbstract != null) {
            hm.put("abstract", entry.bibTexAbstract);
        }
        if (entry.bibTexKeyWords != null) {
            hm.put("keywords", entry.bibTexKeyWords);
        }
        if (entry.bibTexCrossRef != null) {
            hm.put("crossref", entry.bibTexCrossRef);
        }
        if (entry.bibTexHowPublished != null) {
            hm.put("howpublished", entry.bibTexHowPublished);
        }
        if (entry.bibTexAffiliation != null) {
            hm.put("affiliation", entry.bibTexAffiliation);
        }
        if (entry.bibTexContents != null) {
            hm.put("contents", entry.bibTexContents);
        }
        if (entry.bibTexCopyright != null) {
            hm.put("copyright", entry.bibTexCopyright);
        }
        if (entry.bibTexPrice != null) {
            hm.put("price", entry.bibTexPrice);
        }
        if (entry.bibTexSize != null) {
            hm.put("size", entry.bibTexSize);
        }

        result.setField(hm);
        return result;
    }

    private static void addAuthor(Map<String, String> map, String type, List<PersonName> authors) {
        if (authors == null) {
            return;
        }
        String allAuthors = authors.stream().map(PersonName::getFullname).collect(Collectors.joining(" and "));

        map.put(type, allAuthors);
    }

    private static void parseSingleStandardNumber(String type, String bibtype, String standardNum, Map<String, String> map) {
        Pattern pattern = Pattern.compile(':' + type + ":(.[^:]+)");
        Matcher matcher = pattern.matcher(standardNum);
        if (matcher.matches()) {
            map.put(bibtype, matcher.group(1));
        }
    }

    private static void parseStandardNumber(String standardNum, Map<String, String> map) {
        if (standardNum == null) {
            return;
        }
        parseSingleStandardNumber("ISBN", "isbn", standardNum, map);
        parseSingleStandardNumber("ISSN", "issn", standardNum, map);
        parseSingleStandardNumber("LCCN", "lccn", standardNum, map);
        parseSingleStandardNumber("MRN", "mrnumber", standardNum, map);
        parseSingleStandardNumber("DOI", "doi", standardNum, map);
    }

    protected static String mapMSBibToBibtexType(String msbibType) {
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
}

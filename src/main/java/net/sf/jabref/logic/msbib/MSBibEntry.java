/*  Copyright (C) 2003-2016 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.logic.msbib;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.sf.jabref.importer.fileformat.ImportFormat;

import net.sf.jabref.logic.layout.format.LatexToUnicodeFormatter;
import net.sf.jabref.logic.layout.format.RemoveBrackets;
import net.sf.jabref.logic.mods.PageNumbers;
import net.sf.jabref.logic.mods.PersonName;
import net.sf.jabref.logic.util.strings.StringUtil;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexEntryTypes;
import net.sf.jabref.model.entry.EntryType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Date: May 15, 2007; May 03, 2007
 * <p>
 * History
 * May 03, 2007 - Added export functionality
 * May 15, 2007 - Added import functionality
 * May 16, 2007 - Changed all interger entries to strings,
 * except LCID which must be an integer.
 * To avoid exception during integer parsing
 * the exception is caught and LCID is set to zero.
 * Jan 06, 2012 - Changed the XML element ConferenceName to present
 * the Booktitle instead of the organization field content
 *
 * @author S M Mahbub Murshed (udvranto@yahoo.com)
 * @version 2.0.0
 * @see <a href="http://mahbub.wordpress.com/2007/03/24/details-of-microsoft-office-2007-bibliographic-format-compared-to-bibtex/">ms office 2007 bibliography format compared to bibtex</a>
 * @see <a href="http://mahbub.wordpress.com/2007/03/22/deciphering-microsoft-office-2007-bibliography-format/">deciphering ms office 2007 bibliography format</a>
 * See http://www.ecma-international.org/publications/standards/Ecma-376.htm
 */
class MSBibEntry {
    // Conversion maps
    private final Map<String, String> bibTeXToMSBib = null;
    private final Map<String, String> MSBibToBibTeX = null;

    private static final String BIBTEX_PREFIX = "BIBTEX_";
    private static final String MSBIB_PREFIX = "msbib-";

    private static final String B_COLON = "b:";

    // MSBib fields and values
    private Map<String, String> fields = new HashMap<>();
    private String sourceType = "Misc";
    private String bibTexEntry;
    private String tag;
    private static final String GUID = null;
    private int LCID = -1;
    private List<PersonName> authors;
    private List<PersonName> bookAuthors;
    private List<PersonName> editors;
    private List<PersonName> translators;
    private List<PersonName> producerNames;
    private List<PersonName> composers;
    private List<PersonName> conductors;
    private List<PersonName> performers;
    private List<PersonName> writers;
    private List<PersonName> directors;
    private List<PersonName> compilers;
    private List<PersonName> interviewers;
    private List<PersonName> interviewees;

    private List<PersonName> inventors;
    private List<PersonName> counsels;
    private String title;
    private String year;

    private String month;
    private String day;

    private String shortTitle;
    private String comments;
    private PageNumbers pages;
    private String volume;
    private String numberOfVolumes;
    private String edition;

    private String standardNumber;
    private String publisher;
    private String address;
    private String bookTitle;
    private String chapterNumber;
    private String journalName;
    private String issue;
    private String periodicalTitle;
    private String conferenceName;
    private String department;
    private String institution;
    private String thesisType;
    private String internetSiteTitle;
    private String dateAccessed;
    private String doi;
    private String url;
    private String productionCompany;
    private String publicationTitle;
    private String medium;
    private String albumTitle;
    private String recordingNumber;
    private String theater;
    private String distributor;
    private String broadcastTitle;
    private String broadcaster;
    private String station;
    private String type;
    private String patentNumber;
    private String court;
    private String reporter;
    private String caseNumber;
    private String abbreviatedCaseNumber;
    private String bibTexSeries;
    private String bibTexAbstract;
    private String bibTexKeyWords;
    private String bibTexCrossRef;
    private String bibTex_HowPublished;
    private String bibTexAffiliation;
    private String bibTexContents;
    private String bibTexCopyright;
    private String bibTexPrice;
    private String bibTexSize;
    private String bibTexInType;
    private String bibTexPaper;

    // reduced subset, supports only "CITY , STATE, COUNTRY"
    // \b(\w+)\s?[,]?\s?(\w+)\s?[,]?\s?(\w+)\b
    // WORD SPACE , SPACE WORD SPACE , SPACE WORD
    // tested using http://www.javaregex.com/test.html
    private static final Pattern ADDRESS_PATTERN = Pattern.compile("\\b(\\w+)\\s?[,]?\\s?(\\w+)\\s?[,]?\\s?(\\w+)\\b");

    // Allows 20.3-2007|||20/3-  2007 etc.
    // (\d{1,2})\s?[.,-/]\s?(\d{1,2})\s?[.,-/]\s?(\d{2,4})
    // 1-2 DIGITS SPACE SEPERATOR SPACE 1-2 DIGITS SPACE SEPERATOR SPACE 2-4 DIGITS
    // tested using http://www.javaregex.com/test.html
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{1,2})\\s*[.,-/]\\s*(\\d{1,2})\\s*[.,-/]\\s*(\\d{2,4})");


    public MSBibEntry(BibEntry entry) {
        populateFromBibtex(entry);
    }

    public MSBibEntry(Element entry, String bcol) {
        populateFromXml(entry, bcol);
    }

    private String getFromXml(String name, Element entry) {
        String value = null;
        NodeList nodeLst = entry.getElementsByTagName(name);
        if (nodeLst.getLength() > 0) {
            value = nodeLst.item(0).getTextContent();
        }
        return value;
    }

    private void populateFromXml(Element entry, String bcol) {
        String temp;

        sourceType = getFromXml(bcol + "SourceType", entry);

        tag = getFromXml(bcol + "Tag", entry);

        temp = getFromXml(bcol + "LCID", entry);
        if (temp != null) {
            try {
                LCID = Integer.parseInt(temp);
            } catch (NumberFormatException e) {
                LCID = -1;
            }
        }

        title = getFromXml(bcol + "Title", entry);
        year = getFromXml(bcol + "Year", entry);
        month = getFromXml(bcol + "Month", entry);
        day = getFromXml(bcol + "Day", entry);

        shortTitle = getFromXml(bcol + "ShortTitle", entry);
        comments = getFromXml(bcol + "Comments", entry);

        temp = getFromXml(bcol + "Pages", entry);
        if (temp != null) {
            pages = new PageNumbers(temp);
        }

        volume = getFromXml(bcol + "Volume", entry);

        numberOfVolumes = getFromXml(bcol + "NumberVolumes", entry);

        edition = getFromXml(bcol + "Edition", entry);

        standardNumber = getFromXml(bcol + "StandardNumber", entry);

        publisher = getFromXml(bcol + "Publisher", entry);

        String city = getFromXml(bcol + "City", entry);
        String state = getFromXml(bcol + "StateProvince", entry);
        String country = getFromXml(bcol + "CountryRegion", entry);
        StringBuilder addressBuffer = new StringBuilder();
        if (city != null) {
            addressBuffer.append(city).append(", ");
        }
        if (state != null) {
            addressBuffer.append(state).append(' ');
        }
        if (country != null) {
            addressBuffer.append(country);
        }
        address = addressBuffer.toString().trim();
        if (address.isEmpty() || ",".equals(address)) {
            address = null;
        }

        bookTitle = getFromXml(bcol + "BookTitle", entry);

        chapterNumber = getFromXml(bcol + "ChapterNumber", entry);

        journalName = getFromXml(bcol + "JournalName", entry);

        issue = getFromXml(bcol + "Issue", entry);

        periodicalTitle = getFromXml(bcol + "PeriodicalTitle", entry);

        conferenceName = getFromXml(bcol + "ConferenceName", entry);
        department = getFromXml(bcol + "Department", entry);
        institution = getFromXml(bcol + "Institution", entry);

        thesisType = getFromXml(bcol + "ThesisType", entry);
        internetSiteTitle = getFromXml(bcol + "InternetSiteTitle", entry);
        String month = getFromXml(bcol + "MonthAccessed", entry);
        String day = getFromXml(bcol + "DayAccessed", entry);
        String year = getFromXml(bcol + "YearAccessed", entry);
        dateAccessed = "";
        if (month != null) {
            dateAccessed += month + ' ';
        }
        if (day != null) {
            dateAccessed += day + ", ";
        }
        if (year != null) {
            dateAccessed += year;
        }
        dateAccessed = dateAccessed.trim();
        if (dateAccessed.isEmpty() || ",".equals(dateAccessed)) {
            dateAccessed = null;
        }

        doi = getFromXml(bcol + "DOI", entry);
        url = getFromXml(bcol + "URL", entry);
        productionCompany = getFromXml(bcol + "ProductionCompany", entry);

        publicationTitle = getFromXml(bcol + "PublicationTitle", entry);
        medium = getFromXml(bcol + "Medium", entry);
        albumTitle = getFromXml(bcol + "AlbumTitle", entry);
        recordingNumber = getFromXml(bcol + "RecordingNumber", entry);
        theater = getFromXml(bcol + "Theater", entry);
        distributor = getFromXml(bcol + "Distributor", entry);
        broadcastTitle = getFromXml(bcol + "BroadcastTitle", entry);
        broadcaster = getFromXml(bcol + "Broadcaster", entry);
        station = getFromXml(bcol + "Station", entry);
        type = getFromXml(bcol + "Type", entry);
        patentNumber = getFromXml(bcol + "PatentNumber", entry);
        court = getFromXml(bcol + "Court", entry);
        reporter = getFromXml(bcol + "Reporter", entry);
        caseNumber = getFromXml(bcol + "CaseNumber", entry);
        abbreviatedCaseNumber = getFromXml(bcol + "AbbreviatedCaseNumber", entry);
        bibTexSeries = getFromXml(bcol + BIBTEX_PREFIX + "Series", entry);
        bibTexAbstract = getFromXml(bcol + BIBTEX_PREFIX + "Abstract", entry);
        bibTexKeyWords = getFromXml(bcol + BIBTEX_PREFIX + "KeyWords", entry);
        bibTexCrossRef = getFromXml(bcol + BIBTEX_PREFIX + "CrossRef", entry);
        bibTex_HowPublished = getFromXml(bcol + BIBTEX_PREFIX + "HowPublished", entry);
        bibTexAffiliation = getFromXml(bcol + BIBTEX_PREFIX + "Affiliation", entry);
        bibTexContents = getFromXml(bcol + BIBTEX_PREFIX + "Contents", entry);
        bibTexCopyright = getFromXml(bcol + BIBTEX_PREFIX + "Copyright", entry);
        bibTexPrice = getFromXml(bcol + BIBTEX_PREFIX + "Price", entry);
        bibTexSize = getFromXml(bcol + BIBTEX_PREFIX + "Size", entry);

        NodeList nodeLst = entry.getElementsByTagName(bcol + "Author");
        if (nodeLst.getLength() > 0) {
            getAuthors((Element) nodeLst.item(0), bcol);
        }
    }

    public Element getDOM(Document document) {
        Element rootNode = document.createElement(B_COLON + "Source");

        for (Map.Entry<String, String> entry : fields.entrySet()) {
            addField(document, rootNode, entry.getKey(), entry.getValue());
        }

        // FIXME: old
        addField(document, rootNode, "SourceType", sourceType);
        addField(document, rootNode, BIBTEX_PREFIX + "Entry", bibTexEntry);

        addField(document, rootNode, "GUID", GUID);
        if (LCID >= 0) {
            addField(document, rootNode, "LCID", Integer.toString(LCID));
        }

        addDate(document, rootNode, dateAccessed, "Accessed");

        Element allAuthors = document.createElement(B_COLON + "Author");

        addAuthor(document, allAuthors, "Author", authors);
        String bookAuthor = "BookAuthor";
        addAuthor(document, allAuthors, bookAuthor, bookAuthors);
        addAuthor(document, allAuthors, "Editor", editors);
        addAuthor(document, allAuthors, "Translator", translators);
        addAuthor(document, allAuthors, "ProducerName", producerNames);
        addAuthor(document, allAuthors, "Composer", composers);
        addAuthor(document, allAuthors, "Conductor", conductors);
        addAuthor(document, allAuthors, "Performer", performers);
        addAuthor(document, allAuthors, "Writer", writers);
        addAuthor(document, allAuthors, "Director", directors);
        addAuthor(document, allAuthors, "Compiler", compilers);
        addAuthor(document, allAuthors, "Interviewer", interviewers);
        addAuthor(document, allAuthors, "Interviewee", interviewees);
        addAuthor(document, allAuthors, "Inventor", inventors);
        addAuthor(document, allAuthors, "Counsel", counsels);

        rootNode.appendChild(allAuthors);

        if (pages != null) {
            addField(document, rootNode, "Pages", pages.toString("-"));
        }
        addField(document, rootNode, "StandardNumber", standardNumber);
        addField(document, rootNode, "ConferenceName", conferenceName);

        addAddress(document, rootNode, address);

        addField(document, rootNode, "ThesisType", thesisType);
        addField(document, rootNode, "InternetSiteTitle", internetSiteTitle);

        addField(document, rootNode, "ProductionCompany", productionCompany);
        addField(document, rootNode, "PublicationTitle", publicationTitle);
        addField(document, rootNode, "AlbumTitle", albumTitle);
        addField(document, rootNode, "BroadcastTitle", broadcastTitle);

        return rootNode;
    }

    private void populateFromBibtex(BibEntry entry) {
        sourceType = getMSBibSourceType(entry);

        final Map<String, String> bibtexToMSBib = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

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


        for (Map.Entry<String, String> field : bibtexToMSBib.entrySet()) {
            String texField = field.getKey();
            String msField = field.getValue();

            if (entry.hasField(texField)) {
                // clean field
                String unicodeField = removeLaTeX(entry.getField(texField));

                fields.put(msField, unicodeField);
            }
        }

        if (entry.hasField("booktitle")) {
            conferenceName = entry.getField("booktitle");
        }

        if (entry.hasField(MSBIB_PREFIX + "accessed")) {
            dateAccessed = entry.getField(MSBIB_PREFIX + "accessed");
        }

        if ("SoundRecording".equals(sourceType) && (entry.hasField("title"))) {
            albumTitle = entry.getField("title");
        }

        if ("Interview".equals(sourceType) && (entry.hasField("title"))) {
            broadcastTitle = entry.getField("title");
        }

        if (entry.hasField("language")) {
            LCID = getLCID(entry.getField("language"));
        }

        if (entry.hasField(MSBIB_PREFIX + "day")) {
            day = entry.getField(MSBIB_PREFIX + "day");
        }

        if (entry.hasField("pages")) {
            pages = new PageNumbers(entry.getField("pages"));
        }

        standardNumber = "";
        if (entry.hasField("isbn")) {
            standardNumber += " ISBN: " + entry.getField("isbn"); /* SM: 2010.10: lower case */
        }
        if (entry.hasField("issn")) {
            standardNumber += " ISSN: " + entry.getField("issn"); /* SM: 2010.10: lower case */
        }
        if (entry.hasField("lccn")) {
            standardNumber += " LCCN: " + entry.getField("lccn"); /* SM: 2010.10: lower case */
        }
        if (entry.hasField("mrnumber")) {
            standardNumber += " MRN: " + entry.getField("mrnumber");
        }
        if (entry.hasField("doi")) {
            standardNumber += " DOI: " + entry.getField("doi");
        }
        if (standardNumber.isEmpty()) {
            standardNumber = null;
        }

        if (entry.hasField("address")) {
            address = entry.getField("address");
        }

        /* SM: 2010.10 Modified for default source types */
        if (entry.hasField("type")) {
            thesisType = entry.getField("type");
        } else {
            if ("techreport".equalsIgnoreCase(entry.getType())) {
                thesisType = "Tech. rep.";
            } else if ("mastersthesis".equalsIgnoreCase(entry.getType())) {
                thesisType = "Master's thesis";
            } else if ("phdthesis".equalsIgnoreCase(entry.getType())) {
                thesisType = "Ph.D. dissertation";
            } else if ("unpublished".equalsIgnoreCase(entry.getType())) {
                thesisType = "unpublished";
            }
        }

        if (("InternetSite".equals(sourceType) || "DocumentFromInternetSite".equals(sourceType))
                && (entry.hasField("title"))) {
            internetSiteTitle = entry.getField("title");
        }

        if (("ElectronicSource".equals(sourceType) || "Art".equals(sourceType) || "Misc".equals(sourceType))
                && (entry.hasField("title"))) {
            publicationTitle = entry.getField("title");
        }

        if (entry.hasField("author")) {
            authors = getAuthors(entry.getField("author"));
        }
        if (entry.hasField("editor")) {
            editors = getAuthors(entry.getField("editor"));
        }
    }

    // http://www.microsoft.com/globaldev/reference/lcid-all.mspx
    private int getLCID(String language) {
        // TODO: add language to LCID mapping
        // 0 is English
        return 0;
    }

    // http://www.microsoft.com/globaldev/reference/lcid-all.mspx
    private String getLanguage(int LCID) {
        // TODO: add language to LCID mapping
        return "english";
    }

    private List<PersonName> getSpecificAuthors(String type, Element authors, String bcol) {
        List<PersonName> result = null;
        NodeList nodeLst = authors.getElementsByTagName(bcol + type);
        if (nodeLst.getLength() <= 0) {
            return result;
        }
        nodeLst = ((Element) nodeLst.item(0)).getElementsByTagName(bcol + "NameList");
        if (nodeLst.getLength() <= 0) {
            return result;
        }
        NodeList person = ((Element) nodeLst.item(0)).getElementsByTagName(bcol + "Person");
        if (person.getLength() <= 0) {
            return result;
        }

        result = new LinkedList<>();
        for (int i = 0; i < person.getLength(); i++) {
            NodeList firstName = ((Element) person.item(i)).getElementsByTagName(bcol + "First");
            NodeList lastName = ((Element) person.item(i)).getElementsByTagName(bcol + "Last");
            NodeList middleName = ((Element) person.item(i)).getElementsByTagName(bcol + "Middle");
            PersonName name = new PersonName();
            if (firstName.getLength() > 0) {
                name.setFirstname(firstName.item(0).getTextContent());
            }
            if (middleName.getLength() > 0) {
                name.setMiddlename(middleName.item(0).getTextContent());
            }
            if (lastName.getLength() > 0) {
                name.setSurname(lastName.item(0).getTextContent());
            }
            result.add(name);
        }

        return result;
    }

    private void getAuthors(Element authorsElem, String bcol) {
        authors = getSpecificAuthors("Author", authorsElem, bcol);
        bookAuthors = getSpecificAuthors("BookAuthor", authorsElem, bcol);
        editors = getSpecificAuthors("Editor", authorsElem, bcol);
        translators = getSpecificAuthors("Translator", authorsElem, bcol);
        producerNames = getSpecificAuthors("ProducerName", authorsElem, bcol);
        composers = getSpecificAuthors("Composer", authorsElem, bcol);
        conductors = getSpecificAuthors("Conductor", authorsElem, bcol);
        performers = getSpecificAuthors("Performer", authorsElem, bcol);
        writers = getSpecificAuthors("Writer", authorsElem, bcol);
        directors = getSpecificAuthors("Director", authorsElem, bcol);
        compilers = getSpecificAuthors("Compiler", authorsElem, bcol);
        interviewers = getSpecificAuthors("Interviewer", authorsElem, bcol);
        interviewees = getSpecificAuthors("Interviewee", authorsElem, bcol);
        inventors = getSpecificAuthors("Inventor", authorsElem, bcol);
        counsels = getSpecificAuthors("Counsel", authorsElem, bcol);
    }

    private List<PersonName> getAuthors(String authors) {
        List<PersonName> result = new LinkedList<>();

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

    protected String getMSBibSourceType(BibEntry bibtex) {
        final String defaultType = "Misc";

        Map<String, String> entryTypeMapping = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        entryTypeMapping.put("book", "Book");
        entryTypeMapping.put("inbook", "BookSection");
        entryTypeMapping.put("booklet", "BookSection");
        entryTypeMapping.put("incollection", "BookSection");
        entryTypeMapping.put("article", "JournalArticle");
        entryTypeMapping.put("inproceedings", "ConferenceProceedings");
        entryTypeMapping.put("conference", "ConferenceProceedings");
        entryTypeMapping.put("proceedings", "ConferenceProceedings");
        entryTypeMapping.put("collection", "ConferenceProceedings");
        entryTypeMapping.put("techreport", "Report");
        entryTypeMapping.put("manual", "Report");
        entryTypeMapping.put("mastersthesis", "Report");
        entryTypeMapping.put("phdthesis", "Report");
        entryTypeMapping.put("unpublished", "Report");
        entryTypeMapping.put("patent", "Patent");
        entryTypeMapping.put("misc", "Misc");
        entryTypeMapping.put("electronic", "Misc");


        // default type
        String bibtexType = bibtex.getType();

        if ("book".equalsIgnoreCase(bibtexType)) {
        } else if ("inbook".equalsIgnoreCase(bibtexType)) {
            bibTexEntry = "inbook";
        } // SM 2010.10: generalized
        else if ("booklet".equalsIgnoreCase(bibtexType)) {
            bibTexEntry = "booklet";
        } else if ("incollection".equalsIgnoreCase(bibtexType)) {
            bibTexEntry = "incollection";
        } else if ("article".equalsIgnoreCase(bibtexType)) {
        } else if ("inproceedings".equalsIgnoreCase(bibtexType)) {
            bibTexEntry = "inproceedings";
        } // SM 2010.10: generalized
        else if ("conference".equalsIgnoreCase(bibtexType)) {
            bibTexEntry = "conference";
        } else if ("proceedings".equalsIgnoreCase(bibtexType)) {
            bibTexEntry = "proceedings";
        } else if ("collection".equalsIgnoreCase(bibtexType)) {
            bibTexEntry = "collection";
        } else if ("techreport".equalsIgnoreCase(bibtexType)) {
            bibTexEntry = "techreport";
        } // SM 2010.10: generalized
        else if ("manual".equalsIgnoreCase(bibtexType)) {
            bibTexEntry = "manual";
        } else if ("mastersthesis".equalsIgnoreCase(bibtexType)) {
            bibTexEntry = "mastersthesis";
        } else if ("phdthesis".equalsIgnoreCase(bibtexType)) {
            bibTexEntry = "phdthesis";
        } else if ("unpublished".equalsIgnoreCase(bibtexType)) {
            bibTexEntry = "unpublished";
        } else if ("patent".equalsIgnoreCase(bibtexType)) {
        } else if ("misc".equalsIgnoreCase(bibtexType)) {
        } else if ("electronic".equalsIgnoreCase(bibtexType)) {
            bibTexEntry = "electronic";
        }

        return entryTypeMapping.getOrDefault(bibtex.getType(), defaultType);
    }

    private void addField(Document document, Element parent, String name, String value) {
        if (value == null) {
            return;
        }
        Element elem = document.createElement(B_COLON + name);
        elem.appendChild(document.createTextNode(StringUtil.stripNonValidXMLCharacters(value)));
        parent.appendChild(elem);
    }

    private void addAuthor(Document document, Element allAuthors, String entryName, List<PersonName> authorsLst) {
        if (authorsLst == null) {
            return;
        }
        Element authorTop = document.createElement(B_COLON + entryName);
        Element nameList = document.createElement(B_COLON + "NameList");
        for (PersonName name : authorsLst) {
            Element person = document.createElement(B_COLON + "Person");
            addField(document, person, "Last", name.getSurname());
            addField(document, person, "Middle", name.getMiddlename());
            addField(document, person, "First", name.getFirstname());
            nameList.appendChild(person);
        }
        authorTop.appendChild(nameList);

        allAuthors.appendChild(authorTop);
    }

    private void addAddress(Document document, Element parent, String address) {
        if (address == null) {
            return;
        }

        Matcher matcher = ADDRESS_PATTERN.matcher(address);
        if (matcher.matches() && (matcher.groupCount() >= 3)) {
            addField(document, parent, "City", matcher.group(1));
            addField(document, parent, "StateProvince", matcher.group(2));
            addField(document, parent, "CountryRegion", matcher.group(3));
        } else {
            /* SM: 2010.10 generalized */
            addField(document, parent, "City", address);
        }
    }

    private void addDate(Document document, Element parent, String date, String extra) {
        if (date == null) {
            return;
        }

        Matcher matcher = DATE_PATTERN.matcher(date);
        if (matcher.matches() && (matcher.groupCount() >= 3)) {
            addField(document, parent, "Month" + extra, matcher.group(1));
            addField(document, parent, "Day" + extra, matcher.group(2));
            addField(document, parent, "Year" + extra, matcher.group(3));
        }
    }

    private void parseSingleStandardNumber(String type, String bibtype, String standardNum, Map<String, String> map) {
        Pattern pattern = Pattern.compile(':' + type + ":(.[^:]+)");
        Matcher matcher = pattern.matcher(standardNum);
        if (matcher.matches()) {
            map.put(bibtype, matcher.group(1));
        }
    }

    private void parseStandardNumber(String standardNum, Map<String, String> map) {
        if (standardNumber == null) {
            return;
        }
        parseSingleStandardNumber("ISBN", "isbn", standardNum, map); /* SM: 2010.10: lower case */
        parseSingleStandardNumber("ISSN", "issn", standardNum, map); /* SM: 2010.10: lower case */
        parseSingleStandardNumber("LCCN", "lccn", standardNum, map); /* SM: 2010.10: lower case */
        parseSingleStandardNumber("MRN", "mrnumber", standardNum, map);
        parseSingleStandardNumber("DOI", "doi", standardNum, map);
    }

    private void addAuthor(Map<String, String> map, String type, List<PersonName> authors) {
        if (authors == null) {
            return;
        }
        String allAuthors = authors.stream().map(PersonName::getFullname).collect(Collectors.joining(" and "));

        map.put(type, allAuthors);
    }

    protected EntryType mapMSBibToBibtexType(String msbibType) {
        final EntryType defaultType = BibtexEntryTypes.MISC;

        Map<String, EntryType> entryTypeMapping = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        entryTypeMapping.put("Book", BibtexEntryTypes.BOOK);
        entryTypeMapping.put("BookSection", BibtexEntryTypes.INBOOK);
        entryTypeMapping.put("JournalArticle", BibtexEntryTypes.ARTICLE);
        entryTypeMapping.put("ArticleInAPeriodical", BibtexEntryTypes.ARTICLE);
        entryTypeMapping.put("ConferenceProceedings", BibtexEntryTypes.CONFERENCE);
        entryTypeMapping.put("Report", BibtexEntryTypes.TECHREPORT);

        return entryTypeMapping.getOrDefault(msbibType, defaultType);
    }

    public BibEntry getBibtexRepresentation() {

        BibEntry entry;
        if (tag == null) {
            entry = new BibEntry(ImportFormat.DEFAULT_BIBTEXENTRY_ID, mapMSBibToBibtexType(sourceType).getName());
        } else {
            entry = new BibEntry(tag, mapMSBibToBibtexType(sourceType).getName()); // id assumes an existing database so don't
        }

        // Todo: add check for BibTexEntry types

        Map<String, String> hm = new HashMap<>();

        if (tag != null) {
            hm.put(BibEntry.KEY_FIELD, tag);
        }

        if (LCID >= 0) {
            hm.put("language", getLanguage(LCID));
        }
        if (title != null) {
            hm.put("title", title);
        }
        if (year != null) {
            hm.put("year", year);
        }
        if (shortTitle != null) {
            hm.put(MSBIB_PREFIX + "shorttitle", shortTitle);
        }
        if (comments != null) {
            hm.put("note", comments);
        }

        addAuthor(hm, "author", authors);
        addAuthor(hm, MSBIB_PREFIX + "bookauthor", bookAuthors);
        addAuthor(hm, "editor", editors);
        addAuthor(hm, MSBIB_PREFIX + "translator", translators);
        addAuthor(hm, MSBIB_PREFIX + "producername", producerNames);
        addAuthor(hm, MSBIB_PREFIX + "composer", composers);
        addAuthor(hm, MSBIB_PREFIX + "conductor", conductors);
        addAuthor(hm, MSBIB_PREFIX + "performer", performers);
        addAuthor(hm, MSBIB_PREFIX + "writer", writers);
        addAuthor(hm, MSBIB_PREFIX + "director", directors);
        addAuthor(hm, MSBIB_PREFIX + "compiler", compilers);
        addAuthor(hm, MSBIB_PREFIX + "interviewer", interviewers);
        addAuthor(hm, MSBIB_PREFIX + "interviewee", interviewees);
        addAuthor(hm, MSBIB_PREFIX + "inventor", inventors);
        addAuthor(hm, MSBIB_PREFIX + "counsel", counsels);

        if (pages != null) {
            hm.put("pages", pages.toString("--"));
        }
        if (volume != null) {
            hm.put("volume", volume);
        }
        if (numberOfVolumes != null) {
            hm.put(MSBIB_PREFIX + "numberofvolume", numberOfVolumes);
        }
        if (edition != null) {
            hm.put("edition", edition);
        }
        if (edition != null) {
            hm.put("edition", edition);
        }
        parseStandardNumber(standardNumber, hm);

        if (publisher != null) {
            hm.put("publisher", publisher);
        }
        if (publisher != null) {
            hm.put("publisher", publisher);
        }
        if (address != null) {
            hm.put("address", address);
        }
        if (bookTitle != null) {
            hm.put("booktitle", bookTitle);
        }
        if (chapterNumber != null) {
            hm.put("chapter", chapterNumber);
        }
        if (journalName != null) {
            hm.put("journal", journalName);
        }
        if (issue != null) {
            hm.put("number", issue);
        }
        if (month != null) {
            hm.put("month", month);
        }
        if (periodicalTitle != null) {
            hm.put("organization", periodicalTitle);
        }
        if (conferenceName != null) {
            hm.put("organization", conferenceName);
        }
        if (department != null) {
            hm.put("school", department);
        }
        if (institution != null) {
            hm.put("institution", institution);
        }

        if (dateAccessed != null) {
            hm.put(MSBIB_PREFIX + "accessed", dateAccessed);
        }
        if (doi != null) {
            hm.put("doi", doi);
        }
        if (url != null) {
            hm.put("url", url);
        }
        if (productionCompany != null) {
            hm.put(MSBIB_PREFIX + "productioncompany", productionCompany);
        }

        if (medium != null) {
            hm.put(MSBIB_PREFIX + "medium", medium);
        }

        if (recordingNumber != null) {
            hm.put(MSBIB_PREFIX + "recordingnumber", recordingNumber);
        }
        if (theater != null) {
            hm.put(MSBIB_PREFIX + "theater", theater);
        }
        if (distributor != null) {
            hm.put(MSBIB_PREFIX + "distributor", distributor);
        }

        if (broadcaster != null) {
            hm.put(MSBIB_PREFIX + "broadcaster", broadcaster);
        }
        if (station != null) {
            hm.put(MSBIB_PREFIX + "station", station);
        }
        if (type != null) {
            hm.put(MSBIB_PREFIX + "type", type);
        }
        if (patentNumber != null) {
            hm.put(MSBIB_PREFIX + "patentnumber", patentNumber);
        }
        if (court != null) {
            hm.put(MSBIB_PREFIX + "court", court);
        }
        if (reporter != null) {
            hm.put(MSBIB_PREFIX + "reporter", reporter);
        }
        if (caseNumber != null) {
            hm.put(MSBIB_PREFIX + "casenumber", caseNumber);
        }
        if (abbreviatedCaseNumber != null) {
            hm.put(MSBIB_PREFIX + "abbreviatedcasenumber", abbreviatedCaseNumber);
        }

        if (bibTexSeries != null) {
            hm.put("series", bibTexSeries);
        }
        if (bibTexAbstract != null) {
            hm.put("abstract", bibTexAbstract);
        }
        if (bibTexKeyWords != null) {
            hm.put("keywords", bibTexKeyWords);
        }
        if (bibTexCrossRef != null) {
            hm.put("crossref", bibTexCrossRef);
        }
        if (bibTex_HowPublished != null) {
            hm.put("howpublished", bibTex_HowPublished);
        }
        if (bibTexAffiliation != null) {
            hm.put("affiliation", bibTexAffiliation);
        }
        if (bibTexContents != null) {
            hm.put("contents", bibTexContents);
        }
        if (bibTexCopyright != null) {
            hm.put("copyright", bibTexCopyright);
        }
        if (bibTexPrice != null) {
            hm.put("price", bibTexPrice);
        }
        if (bibTexSize != null) {
            hm.put("size", bibTexSize);
        }

        entry.setField(hm);
        return entry;
    }

    private String removeLaTeX(String text) {
        // TODO: just use latex free version everywhere in the future
        // TODO: use for every field?
        String result = new RemoveBrackets().format(text);
        result = new LatexToUnicodeFormatter().format(result);

        return result;
    }
}

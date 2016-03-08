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

import java.io.StringWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.sf.jabref.importer.fileformat.ImportFormat;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexEntryTypes;
import net.sf.jabref.logic.layout.LayoutFormatter;
import net.sf.jabref.logic.layout.format.RemoveBrackets;
import net.sf.jabref.logic.layout.format.XMLChars;
import net.sf.jabref.logic.mods.PageNumbers;
import net.sf.jabref.logic.mods.PersonName;
import net.sf.jabref.logic.util.strings.StringUtil;
import net.sf.jabref.model.entry.EntryType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
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
    private static final Log LOGGER = LogFactory.getLog(MSBibEntry.class);

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

    /* SM 2010.10 intype, paper support */
    private String bibTexInType;
    private String bibTexPaper;

    private static final String BIBTEX = "BIBTEX_";
    private static final String MSBIB = "msbib-";

    private static final String B_COLON = "b:";

    // reduced subset, supports only "CITY , STATE, COUNTRY"
    // \b(\w+)\s?[,]?\s?(\w+)\s?[,]?\s?(\w+)\b
    // WORD SPACE , SPACE WORD SPACE , SPACE WORD
    // tested using http://www.javaregex.com/test.html
    private static final Pattern ADDRESS_PATTERN = Pattern.compile("\\b(\\w+)\\s*[,]?\\s*(\\w+)\\s*[,]?\\s*(\\w+)\\b");

    // Allows 20.3-2007|||20/3-  2007 etc.
    // (\d{1,2})\s?[.,-/]\s?(\d{1,2})\s?[.,-/]\s?(\d{2,4})
    // 1-2 DIGITS SPACE SEPERATOR SPACE 1-2 DIGITS SPACE SEPERATOR SPACE 2-4 DIGITS
    // tested using http://www.javaregex.com/test.html
    private static final Pattern DATE_PATTERN = Pattern
            .compile("(\\d{1,2})\\s*[.,-/]\\s*(\\d{1,2})\\s*[.,-/]\\s*(\\d{2,4})");


    public MSBibEntry(BibEntry bibtex) {
        populateFromBibtex(bibtex);
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
        StringBuffer addressBuffer = new StringBuffer();
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
        bibTexSeries = getFromXml(bcol + BIBTEX + "Series", entry);
        bibTexAbstract = getFromXml(bcol + BIBTEX + "Abstract", entry);
        bibTexKeyWords = getFromXml(bcol + BIBTEX + "KeyWords", entry);
        bibTexCrossRef = getFromXml(bcol + BIBTEX + "CrossRef", entry);
        bibTex_HowPublished = getFromXml(bcol + BIBTEX + "HowPublished", entry);
        bibTexAffiliation = getFromXml(bcol + BIBTEX + "Affiliation", entry);
        bibTexContents = getFromXml(bcol + BIBTEX + "Contents", entry);
        bibTexCopyright = getFromXml(bcol + BIBTEX + "Copyright", entry);
        bibTexPrice = getFromXml(bcol + BIBTEX + "Price", entry);
        bibTexSize = getFromXml(bcol + BIBTEX + "Size", entry);

        NodeList nodeLst = entry.getElementsByTagName(bcol + "Author");
        if (nodeLst.getLength() > 0) {
            getAuthors((Element) nodeLst.item(0), bcol);
        }
    }

    private void populateFromBibtex(BibEntry bibtex) {

        sourceType = getMSBibSourceType(bibtex);

        if (bibtex.hasField(BibEntry.KEY_FIELD)) {
            tag = bibtex.getField(BibEntry.KEY_FIELD);
        }

        if (bibtex.hasField("language")) {
            LCID = getLCID(bibtex.getField("language"));
        }

        if (bibtex.hasField("title")) {
            String temp = bibtex.getField("title");
            // TODO: remove LaTex syntax
            title = new RemoveBrackets().format(temp);
        }
        if (bibtex.hasField("year")) {
            year = bibtex.getField("year");
        }
        if (bibtex.hasField("month")) {
            month = bibtex.getField("month");
        }
        if (bibtex.hasField(MSBIB + "day")) {
            day = bibtex.getField(MSBIB + "day");
        }

        if (bibtex.hasField(MSBIB + "shorttitle")) {
            shortTitle = bibtex.getField(MSBIB + "shorttitle");
        }
        if (bibtex.hasField("note")) {
            comments = bibtex.getField("note");
        }

        if (bibtex.hasField("pages")) {
            pages = new PageNumbers(bibtex.getField("pages"));
        }

        if (bibtex.hasField("volume")) {
            volume = bibtex.getField("volume");
        }

        if (bibtex.hasField(MSBIB + "numberofvolume")) {
            numberOfVolumes = bibtex.getField(MSBIB + "numberofvolume");
        }

        if (bibtex.hasField("edition")) {
            edition = bibtex.getField("edition");
        }

        standardNumber = "";
        if (bibtex.hasField("isbn")) {
            standardNumber += " ISBN: " + bibtex.getField("isbn"); /* SM: 2010.10: lower case */
        }
        if (bibtex.hasField("issn")) {
            standardNumber += " ISSN: " + bibtex.getField("issn"); /* SM: 2010.10: lower case */
        }
        if (bibtex.hasField("lccn")) {
            standardNumber += " LCCN: " + bibtex.getField("lccn"); /* SM: 2010.10: lower case */
        }
        if (bibtex.hasField("mrnumber")) {
            standardNumber += " MRN: " + bibtex.getField("mrnumber");
        }
        /* SM: 2010.10 begin DOI support */
        if (bibtex.hasField("doi")) {
            standardNumber += " DOI: " + bibtex.getField("doi");
        }
        /* SM: 2010.10 end DOI support */
        if (standardNumber.isEmpty()) {
            standardNumber = null;
        }

        if (bibtex.hasField("publisher")) {
            publisher = bibtex.getField("publisher");
        }

        if (bibtex.hasField("address")) {
            address = bibtex.getField("address");
        }

        if (bibtex.hasField("booktitle")) {
            bookTitle = bibtex.getField("booktitle");
        }

        if (bibtex.hasField("chapter")) {
            chapterNumber = bibtex.getField("chapter");
        }

        if (bibtex.hasField("journal")) {
            journalName = bibtex.getField("journal");
        }

        if (bibtex.hasField("number")) {
            issue = bibtex.getField("number");
        }

        if (bibtex.hasField(MSBIB + "periodical")) {
            periodicalTitle = bibtex.getField(MSBIB + "periodical");
        }

        if (bibtex.hasField("booktitle")) {
            conferenceName = bibtex.getField("booktitle");
        }
        if (bibtex.hasField("school")) {
            department = bibtex.getField("school");
        }
        if (bibtex.hasField("institution")) {
            institution = bibtex.getField("institution");
        }

        /* SM: 2010.10 Modified for default source types */
        if (bibtex.hasField("type")) {
            thesisType = bibtex.getField("type");
        } else {
            if ("techreport".equalsIgnoreCase(bibtex.getType())) {
                thesisType = "Tech. rep.";
            } else if ("mastersthesis".equalsIgnoreCase(bibtex.getType())) {
                thesisType = "Master's thesis";
            } else if ("phdthesis".equalsIgnoreCase(bibtex.getType())) {
                thesisType = "Ph.D. dissertation";
            } else if ("unpublished".equalsIgnoreCase(bibtex.getType())) {
                thesisType = "unpublished";
            }
        }

        if (("InternetSite".equals(sourceType) || "DocumentFromInternetSite".equals(sourceType))
                && (bibtex.hasField("title"))) {
            internetSiteTitle = bibtex.getField("title");
        }
        if (bibtex.hasField(MSBIB + "accessed")) {
            dateAccessed = bibtex.getField(MSBIB + "accessed");
        }
        if (bibtex.hasField("url")) {
            url = bibtex.getField("url"); /* SM: 2010.10: lower case */
        }
        if (bibtex.hasField(MSBIB + "productioncompany")) {
            productionCompany = bibtex.getField(MSBIB + "productioncompany");
        }

        if (("ElectronicSource".equals(sourceType)
                || "Art".equals(sourceType)
                || "Misc".equals(sourceType))
                && (bibtex.hasField("title"))) {
            publicationTitle = bibtex.getField("title");
        }
        if (bibtex.hasField(MSBIB + "medium")) {
            medium = bibtex.getField(MSBIB + "medium");
        }
        if ("SoundRecording".equals(sourceType) && (bibtex.hasField("title"))) {
            albumTitle = bibtex.getField("title");
        }
        if (bibtex.hasField(MSBIB + "recordingnumber")) {
            recordingNumber = bibtex.getField(MSBIB + "recordingnumber");
        }
        if (bibtex.hasField(MSBIB + "theater")) {
            theater = bibtex.getField(MSBIB + "theater");
        }
        if (bibtex.hasField(MSBIB + "distributor")) {
            distributor = bibtex.getField(MSBIB + "distributor");
        }
        if ("Interview".equals(sourceType) && (bibtex.hasField("title"))) {
            broadcastTitle = bibtex.getField("title");
        }
        if (bibtex.hasField(MSBIB + "broadcaster")) {
            broadcaster = bibtex.getField(MSBIB + "broadcaster");
        }
        if (bibtex.hasField(MSBIB + "station")) {
            station = bibtex.getField(MSBIB + "station");
        }
        if (bibtex.hasField(MSBIB + "type")) {
            type = bibtex.getField(MSBIB + "type");
        }
        if (bibtex.hasField(MSBIB + "patentnumber")) {
            patentNumber = bibtex.getField(MSBIB + "patentnumber");
        }
        if (bibtex.hasField(MSBIB + "court")) {
            court = bibtex.getField(MSBIB + "court");
        }
        if (bibtex.hasField(MSBIB + "reporter")) {
            reporter = bibtex.getField(MSBIB + "reporter");
        }
        if (bibtex.hasField(MSBIB + "casenumber")) {
            caseNumber = bibtex.getField(MSBIB + "casenumber");
        }
        if (bibtex.hasField(MSBIB + "abbreviatedcasenumber")) {
            abbreviatedCaseNumber = bibtex.getField(MSBIB + "abbreviatedcasenumber");
        }

        if (bibtex.hasField("series")) {
            bibTexSeries = bibtex.getField("series");
        }
        if (bibtex.hasField("abstract")) {
            bibTexAbstract = bibtex.getField("abstract");
        }
        if (bibtex.hasField("keywords")) {
            bibTexKeyWords = bibtex.getField("keywords");
        }
        if (bibtex.hasField("crossref")) {
            bibTexCrossRef = bibtex.getField("crossref");
        }
        if (bibtex.hasField("howpublished")) {
            bibTex_HowPublished = bibtex.getField("howpublished");
        }
        if (bibtex.hasField("affiliation")) {
            bibTexAffiliation = bibtex.getField("affiliation");
        }
        if (bibtex.hasField("contents")) {
            bibTexContents = bibtex.getField("contents");
        }
        if (bibtex.hasField("copyright")) {
            bibTexCopyright = bibtex.getField("copyright");
        }
        if (bibtex.hasField("price")) {
            bibTexPrice = bibtex.getField("price");
        }
        if (bibtex.hasField("size")) {
            bibTexSize = bibtex.getField("size");
        }

        /* SM: 2010.10 end intype, paper support */
        if (bibtex.hasField("intype")) {
            bibTexInType = bibtex.getField("intype");
        }
        if (bibtex.hasField("paper")) {
            bibTexPaper = bibtex.getField("paper");
        }

        if (bibtex.hasField("author")) {
            authors = getAuthors(bibtex.getField("author"));
        }
        if (bibtex.hasField("editor")) {
            editors = getAuthors(bibtex.getField("editor"));
        }

        boolean FORMATXML = false;
        if (FORMATXML) {
            title = format(title);
            bibTexAbstract = format(bibTexAbstract);
        }
    }

    private String format(String value) {
        if (value == null) {
            return null;
        }
        String result;
        LayoutFormatter chars = new XMLChars();
        result = chars.format(value);
        return result;
    }

    // http://www.microsoft.com/globaldev/reference/lcid-all.mspx
    private int getLCID(String language) {
        // TODO: add language to LCID mapping

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

    private String getMSBibSourceType(BibEntry bibtex) {
        String bibtexType = bibtex.getType();

        String result = "Misc";
        if ("book".equalsIgnoreCase(bibtexType)) {
            result = "Book";
        } else if ("inbook".equalsIgnoreCase(bibtexType)) {
            result = "BookSection";
            bibTexEntry = "inbook";
        } /* SM 2010.10: generalized */ else if ("booklet".equalsIgnoreCase(bibtexType)) {
            result = "BookSection";
            bibTexEntry = "booklet";
        } else if ("incollection".equalsIgnoreCase(bibtexType)) {
            result = "BookSection";
            bibTexEntry = "incollection";
        } else if ("article".equalsIgnoreCase(bibtexType)) {
            result = "JournalArticle";
        } else if ("inproceedings".equalsIgnoreCase(bibtexType)) {
            result = "ConferenceProceedings";
            bibTexEntry = "inproceedings";
        } /* SM 2010.10: generalized */ else if ("conference".equalsIgnoreCase(bibtexType)) {
            result = "ConferenceProceedings";
            bibTexEntry = "conference";
        } else if ("proceedings".equalsIgnoreCase(bibtexType)) {
            result = "ConferenceProceedings";
            bibTexEntry = "proceedings";
        } else if ("collection".equalsIgnoreCase(bibtexType)) {
            result = "ConferenceProceedings";
            bibTexEntry = "collection";
        } else if ("techreport".equalsIgnoreCase(bibtexType)) {
            result = "Report";
            bibTexEntry = "techreport";
        } /* SM 2010.10: generalized */ else if ("manual".equalsIgnoreCase(bibtexType)) {
            result = "Report";
            bibTexEntry = "manual";
        } else if ("mastersthesis".equalsIgnoreCase(bibtexType)) {
            result = "Report";
            bibTexEntry = "mastersthesis";
        } else if ("phdthesis".equalsIgnoreCase(bibtexType)) {
            result = "Report";
            bibTexEntry = "phdthesis";
        } else if ("unpublished".equalsIgnoreCase(bibtexType)) {
            result = "Report";
            bibTexEntry = "unpublished";
        } else if ("patent".equalsIgnoreCase(bibtexType)) {
            result = "Patent";
        } else if ("misc".equalsIgnoreCase(bibtexType)) {
            result = "Misc";
        } else if ("electronic".equalsIgnoreCase(bibtexType)) {
            result = "Misc";
            bibTexEntry = "electronic";
        }

        return result;
    }

    private Node getDOMrepresentation() {
        Node result = null;
        try {
            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

            result = getDOMrepresentation(documentBuilder.newDocument());
        } catch (ParserConfigurationException e) {
            LOGGER.warn("Could not create DocumentBuilder", e);
        }
        return result;
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
        if (matcher.matches() && (matcher.groupCount() > 3)) {
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
        if (matcher.matches() && (matcher.groupCount() > 3)) {
            addField(document, parent, "Month" + extra, matcher.group(1));
            addField(document, parent, "Day" + extra, matcher.group(2));
            addField(document, parent, "Year" + extra, matcher.group(3));
        }
    }

    public Element getDOMrepresentation(Document document) {


        Element msbibEntry = document.createElement(B_COLON + "Source");

        addField(document, msbibEntry, "SourceType", sourceType);
        addField(document, msbibEntry, BIBTEX + "Entry", bibTexEntry);

        addField(document, msbibEntry, "Tag", tag);
        addField(document, msbibEntry, "GUID", GUID);
        if (LCID >= 0) {
            addField(document, msbibEntry, "LCID", Integer.toString(LCID));
        }
        addField(document, msbibEntry, "Title", title);
        addField(document, msbibEntry, "Year", year);
        addField(document, msbibEntry, "ShortTitle", shortTitle);
        addField(document, msbibEntry, "Comments", comments);

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

        msbibEntry.appendChild(allAuthors);

        if (pages != null) {
            addField(document, msbibEntry, "Pages", pages.toString("-"));
        }
        addField(document, msbibEntry, "Volume", volume);
        addField(document, msbibEntry, "NumberVolumes", numberOfVolumes);
        addField(document, msbibEntry, "Edition", edition);
        addField(document, msbibEntry, "StandardNumber", standardNumber);
        addField(document, msbibEntry, "Publisher", publisher);

        addAddress(document, msbibEntry, address);

        addField(document, msbibEntry, "BookTitle", bookTitle);
        addField(document, msbibEntry, "ChapterNumber", chapterNumber);

        addField(document, msbibEntry, "JournalName", journalName);
        addField(document, msbibEntry, "Issue", issue);
        addField(document, msbibEntry, "PeriodicalTitle", periodicalTitle);
        addField(document, msbibEntry, "ConferenceName", conferenceName);

        addField(document, msbibEntry, "Department", department);
        addField(document, msbibEntry, "Institution", institution);
        addField(document, msbibEntry, "ThesisType", thesisType);
        addField(document, msbibEntry, "InternetSiteTitle", internetSiteTitle);

        addDate(document, msbibEntry, dateAccessed, "Accessed");

            /* SM 2010.10 added month export */
        addField(document, msbibEntry, "Month", month);

        addField(document, msbibEntry, "URL", url);
        addField(document, msbibEntry, "ProductionCompany", productionCompany);
        addField(document, msbibEntry, "PublicationTitle", publicationTitle);
        addField(document, msbibEntry, "Medium", medium);
        addField(document, msbibEntry, "AlbumTitle", albumTitle);
        addField(document, msbibEntry, "RecordingNumber", recordingNumber);
        addField(document, msbibEntry, "Theater", theater);
        addField(document, msbibEntry, "Distributor", distributor);
        addField(document, msbibEntry, "BroadcastTitle", broadcastTitle);
        addField(document, msbibEntry, "Broadcaster", broadcaster);
        addField(document, msbibEntry, "Station", station);
        addField(document, msbibEntry, "Type", type);
        addField(document, msbibEntry, "PatentNumber", patentNumber);
        addField(document, msbibEntry, "Court", court);
        addField(document, msbibEntry, "Reporter", reporter);
        addField(document, msbibEntry, "CaseNumber", caseNumber);
        addField(document, msbibEntry, "AbbreviatedCaseNumber", abbreviatedCaseNumber);

        addField(document, msbibEntry, BIBTEX + "Series", bibTexSeries);
        addField(document, msbibEntry, BIBTEX + "Abstract", bibTexAbstract);
        addField(document, msbibEntry, BIBTEX + "KeyWords", bibTexKeyWords);
        addField(document, msbibEntry, BIBTEX + "CrossRef", bibTexCrossRef);
        addField(document, msbibEntry, BIBTEX + "HowPublished", bibTex_HowPublished);
        addField(document, msbibEntry, BIBTEX + "Affiliation", bibTexAffiliation);
        addField(document, msbibEntry, BIBTEX + "Contents", bibTexContents);
        addField(document, msbibEntry, BIBTEX + "Copyright", bibTexCopyright);
        addField(document, msbibEntry, BIBTEX + "Price", bibTexPrice);
        addField(document, msbibEntry, BIBTEX + "Size", bibTexSize);

            /* SM: 2010.10 end intype, paper support */
        addField(document, msbibEntry, BIBTEX + "InType", bibTexInType);
        addField(document, msbibEntry, BIBTEX + "Paper", bibTexPaper);

        return msbibEntry;
    }

    private void parseSingleStandardNumber(String type, String bibtype, String standardNum, Map<String, String> map) {
        // tested using http://www.javaregex.com/test.html
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
        /* SM: 2010.10 begin DOI support */
        parseSingleStandardNumber("DOI", "doi", standardNum, map);
        /* SM: 2010.10 end DOI support */
    }

    private void addAuthor(Map<String, String> map, String type, List<PersonName> authors) {
        if (authors == null) {
            return;
        }
        String allAuthors = authors.stream().map(name -> name.getFullname()).collect(Collectors.joining(" and "));

        map.put(type, allAuthors);
    }

    private EntryType mapMSBibToBibtexType(String msbib) {
        EntryType bibtex;
        switch (msbib) {
        case "Book":
            bibtex = BibtexEntryTypes.BOOK;
            break;
        case "BookSection":
            bibtex = BibtexEntryTypes.INBOOK;
            break;
        case "JournalArticle":
        case "ArticleInAPeriodical":
            bibtex = BibtexEntryTypes.ARTICLE;
            break;
        case "ConferenceProceedings":
            bibtex = BibtexEntryTypes.CONFERENCE;
            break;
        case "Report":
            bibtex = BibtexEntryTypes.TECHREPORT;
            break;
        case "InternetSite":
        case "DocumentFromInternetSite":
        case "ElectronicSource":
        case "Art":
        case "SoundRecording":
        case "Performance":
        case "Film":
        case "Interview":
        case "Patent":
        case "Case":
        default:
            bibtex = BibtexEntryTypes.MISC;
            break;
        }

        return bibtex;
    }

    public BibEntry getBibtexRepresentation() {

        BibEntry entry;
        if (tag == null) {
            entry = new BibEntry(ImportFormat.DEFAULT_BIBTEXENTRY_ID, mapMSBibToBibtexType(sourceType).getName());
        } else {
            entry = new BibEntry(tag, mapMSBibToBibtexType(sourceType).getName()); // id assumes an existing database so don't
        }

        // Todo: add check for BibTexEntry types

        HashMap<String, String> hm = new HashMap<>();

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
            hm.put(MSBIB + "shorttitle", shortTitle);
        }
        if (comments != null) {
            hm.put("note", comments);
        }

        addAuthor(hm, "author", authors);
        addAuthor(hm, MSBIB + "bookauthor", bookAuthors);
        addAuthor(hm, "editor", editors);
        addAuthor(hm, MSBIB + "translator", translators);
        addAuthor(hm, MSBIB + "producername", producerNames);
        addAuthor(hm, MSBIB + "composer", composers);
        addAuthor(hm, MSBIB + "conductor", conductors);
        addAuthor(hm, MSBIB + "performer", performers);
        addAuthor(hm, MSBIB + "writer", writers);
        addAuthor(hm, MSBIB + "director", directors);
        addAuthor(hm, MSBIB + "compiler", compilers);
        addAuthor(hm, MSBIB + "interviewer", interviewers);
        addAuthor(hm, MSBIB + "interviewee", interviewees);
        addAuthor(hm, MSBIB + "inventor", inventors);
        addAuthor(hm, MSBIB + "counsel", counsels);

        if (pages != null) {
            hm.put("pages", pages.toString("--"));
        }
        if (volume != null) {
            hm.put("volume", volume);
        }
        if (numberOfVolumes != null) {
            hm.put(MSBIB + "numberofvolume", numberOfVolumes);
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
            hm.put(MSBIB + "accessed", dateAccessed);
        }
        if (url != null) {
            hm.put("url", url);
        }
        if (productionCompany != null) {
            hm.put(MSBIB + "productioncompany", productionCompany);
        }

        if (medium != null) {
            hm.put(MSBIB + "medium", medium);
        }

        if (recordingNumber != null) {
            hm.put(MSBIB + "recordingnumber", recordingNumber);
        }
        if (theater != null) {
            hm.put(MSBIB + "theater", theater);
        }
        if (distributor != null) {
            hm.put(MSBIB + "distributor", distributor);
        }

        if (broadcaster != null) {
            hm.put(MSBIB + "broadcaster", broadcaster);
        }
        if (station != null) {
            hm.put(MSBIB + "station", station);
        }
        if (type != null) {
            hm.put(MSBIB + "type", type);
        }
        if (patentNumber != null) {
            hm.put(MSBIB + "patentnumber", patentNumber);
        }
        if (court != null) {
            hm.put(MSBIB + "court", court);
        }
        if (reporter != null) {
            hm.put(MSBIB + "reporter", reporter);
        }
        if (caseNumber != null) {
            hm.put(MSBIB + "casenumber", caseNumber);
        }
        if (abbreviatedCaseNumber != null) {
            hm.put(MSBIB + "abbreviatedcasenumber", abbreviatedCaseNumber);
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

    /*
     * render as XML
     *
     * TODO This is untested.
     */
    @Override
    public String toString() {
        StringWriter result = new StringWriter();
        try {
            DOMSource source = new DOMSource(getDOMrepresentation());
            StreamResult streamResult = new StreamResult(result);
            Transformer trans = TransformerFactory.newInstance().newTransformer();
            trans.setOutputProperty(OutputKeys.INDENT, "yes");
            trans.transform(source, streamResult);
        } catch (TransformerException e) {
            LOGGER.warn("Could not build XML representation", e);
        }
        return result.toString();
    }

}

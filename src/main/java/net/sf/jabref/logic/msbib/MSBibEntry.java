/*  Copyright (C) 2003-2011 JabRef contributors.
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.sf.jabref.importer.fileformat.ImportFormat;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.model.entry.BibtexEntryTypes;
import net.sf.jabref.exporter.layout.LayoutFormatter;
import net.sf.jabref.exporter.layout.format.XMLChars;
import net.sf.jabref.logic.mods.PageNumbers;
import net.sf.jabref.logic.mods.PersonName;

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
 */
class MSBibEntry {

    private static final Log LOGGER = LogFactory.getLog(MSBibEntry.class);

    private String sourceType = "Misc";
    private String bibTexEntry;

    private String tag;
    private final String GUID = null;
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
    private String bibTex_Series;
    private String bibTex_Abstract;
    private String bibTex_KeyWords;
    private String bibTex_CrossRef;
    private String bibTex_HowPublished;
    private String bibTex_Affiliation;
    private String bibTex_Contents;
    private String bibTex_Copyright;
    private String bibTex_Price;
    private String bibTex_Size;

    /* SM 2010.10 intype, paper support */
    private String bibTex_InType;
    private String bibTex_Paper;

    private final String BIBTEX = "BIBTEX_";
    private final String MSBIB = "msbib-";

    private final String bcol = "b:";


    private MSBibEntry() {
    }

    public MSBibEntry(BibtexEntry bibtex) {
        this();
        populateFromBibtex(bibtex);
    }

    public MSBibEntry(Element entry, String bcol) {
        this();
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
        String temp = null;

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
        address = "";
        if (city != null) {
            address += city + ", ";
        }
        if (state != null) {
            address += state + ' ';
        }
        if (country != null) {
            address += country;
        }
        address = address.trim();
        if (address.isEmpty() || address.equals(",")) {
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
        if (dateAccessed.isEmpty() || dateAccessed.equals(",")) {
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
        bibTex_Series = getFromXml(bcol + BIBTEX + "Series", entry);
        bibTex_Abstract = getFromXml(bcol + BIBTEX + "Abstract", entry);
        bibTex_KeyWords = getFromXml(bcol + BIBTEX + "KeyWords", entry);
        bibTex_CrossRef = getFromXml(bcol + BIBTEX + "CrossRef", entry);
        bibTex_HowPublished = getFromXml(bcol + BIBTEX + "HowPublished", entry);
        bibTex_Affiliation = getFromXml(bcol + BIBTEX + "Affiliation", entry);
        bibTex_Contents = getFromXml(bcol + BIBTEX + "Contents", entry);
        bibTex_Copyright = getFromXml(bcol + BIBTEX + "Copyright", entry);
        bibTex_Price = getFromXml(bcol + BIBTEX + "Price", entry);
        bibTex_Size = getFromXml(bcol + BIBTEX + "Size", entry);

        NodeList nodeLst = entry.getElementsByTagName(bcol + "Author");
        if (nodeLst.getLength() > 0) {
            getAuthors((Element) nodeLst.item(0), bcol);
        }
    }

    private void populateFromBibtex(BibtexEntry bibtex) {

        sourceType = getMSBibSourceType(bibtex);

        if (bibtex.getField("bibtexkey") != null) {
            tag = bibtex.getField("bibtexkey");
        }

        if (bibtex.getField("language") != null) {
            LCID = getLCID(bibtex.getField("language"));
        }

        if (bibtex.getField("title") != null) {
            title = bibtex.getField("title");
        }
        if (bibtex.getField("year") != null) {
            year = bibtex.getField("year");
        }
        if (bibtex.getField("month") != null) {
            month = bibtex.getField("month");
        }
        if (bibtex.getField(MSBIB + "day") != null) {
            day = bibtex.getField(MSBIB + "day");
        }

        if (bibtex.getField(MSBIB + "shorttitle") != null) {
            shortTitle = bibtex.getField(MSBIB + "shorttitle");
        }
        if (bibtex.getField("note") != null) {
            comments = bibtex.getField("note");
        }

        if (bibtex.getField("pages") != null) {
            pages = new PageNumbers(bibtex.getField("pages"));
        }

        if (bibtex.getField("volume") != null) {
            volume = bibtex.getField("volume");
        }

        if (bibtex.getField(MSBIB + "numberofvolume") != null) {
            numberOfVolumes = bibtex.getField(MSBIB + "numberofvolume");
        }

        if (bibtex.getField("edition") != null) {
            edition = bibtex.getField("edition");
        }

        standardNumber = "";
        if (bibtex.getField("isbn") != null) {
            standardNumber += " ISBN: " + bibtex.getField("isbn"); /* SM: 2010.10: lower case */
        }
        if (bibtex.getField("issn") != null) {
            standardNumber += " ISSN: " + bibtex.getField("issn"); /* SM: 2010.10: lower case */
        }
        if (bibtex.getField("lccn") != null) {
            standardNumber += " LCCN: " + bibtex.getField("lccn"); /* SM: 2010.10: lower case */
        }
        if (bibtex.getField("mrnumber") != null) {
            standardNumber += " MRN: " + bibtex.getField("mrnumber");
        }
        /* SM: 2010.10 begin DOI support */
        if (bibtex.getField("doi") != null) {
            standardNumber += " DOI: " + bibtex.getField("doi");
        }
        /* SM: 2010.10 end DOI support */
        if (standardNumber.isEmpty()) {
            standardNumber = null;
        }

        if (bibtex.getField("publisher") != null) {
            publisher = bibtex.getField("publisher");
        }

        if (bibtex.getField("address") != null) {
            address = bibtex.getField("address");
        }

        if (bibtex.getField("booktitle") != null) {
            bookTitle = bibtex.getField("booktitle");
        }

        if (bibtex.getField("chapter") != null) {
            chapterNumber = bibtex.getField("chapter");
        }

        if (bibtex.getField("journal") != null) {
            journalName = bibtex.getField("journal");
        }

        if (bibtex.getField("number") != null) {
            issue = bibtex.getField("number");
        }

        if (bibtex.getField(MSBIB + "periodical") != null) {
            periodicalTitle = bibtex.getField(MSBIB + "periodical");
        }

        if (bibtex.getField("booktitle") != null) {
            conferenceName = bibtex.getField("booktitle");
        }
        if (bibtex.getField("school") != null) {
            department = bibtex.getField("school");
        }
        if (bibtex.getField("institution") != null) {
            institution = bibtex.getField("institution");
        }

        /* SM: 2010.10 Modified for default source types */
        if (bibtex.getField("type") != null) {
            thesisType = bibtex.getField("type");
        } else {
            if (bibtex.getType().getName().equalsIgnoreCase("techreport")) {
                thesisType = "Tech. rep.";
            } else if (bibtex.getType().getName().equalsIgnoreCase("mastersthesis")) {
                thesisType = "Master's thesis";
            } else if (bibtex.getType().getName().equalsIgnoreCase("phdthesis")) {
                thesisType = "Ph.D. dissertation";
            } else if (bibtex.getType().getName().equalsIgnoreCase("unpublished")) {
                thesisType = "unpublished";
            }
        }

        if ((sourceType.equals("InternetSite") || sourceType.equals("DocumentFromInternetSite"))
                && bibtex.getField("title") != null) {
            internetSiteTitle = bibtex.getField("title");
        }
        if (bibtex.getField(MSBIB + "accessed") != null) {
            dateAccessed = bibtex.getField(MSBIB + "accessed");
        }
        if (bibtex.getField("url") != null) {
            url = bibtex.getField("url"); /* SM: 2010.10: lower case */
        }
        if (bibtex.getField(MSBIB + "productioncompany") != null) {
            productionCompany = bibtex.getField(MSBIB + "productioncompany");
        }

        if ((sourceType.equals("ElectronicSource")
                || sourceType.equals("Art")
                || sourceType.equals("Misc"))
                && bibtex.getField("title") != null) {
            publicationTitle = bibtex.getField("title");
        }
        if (bibtex.getField(MSBIB + "medium") != null) {
            medium = bibtex.getField(MSBIB + "medium");
        }
        if (sourceType.equals("SoundRecording") && bibtex.getField("title") != null) {
            albumTitle = bibtex.getField("title");
        }
        if (bibtex.getField(MSBIB + "recordingnumber") != null) {
            recordingNumber = bibtex.getField(MSBIB + "recordingnumber");
        }
        if (bibtex.getField(MSBIB + "theater") != null) {
            theater = bibtex.getField(MSBIB + "theater");
        }
        if (bibtex.getField(MSBIB + "distributor") != null) {
            distributor = bibtex.getField(MSBIB + "distributor");
        }
        if (sourceType.equals("Interview") && bibtex.getField("title") != null) {
            broadcastTitle = bibtex.getField("title");
        }
        if (bibtex.getField(MSBIB + "broadcaster") != null) {
            broadcaster = bibtex.getField(MSBIB + "broadcaster");
        }
        if (bibtex.getField(MSBIB + "station") != null) {
            station = bibtex.getField(MSBIB + "station");
        }
        if (bibtex.getField(MSBIB + "type") != null) {
            type = bibtex.getField(MSBIB + "type");
        }
        if (bibtex.getField(MSBIB + "patentnumber") != null) {
            patentNumber = bibtex.getField(MSBIB + "patentnumber");
        }
        if (bibtex.getField(MSBIB + "court") != null) {
            court = bibtex.getField(MSBIB + "court");
        }
        if (bibtex.getField(MSBIB + "reporter") != null) {
            reporter = bibtex.getField(MSBIB + "reporter");
        }
        if (bibtex.getField(MSBIB + "casenumber") != null) {
            caseNumber = bibtex.getField(MSBIB + "casenumber");
        }
        if (bibtex.getField(MSBIB + "abbreviatedcasenumber") != null) {
            abbreviatedCaseNumber = bibtex.getField(MSBIB + "abbreviatedcasenumber");
        }

        if (bibtex.getField("series") != null) {
            bibTex_Series = bibtex.getField("series");
        }
        if (bibtex.getField("abstract") != null) {
            bibTex_Abstract = bibtex.getField("abstract");
        }
        if (bibtex.getField("keywords") != null) {
            bibTex_KeyWords = bibtex.getField("keywords");
        }
        if (bibtex.getField("crossref") != null) {
            bibTex_CrossRef = bibtex.getField("crossref");
        }
        if (bibtex.getField("howpublished") != null) {
            bibTex_HowPublished = bibtex.getField("howpublished");
        }
        if (bibtex.getField("affiliation") != null) {
            bibTex_Affiliation = bibtex.getField("affiliation");
        }
        if (bibtex.getField("contents") != null) {
            bibTex_Contents = bibtex.getField("contents");
        }
        if (bibtex.getField("copyright") != null) {
            bibTex_Copyright = bibtex.getField("copyright");
        }
        if (bibtex.getField("price") != null) {
            bibTex_Price = bibtex.getField("price");
        }
        if (bibtex.getField("size") != null) {
            bibTex_Size = bibtex.getField("size");
        }

        /* SM: 2010.10 end intype, paper support */
        if (bibtex.getField("intype") != null) {
            bibTex_InType = bibtex.getField("intype");
        }
        if (bibtex.getField("paper") != null) {
            bibTex_Paper = bibtex.getField("paper");
        }

        if (bibtex.getField("author") != null) {
            authors = getAuthors(bibtex.getField("author"));
        }
        if (bibtex.getField("editor") != null) {
            editors = getAuthors(bibtex.getField("editor"));
        }

        boolean FORMATXML = false;
        if (FORMATXML) {
            title = format(title);
            bibTex_Abstract = format(bibTex_Abstract);
        }
    }

    private String format(String value) {
        if (value == null) {
            return null;
        }
        String result = null;
        LayoutFormatter chars = new XMLChars();
        result = chars.format(value);
        return result;
    }

    // http://www.microsoft.com/globaldev/reference/lcid-all.mspx
    private int getLCID(String language) {
        // TODO: add lanaguage to LCID mapping

        return 0;
    }

    // http://www.microsoft.com/globaldev/reference/lcid-all.mspx
    private String getLanguage(int LCID) {
        // TODO: add lanaguage to LCID mapping

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

        if (!authors.contains(" and ")) {
            result.add(new PersonName(authors));
        } else {
            String[] names = authors.split(" and ");
            for (String name : names) {
                result.add(new PersonName(name));
            }
        }
        return result;
    }

    /* construct a MSBib date object */
    protected String getDate(BibtexEntry bibtex) {
        String result = "";
        if (bibtex.getField("year") != null) {
            result += bibtex.getField("year");
        }
        if (bibtex.getField("month") != null) {
            result += '-' + bibtex.getField("month");
        }

        return result;
    }

    private String getMSBibSourceType(BibtexEntry bibtex) {
        String bibtexType = bibtex.getType().getName();

        String result = "Misc";
        if (bibtexType.equalsIgnoreCase("book")) {
            result = "Book";
        } else if (bibtexType.equalsIgnoreCase("inbook")) {
            result = "BookSection";
            bibTexEntry = "inbook";
        } /* SM 2010.10: generalized */ else if (bibtexType.equalsIgnoreCase("booklet")) {
            result = "BookSection";
            bibTexEntry = "booklet";
        } else if (bibtexType.equalsIgnoreCase("incollection")) {
            result = "BookSection";
            bibTexEntry = "incollection";
        } else if (bibtexType.equalsIgnoreCase("article")) {
            result = "JournalArticle";
        } else if (bibtexType.equalsIgnoreCase("inproceedings")) {
            result = "ConferenceProceedings";
            bibTexEntry = "inproceedings";
        } /* SM 2010.10: generalized */ else if (bibtexType.equalsIgnoreCase("conference")) {
            result = "ConferenceProceedings";
            bibTexEntry = "conference";
        } else if (bibtexType.equalsIgnoreCase("proceedings")) {
            result = "ConferenceProceedings";
            bibTexEntry = "proceedings";
        } else if (bibtexType.equalsIgnoreCase("collection")) {
            result = "ConferenceProceedings";
            bibTexEntry = "collection";
        } else if (bibtexType.equalsIgnoreCase("techreport")) {
            result = "Report";
            bibTexEntry = "techreport";
        } /* SM 2010.10: generalized */ else if (bibtexType.equalsIgnoreCase("manual")) {
            result = "Report";
            bibTexEntry = "manual";
        } else if (bibtexType.equalsIgnoreCase("mastersthesis")) {
            result = "Report";
            bibTexEntry = "mastersthesis";
        } else if (bibtexType.equalsIgnoreCase("phdthesis")) {
            result = "Report";
            bibTexEntry = "phdthesis";
        } else if (bibtexType.equalsIgnoreCase("unpublished")) {
            result = "Report";
            bibTexEntry = "unpublished";
        } else if (bibtexType.equalsIgnoreCase("patent")) {
            result = "Patent";
        } else if (bibtexType.equalsIgnoreCase("misc")) {
            result = "Misc";
        } else if (bibtexType.equalsIgnoreCase("electronic")) {
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
        Element elem = document.createElement(bcol + name);
        elem.appendChild(document.createTextNode(stripNonValidXMLCharacters(value)));
        parent.appendChild(elem);
    }

    private void addAuthor(Document document, Element allAuthors, String entryName, List<PersonName> authorsLst) {
        if (authorsLst == null) {
            return;
        }
        Element authorTop = document.createElement(bcol + entryName);
        Element nameList = document.createElement(bcol + "NameList");
        for (PersonName name : authorsLst) {
            Element person = document.createElement(bcol + "Person");
            addField(document, person, "Last", name.getSurname());
            addField(document, person, "Middle", name.getMiddlename());
            addField(document, person, "First", name.getFirstname());
            nameList.appendChild(person);
        }
        authorTop.appendChild(nameList);

        allAuthors.appendChild(authorTop);
    }

    private void addAdrress(Document document, Element parent, String address) {
        if (address == null) {
            return;
        }

        // reduced subset, supports only "CITY , STATE, COUNTRY"
        // \b(\w+)\s?[,]?\s?(\w+)\s?[,]?\s?(\w+)\b
        // WORD SPACE , SPACE WORD SPACE , SPACE WORD
        // tested using http://www.javaregex.com/test.html
        Pattern pattern = Pattern.compile("\\b(\\w+)\\s*[,]?\\s*(\\w+)\\s*[,]?\\s*(\\w+)\\b");
        Matcher matcher = pattern.matcher(address);
        if (matcher.matches() && matcher.groupCount() > 3) {
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

        // Allows 20.3-2007|||20/3-  2007 etc. 
        // (\d{1,2})\s?[.,-/]\s?(\d{1,2})\s?[.,-/]\s?(\d{2,4})
        // 1-2 DIGITS SPACE SEPERATOR SPACE 1-2 DIGITS SPACE SEPERATOR SPACE 2-4 DIGITS
        // tested using http://www.javaregex.com/test.html
        Pattern pattern = Pattern.compile("(\\d{1,2})\\s*[.,-/]\\s*(\\d{1,2})\\s*[.,-/]\\s*(\\d{2,4})");
        Matcher matcher = pattern.matcher(date);
        if (matcher.matches() && matcher.groupCount() > 3) {
            addField(document, parent, "Month" + extra, matcher.group(1));
            addField(document, parent, "Day" + extra, matcher.group(2));
            addField(document, parent, "Year" + extra, matcher.group(3));
        }
    }

    public Element getDOMrepresentation(Document document) {


        Element msbibEntry = document.createElement(bcol + "Source");

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

        Element allAuthors = document.createElement(bcol + "Author");

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

        addAdrress(document, msbibEntry, address);

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

        addField(document, msbibEntry, BIBTEX + "Series", bibTex_Series);
        addField(document, msbibEntry, BIBTEX + "Abstract", bibTex_Abstract);
        addField(document, msbibEntry, BIBTEX + "KeyWords", bibTex_KeyWords);
        addField(document, msbibEntry, BIBTEX + "CrossRef", bibTex_CrossRef);
        addField(document, msbibEntry, BIBTEX + "HowPublished", bibTex_HowPublished);
        addField(document, msbibEntry, BIBTEX + "Affiliation", bibTex_Affiliation);
        addField(document, msbibEntry, BIBTEX + "Contents", bibTex_Contents);
        addField(document, msbibEntry, BIBTEX + "Copyright", bibTex_Copyright);
        addField(document, msbibEntry, BIBTEX + "Price", bibTex_Price);
        addField(document, msbibEntry, BIBTEX + "Size", bibTex_Size);

            /* SM: 2010.10 end intype, paper support */
        addField(document, msbibEntry, BIBTEX + "InType", bibTex_InType);
        addField(document, msbibEntry, BIBTEX + "Paper", bibTex_Paper);

        return msbibEntry;
    }

    private void parseSingleStandardNumber(String type, String bibtype, String standardNum, HashMap<String, String> map) {
        // tested using http://www.javaregex.com/test.html
        Pattern pattern = Pattern.compile(':' + type + ":(.[^:]+)");
        Matcher matcher = pattern.matcher(standardNum);
        if (matcher.matches()) {
            map.put(bibtype, matcher.group(1));
        }
    }

    private void parseStandardNumber(String standardNum, HashMap<String, String> map) {
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

    private void addAuthor(HashMap<String, String> map, String type, List<PersonName> authors) {
        if (authors == null) {
            return;
        }
        String allAuthors = "";
        boolean First = true;
        for (PersonName name : authors) {
            if (!First) {
                allAuthors += " and ";
            }
            allAuthors += name.getFullname();
            First = false;
        }
        map.put(type, allAuthors);
    }

    private EntryType mapMSBibToBibtexType(String msbib) {
        EntryType bibtex;
        if (msbib.equals("Book")) {
            bibtex = BibtexEntryTypes.BOOK;
        } else if (msbib.equals("BookSection")) {
            bibtex = BibtexEntryTypes.INBOOK;
        } else if (msbib.equals("JournalArticle") || msbib.equals("ArticleInAPeriodical")) {
            bibtex = BibtexEntryTypes.ARTICLE;
        } else if (msbib.equals("ConferenceProceedings")) {
            bibtex = BibtexEntryTypes.CONFERENCE;
        } else if (msbib.equals("Report")) {
            bibtex = BibtexEntryTypes.TECHREPORT;
        } else if (msbib.equals("InternetSite") || msbib.equals("DocumentFromInternetSite") || msbib.equals("ElectronicSource") || msbib.equals("Art") || msbib.equals("SoundRecording") || msbib.equals("Performance") || msbib.equals("Film") || msbib.equals("Interview") || msbib.equals("Patent") || msbib.equals("Case")) {
            bibtex = BibtexEntryTypes.MISC;
        } else {
            bibtex = BibtexEntryTypes.MISC;
        }

        return bibtex;
    }

    public BibtexEntry getBibtexRepresentation() {

        BibtexEntry entry = null;
        if (tag == null) {
            entry = new BibtexEntry(ImportFormat.DEFAULT_BIBTEXENTRY_ID,
                    mapMSBibToBibtexType(sourceType));
        } else {
            entry = new BibtexEntry(tag,
                    mapMSBibToBibtexType(sourceType)); // id assumes an existing database so don't
        }

        // Todo: add check for BibTexEntry types

        HashMap<String, String> hm = new HashMap<>();

        if (tag != null) {
            hm.put("bibtexkey", tag);
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

        if (bibTex_Series != null) {
            hm.put("series", bibTex_Series);
        }
        if (bibTex_Abstract != null) {
            hm.put("abstract", bibTex_Abstract);
        }
        if (bibTex_KeyWords != null) {
            hm.put("keywords", bibTex_KeyWords);
        }
        if (bibTex_CrossRef != null) {
            hm.put("crossref", bibTex_CrossRef);
        }
        if (bibTex_HowPublished != null) {
            hm.put("howpublished", bibTex_HowPublished);
        }
        if (bibTex_Affiliation != null) {
            hm.put("affiliation", bibTex_Affiliation);
        }
        if (bibTex_Contents != null) {
            hm.put("contents", bibTex_Contents);
        }
        if (bibTex_Copyright != null) {
            hm.put("copyright", bibTex_Copyright);
        }
        if (bibTex_Price != null) {
            hm.put("price", bibTex_Price);
        }
        if (bibTex_Size != null) {
            hm.put("size", bibTex_Size);
        }

        entry.setField(hm);
        return entry;
    }

    /**
     * This method ensures that the output String has only
     * valid XML unicode characters as specified by the
     * XML 1.0 standard. For reference, please see
     * <a href="http://www.w3.org/TR/2000/REC-xml-20001006#NT-Char">the
     * standard</a>. This method will return an empty
     * String if the input is null or empty.
     * <p>
     * URL: http://cse-mjmcl.cse.bris.ac.uk/blog/2007/02/14/1171465494443.html
     *
     * @param in The String whose non-valid characters we want to remove.
     * @return The in String, stripped of non-valid characters.
     */
    private String stripNonValidXMLCharacters(String in) {
        StringBuilder out = new StringBuilder(); // Used to hold the output.
        char current; // Used to reference the current character.

        if (in == null || in != null && in.isEmpty()) {
            return ""; // vacancy test.
        }
        for (int i = 0; i < in.length(); i++) {
            current = in.charAt(i); // NOTE: No IndexOutOfBoundsException caught here; it should not happen.
            if (current == 0x9 ||
                    current == 0xA ||
                    current == 0xD ||
                    current >= 0x20 && current <= 0xD7FF ||
                    current >= 0xE000 && current <= 0xFFFD ||
                    current >= 0x10000 && current <= 0x10FFFF) {
                out.append(current);
            }
        }
        return out.toString();
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

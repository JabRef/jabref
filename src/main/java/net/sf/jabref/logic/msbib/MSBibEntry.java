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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.sf.jabref.logic.mods.PageNumbers;
import net.sf.jabref.logic.mods.PersonName;
import net.sf.jabref.logic.util.strings.StringUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
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
    private static final String BIBTEX_PREFIX = "BIBTEX_";
    private static final String MSBIB_PREFIX = "msbib-";

    private static final String B_COLON = "b:";

    // MSBib fields and values
    public Map<String, String> fields = new HashMap<>();

    public String msbibType = "Misc";
    public String bibtexType;
    public String tag;
    public static final String GUID = null;
    public int LCID = -1;
    public List<PersonName> authors;
    public List<PersonName> bookAuthors;
    public List<PersonName> editors;
    public List<PersonName> translators;
    public List<PersonName> producerNames;
    public List<PersonName> composers;
    public List<PersonName> conductors;
    public List<PersonName> performers;
    public List<PersonName> writers;
    public List<PersonName> directors;
    public List<PersonName> compilers;
    public List<PersonName> interviewers;
    public List<PersonName> interviewees;

    public List<PersonName> inventors;
    public List<PersonName> counsels;
    public String title;
    public String year;

    public String month;
    public String day;

    public String shortTitle;
    public String comments;
    public PageNumbers pages;
    public String volume;
    public String numberOfVolumes;
    public String edition;

    public String standardNumber;
    public String publisher;
    public String address;
    public String bookTitle;
    public String chapterNumber;
    public String journalName;
    public String issue;
    public String periodicalTitle;
    public String conferenceName;
    public String department;
    public String institution;
    public String thesisType;
    public String internetSiteTitle;
    public String dateAccessed;
    public String doi;
    public String url;
    public String productionCompany;
    public String publicationTitle;
    public String medium;
    public String albumTitle;
    public String recordingNumber;
    public String theater;
    public String distributor;
    public String broadcastTitle;
    public String broadcaster;
    public String station;
    public String type;
    public String patentNumber;
    public String court;
    public String reporter;
    public String caseNumber;
    public String abbreviatedCaseNumber;
    public String bibTexSeries;
    public String bibTexAbstract;
    public String bibTexKeyWords;
    public String bibTexCrossRef;
    public String bibTexHowPublished;
    public String bibTexAffiliation;
    public String bibTexContents;
    public String bibTexCopyright;
    public String bibTexPrice;
    public String bibTexSize;
    public String bibTexInType;
    public String bibTexPaper;

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

    public MSBibEntry() {

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

        msbibType = getFromXml(bcol + "SourceType", entry);

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
        bibTexHowPublished = getFromXml(bcol + BIBTEX_PREFIX + "HowPublished", entry);
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

    public Element getDOM(Document document) {
        Element rootNode = document.createElement(B_COLON + "Source");

        for (Map.Entry<String, String> entry : fields.entrySet()) {
            addField(document, rootNode, entry.getKey(), entry.getValue());
        }

        // FIXME: old
        // Not based on bibtex content = additional
        addField(document, rootNode, "SourceType", msbibType);
        addField(document, rootNode, BIBTEX_PREFIX + "Entry", bibtexType);
        addField(document, rootNode, "GUID", GUID);

        if (LCID >= 0) {
            addField(document, rootNode, "LCID", Integer.toString(LCID));
        }

        // based on bibtex content
        if (dateAccessed != null) {
            Matcher matcher = DATE_PATTERN.matcher(dateAccessed);
            if (matcher.matches() && (matcher.groupCount() >= 3)) {
                addField(document, rootNode, "Month" + "Accessed", matcher.group(1));
                addField(document, rootNode, "Day" + "Accessed", matcher.group(2));
                addField(document, rootNode, "Year" + "Accessed", matcher.group(3));
            }
        }

        Element allAuthors = document.createElement(B_COLON + "Author");

        addAuthor(document, allAuthors, "Author", authors);
        addAuthor(document, allAuthors, "BookAuthor", bookAuthors);
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

    // http://www.microsoft.com/globaldev/reference/lcid-all.mspx
    public int getLCID(String language) {
        // TODO: add language to LCID mapping
        // 0 is English
        return 0;
    }

    // http://www.microsoft.com/globaldev/reference/lcid-all.mspx
    public String getLanguage(int LCID) {
        // TODO: add language to LCID mapping
        return "english";
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
}

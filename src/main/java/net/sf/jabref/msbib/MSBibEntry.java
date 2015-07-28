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
package net.sf.jabref.msbib;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexEntryType;
import net.sf.jabref.BibtexFields;
import net.sf.jabref.export.layout.LayoutFormatter;
import net.sf.jabref.export.layout.format.XMLChars;
import net.sf.jabref.mods.PageNumbers;
import net.sf.jabref.mods.PersonName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Date: May 15, 2007; May 03, 2007
 *
 * History
 * May 03, 2007 - Added export functionality
 * May 15, 2007 - Added import functionality
 * May 16, 2007 - Changed all interger entries to strings,
 * 				  except LCID which must be an integer.
 * 				  To avoid exception during integer parsing
 *				  the exception is caught and LCID is set to zero.
 * Jan 06, 2012 - Changed the XML element ConferenceName to present
 * 				  the Booktitle instead of the organization field content
 *
 * @author S M Mahbub Murshed (udvranto@yahoo.com)
 * @version 2.0.0
 * @see <a href="http://mahbub.wordpress.com/2007/03/24/details-of-microsoft-office-2007-bibliographic-format-compared-to-bibtex/">ms office 2007 bibliography format compared to bibtex</a>
 * @see <a href="http://mahbub.wordpress.com/2007/03/22/deciphering-microsoft-office-2007-bibliography-format/">deciphering ms office 2007 bibliography format</a>
 */
class MSBibEntry {

    private String sourceType = "Misc";
    private String bibTexEntry = null;

    private String tag = null;
    private final String GUID = null;
    private int LCID = -1;

    private List<PersonName> authors = null;
    private List<PersonName> bookAuthors = null;
    private List<PersonName> editors = null;
    private List<PersonName> translators = null;
    private List<PersonName> producerNames = null;
    private List<PersonName> composers = null;
    private List<PersonName> conductors = null;
    private List<PersonName> performers = null;
    private List<PersonName> writers = null;
    private List<PersonName> directors = null;
    private List<PersonName> compilers = null;
    private List<PersonName> interviewers = null;
    private List<PersonName> interviewees = null;
    private List<PersonName> inventors = null;
    private List<PersonName> counsels = null;

    private String title = null;
    private String year = null;
    private String month = null;
    private String day = null;

    private String shortTitle = null;
    private String comments = null;

    private PageNumbers pages = null;
    private String volume = null;
    private String numberOfVolumes = null;
    private String edition = null;
    private String standardNumber = null;
    private String publisher = null;

    private String address = null;
    private String bookTitle = null;
    private String chapterNumber = null;
    private String journalName = null;
    private String issue = null;
    private String periodicalTitle = null;
    private String conferenceName = null;
    private String department = null;
    private String institution = null;
    private String thesisType = null;
    private String internetSiteTitle = null;
    private String dateAccessed = null;
    private String url = null;
    private String productionCompany = null;
    private String publicationTitle = null;
    private String medium = null;
    private String albumTitle = null;
    private String recordingNumber = null;
    private String theater = null;
    private String distributor = null;
    private String broadcastTitle = null;
    private String broadcaster = null;
    private String station = null;
    private String type = null;
    private String patentNumber = null;
    private String court = null;
    private String reporter = null;
    private String caseNumber = null;
    private String abbreviatedCaseNumber = null;
    private String bibTex_Series = null;
    private String bibTex_Abstract = null;
    private String bibTex_KeyWords = null;
    private String bibTex_CrossRef = null;
    private String bibTex_HowPublished = null;
    private String bibTex_Affiliation = null;
    private String bibTex_Contents = null;
    private String bibTex_Copyright = null;
    private String bibTex_Price = null;
    private String bibTex_Size = null;

    /* SM 2010.10 intype, paper support */
    private String bibTex_InType = null;
    private String bibTex_Paper = null;

    private final String BIBTEX = "BIBTEX_";
    private final String MSBIB = "msbib-";

    private final String bcol = "b:";


    private MSBibEntry() {
    }

    public MSBibEntry(BibtexEntry bibtex) {
        this();
        populateFromBibtex(bibtex);
    }

    public MSBibEntry(Element entry, String _bcol) {
        this();
        populateFromXml(entry, _bcol);
    }

    private String getFromXml(String name, Element entry) {
        String value = null;
        NodeList nodeLst = entry.getElementsByTagName(name);
        if (nodeLst.getLength() > 0) {
            value = nodeLst.item(0).getTextContent();
        }
        return value;
    }

    private void populateFromXml(Element entry, String _bcol) {
        String temp = null;

        sourceType = getFromXml(_bcol + "SourceType", entry);

        tag = getFromXml(_bcol + "Tag", entry);

        temp = getFromXml(_bcol + "LCID", entry);
        if (temp != null)
        {
            try {
                LCID = Integer.parseInt(temp);
            } catch (Exception e) {
                LCID = -1;
            }
        }

        title = getFromXml(_bcol + "Title", entry);
        year = getFromXml(_bcol + "Year", entry);
        month = getFromXml(_bcol + "Month", entry);
        day = getFromXml(_bcol + "Day", entry);

        shortTitle = getFromXml(_bcol + "ShortTitle", entry);
        comments = getFromXml(_bcol + "Comments", entry);

        temp = getFromXml(_bcol + "Pages", entry);
        if (temp != null) {
            pages = new PageNumbers(temp);
        }

        volume = getFromXml(_bcol + "Volume", entry);

        numberOfVolumes = getFromXml(_bcol + "NumberVolumes", entry);

        edition = getFromXml(_bcol + "Edition", entry);

        standardNumber = getFromXml(_bcol + "StandardNumber", entry);

        publisher = getFromXml(_bcol + "Publisher", entry);

        String city = getFromXml(_bcol + "City", entry);
        String state = getFromXml(_bcol + "StateProvince", entry);
        String country = getFromXml(_bcol + "CountryRegion", entry);
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

        bookTitle = getFromXml(_bcol + "BookTitle", entry);

        chapterNumber = getFromXml(_bcol + "ChapterNumber", entry);

        journalName = getFromXml(_bcol + "JournalName", entry);

        issue = getFromXml(_bcol + "Issue", entry);

        periodicalTitle = getFromXml(_bcol + "PeriodicalTitle", entry);

        conferenceName = getFromXml(_bcol + "ConferenceName", entry);
        department = getFromXml(_bcol + "Department", entry);
        institution = getFromXml(_bcol + "Institution", entry);

        thesisType = getFromXml(_bcol + "ThesisType", entry);
        internetSiteTitle = getFromXml(_bcol + "InternetSiteTitle", entry);
        String month = getFromXml(_bcol + "MonthAccessed", entry);
        String day = getFromXml(_bcol + "DayAccessed", entry);
        String year = getFromXml(_bcol + "YearAccessed", entry);
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

        url = getFromXml(_bcol + "URL", entry);
        productionCompany = getFromXml(_bcol + "ProductionCompany", entry);

        publicationTitle = getFromXml(_bcol + "PublicationTitle", entry);
        medium = getFromXml(_bcol + "Medium", entry);
        albumTitle = getFromXml(_bcol + "AlbumTitle", entry);
        recordingNumber = getFromXml(_bcol + "RecordingNumber", entry);
        theater = getFromXml(_bcol + "Theater", entry);
        distributor = getFromXml(_bcol + "Distributor", entry);
        broadcastTitle = getFromXml(_bcol + "BroadcastTitle", entry);
        broadcaster = getFromXml(_bcol + "Broadcaster", entry);
        station = getFromXml(_bcol + "Station", entry);
        type = getFromXml(_bcol + "Type", entry);
        patentNumber = getFromXml(_bcol + "PatentNumber", entry);
        court = getFromXml(_bcol + "Court", entry);
        reporter = getFromXml(_bcol + "Reporter", entry);
        caseNumber = getFromXml(_bcol + "CaseNumber", entry);
        abbreviatedCaseNumber = getFromXml(_bcol + "AbbreviatedCaseNumber", entry);
        bibTex_Series = getFromXml(_bcol + BIBTEX + "Series", entry);
        bibTex_Abstract = getFromXml(_bcol + BIBTEX + "Abstract", entry);
        bibTex_KeyWords = getFromXml(_bcol + BIBTEX + "KeyWords", entry);
        bibTex_CrossRef = getFromXml(_bcol + BIBTEX + "CrossRef", entry);
        bibTex_HowPublished = getFromXml(_bcol + BIBTEX + "HowPublished", entry);
        bibTex_Affiliation = getFromXml(_bcol + BIBTEX + "Affiliation", entry);
        bibTex_Contents = getFromXml(_bcol + BIBTEX + "Contents", entry);
        bibTex_Copyright = getFromXml(_bcol + BIBTEX + "Copyright", entry);
        bibTex_Price = getFromXml(_bcol + BIBTEX + "Price", entry);
        bibTex_Size = getFromXml(_bcol + BIBTEX + "Size", entry);

        NodeList nodeLst = entry.getElementsByTagName(_bcol + "Author");
        if (nodeLst.getLength() > 0) {
            getAuthors((Element) (nodeLst.item(0)), _bcol);
        }
    }

    private void populateFromBibtex(BibtexEntry bibtex) {
        // date = getDate(bibtex);	
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
        } else
        {
            if (bibtex.getType().getName().equalsIgnoreCase("techreport")) {
                thesisType = "Tech. rep.";
            } else if (bibtex.getType().getName().equalsIgnoreCase("mastersthesis")) {
                thesisType = "Master's thesis";
            } else if (bibtex.getType().getName().equalsIgnoreCase("phdthesis")) {
                thesisType = "Ph.D. dissertation";
            } else if (bibtex.getType().getName().equalsIgnoreCase("unpublished"))
             {
                thesisType = "unpublished";
            //else if (bibtex.getType().getName().equalsIgnoreCase("manual"))
            //	thesisType = "manual";
            }
        }

        if ((sourceType.equals("InternetSite") || sourceType.equals("DocumentFromInternetSite"))
                && (bibtex.getField("title") != null)) {
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
                && (bibtex.getField("title") != null)) {
            publicationTitle = bibtex.getField("title");
        }
        if (bibtex.getField(MSBIB + "medium") != null) {
            medium = bibtex.getField(MSBIB + "medium");
        }
        if (sourceType.equals("SoundRecording") && (bibtex.getField("title") != null)) {
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
        if (sourceType.equals("Interview") && (bibtex.getField("title") != null)) {
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
        if (FORMATXML)
        {
            title = format(title);
            // shortTitle = format(shortTitle);
            // publisher = format(publisher);
            // conferenceName = format(conferenceName);
            // department = format(department);
            // institution = format(institution);
            // internetSiteTitle = format(internetSiteTitle);
            // publicationTitle = format(publicationTitle);
            // albumTitle = format(albumTitle);
            // theater = format(theater);
            // distributor = format(distributor);
            // broadcastTitle = format(broadcastTitle);
            // broadcaster = format(broadcaster);
            // station = format(station);
            // court = format(court);
            // reporter = format(reporter);
            // bibTex_Series = format(bibTex_Series);
            bibTex_Abstract = format(bibTex_Abstract);
        }
    }

    private String format(String value)
    {
        if (value == null) {
            return null;
        }
        String result = null;
        LayoutFormatter chars = new XMLChars();
        result = chars.format(value);
        return result;
    }

    // http://www.microsoft.com/globaldev/reference/lcid-all.mspx
    private int getLCID(String language)
    {
        // TODO: add lanaguage to LCID mapping

        return 0;
    }

    // http://www.microsoft.com/globaldev/reference/lcid-all.mspx
    private String getLanguage(int LCID)
    {
        // TODO: add lanaguage to LCID mapping

        return "english";
    }

    private List<PersonName> getSpecificAuthors(String type, Element authors, String _bcol) {
        List<PersonName> result = null;
        NodeList nodeLst = authors.getElementsByTagName(_bcol + type);
        if (nodeLst.getLength() <= 0) {
            return result;
        }
        nodeLst = ((Element) (nodeLst.item(0))).getElementsByTagName(_bcol + "NameList");
        if (nodeLst.getLength() <= 0) {
            return result;
        }
        NodeList person = ((Element) (nodeLst.item(0))).getElementsByTagName(_bcol + "Person");
        if (person.getLength() <= 0) {
            return result;
        }

        result = new LinkedList<PersonName>();
        for (int i = 0; i < person.getLength(); i++)
        {
            NodeList firstName = ((Element) (person.item(i))).getElementsByTagName(_bcol + "First");
            NodeList lastName = ((Element) (person.item(i))).getElementsByTagName(_bcol + "Last");
            NodeList middleName = ((Element) (person.item(i))).getElementsByTagName(_bcol + "Middle");
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

    private void getAuthors(Element authorsElem, String _bcol) {
        authors = getSpecificAuthors("Author", authorsElem, _bcol);
        bookAuthors = getSpecificAuthors("BookAuthor", authorsElem, _bcol);
        editors = getSpecificAuthors("Editor", authorsElem, _bcol);
        translators = getSpecificAuthors("Translator", authorsElem, _bcol);
        producerNames = getSpecificAuthors("ProducerName", authorsElem, _bcol);
        composers = getSpecificAuthors("Composer", authorsElem, _bcol);
        conductors = getSpecificAuthors("Conductor", authorsElem, _bcol);
        performers = getSpecificAuthors("Performer", authorsElem, _bcol);
        writers = getSpecificAuthors("Writer", authorsElem, _bcol);
        directors = getSpecificAuthors("Director", authorsElem, _bcol);
        compilers = getSpecificAuthors("Compiler", authorsElem, _bcol);
        interviewers = getSpecificAuthors("Interviewer", authorsElem, _bcol);
        interviewees = getSpecificAuthors("Interviewee", authorsElem, _bcol);
        inventors = getSpecificAuthors("Inventor", authorsElem, _bcol);
        counsels = getSpecificAuthors("Counsel", authorsElem, _bcol);
    }

    private List<PersonName> getAuthors(String authors) {
        List<PersonName> result = new LinkedList<PersonName>();

        if (!authors.contains(" and "))
        {
            result.add(new PersonName(authors));
        }
        else
        {
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
            result += (bibtex.getField("year"));
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
        } else if (bibtexType.equalsIgnoreCase("inbook"))
        {
            result = "BookSection";
            bibTexEntry = "inbook";
        } /* SM 2010.10: generalized */
        else if (bibtexType.equalsIgnoreCase("booklet"))
        {
            result = "BookSection";
            bibTexEntry = "booklet";
        }
        else if (bibtexType.equalsIgnoreCase("incollection"))
        {
            result = "BookSection";
            bibTexEntry = "incollection";
        }

        else if (bibtexType.equalsIgnoreCase("article")) {
            result = "JournalArticle";
        } else if (bibtexType.equalsIgnoreCase("inproceedings"))
        {
            result = "ConferenceProceedings";
            bibTexEntry = "inproceedings";
        } /* SM 2010.10: generalized */
        else if (bibtexType.equalsIgnoreCase("conference"))
        {
            result = "ConferenceProceedings";
            bibTexEntry = "conference";
        }
        else if (bibtexType.equalsIgnoreCase("proceedings"))
        {
            result = "ConferenceProceedings";
            bibTexEntry = "proceedings";
        }
        else if (bibtexType.equalsIgnoreCase("collection"))
        {
            result = "ConferenceProceedings";
            bibTexEntry = "collection";
        }

        else if (bibtexType.equalsIgnoreCase("techreport"))
        {
            result = "Report";
            bibTexEntry = "techreport";
        } /* SM 2010.10: generalized */
        else if (bibtexType.equalsIgnoreCase("manual"))
        {
            result = "Report";
            bibTexEntry = "manual";
        }
        else if (bibtexType.equalsIgnoreCase("mastersthesis"))
        {
            result = "Report";
            bibTexEntry = "mastersthesis";
        }
        else if (bibtexType.equalsIgnoreCase("phdthesis"))
        {
            result = "Report";
            bibTexEntry = "phdthesis";
        }
        else if (bibtexType.equalsIgnoreCase("unpublished"))
        {
            result = "Report";
            bibTexEntry = "unpublished";
        }

        else if (bibtexType.equalsIgnoreCase("patent")) {
            result = "Patent";
        } else if (bibtexType.equalsIgnoreCase("misc")) {
            result = "Misc";
        } else if (bibtexType.equalsIgnoreCase("electronic"))
        {
            result = "Misc";
            bibTexEntry = "electronic";
        }

        return result;
    }

    private Node getDOMrepresentation() {
        Node result = null;
        try {
            DocumentBuilder d = DocumentBuilderFactory.newInstance().newDocumentBuilder();

            result = getDOMrepresentation(d.newDocument());
        } catch (Exception e)
        {
            throw new Error(e);
        }
        return result;
    }

    private void addField(Document d, Element parent, String name, String value) {
        if (value == null) {
            return;
        }
        Element elem = d.createElement(bcol + name);
        // elem.appendChild(d.createTextNode(healXML(value)));
        //		Text txt = d.createTextNode(value);
        //		if(!txt.getTextContent().equals(value))
        //			System.out.println("Values dont match!");
        //			// throw new Exception("Values dont match!");
        //		elem.appendChild(txt);
        elem.appendChild(d.createTextNode(stripNonValidXMLCharacters(value)));
        parent.appendChild(elem);
    }

    private void addAuthor(Document d, Element allAuthors, String entryName, List<PersonName> authorsLst) {
        if (authorsLst == null) {
            return;
        }
        Element authorTop = d.createElement(bcol + entryName);
        Element nameList = d.createElement(bcol + "NameList");
        for (PersonName name : authorsLst) {
            Element person = d.createElement(bcol + "Person");
            addField(d, person, "Last", name.getSurname());
            addField(d, person, "Middle", name.getMiddlename());
            addField(d, person, "First", name.getFirstname());
            nameList.appendChild(person);
        }
        authorTop.appendChild(nameList);

        allAuthors.appendChild(authorTop);
    }

    private void addAdrress(Document d, Element parent, String address) {
        if (address == null) {
            return;
        }

        // US address parser
        // See documentation here http://regexlib.com/REDetails.aspx?regexp_id=472
        // Pattern p = Pattern.compile("^(?n:(((?<address1>(\\d{1,5}(\\ 1\\/[234])?(\\x20[A-Z]([a-z])+)+ )|(P\\.O\\.\\ Box\\ \\d{1,5}))\\s{1,2}(?i:(?<address2>(((APT|B LDG|DEPT|FL|HNGR|LOT|PIER|RM|S(LIP|PC|T(E|OP))|TRLR|UNIT)\\x20\\w{1,5})|(BSMT|FRNT|LBBY|LOWR|OFC|PH|REAR|SIDE|UPPR)\\.?)\\s{1,2})?))?)(?<city>[A-Z]([a-z])+(\\.?)(\\x20[A-Z]([a-z])+){0,2})([,\\x20]+?)(?<state>A[LKSZRAP]|C[AOT]|D[EC]|F[LM]|G[AU]|HI|I[ADL N]|K[SY]|LA|M[ADEHINOPST]|N[CDEHJMVY]|O[HKR]|P[ARW]|RI|S[CD] |T[NX]|UT|V[AIT]|W[AIVY])([,\\x20]+?)(?<zipcode>(?!0{5})\\d{5}(-\\d {4})?)((([,\\x20]+?)(?<country>[A-Z]([a-z])+(\\.?)(\\x20[A-Z]([a-z])+){0,2}))?))$");
        // the pattern above is for C#, may not work with java. Never tested though.

        // reduced subset, supports only "CITY , STATE, COUNTRY"
        // \b(\w+)\s?[,]?\s?(\w+)\s?[,]?\s?(\w+)\b
        // WORD SPACE , SPACE WORD SPACE , SPACE WORD
        // tested using http://www.javaregex.com/test.html
        Pattern p = Pattern.compile("\\b(\\w+)\\s*[,]?\\s*(\\w+)\\s*[,]?\\s*(\\w+)\\b");
        Matcher m = p.matcher(address);
        if (m.matches() && (m.groupCount() > 3))
        {
            addField(d, parent, "City", m.group(1));
            addField(d, parent, "StateProvince", m.group(2));
            addField(d, parent, "CountryRegion", m.group(3));
        } else {
            /* SM: 2010.10 generalized */
            addField(d, parent, "City", address);
        }
    }

    private void addDate(Document d, Element parent, String date, String extra) {
        if (date == null) {
            return;
        }

        // Allows 20.3-2007|||20/3-  2007 etc. 
        // (\d{1,2})\s?[.,-/]\s?(\d{1,2})\s?[.,-/]\s?(\d{2,4})
        // 1-2 DIGITS SPACE SEPERATOR SPACE 1-2 DIGITS SPACE SEPERATOR SPACE 2-4 DIGITS
        // tested using http://www.javaregex.com/test.html
        Pattern p = Pattern.compile("(\\d{1,2})\\s*[.,-/]\\s*(\\d{1,2})\\s*[.,-/]\\s*(\\d{2,4})");
        Matcher m = p.matcher(date);
        if (m.matches() && (m.groupCount() > 3))
        {
            addField(d, parent, "Month" + extra, m.group(1));
            addField(d, parent, "Day" + extra, m.group(2));
            addField(d, parent, "Year" + extra, m.group(3));
        }
    }

    public Element getDOMrepresentation(Document d) {

        try {
            Element msbibEntry = d.createElement(bcol + "Source");

            addField(d, msbibEntry, "SourceType", sourceType);
            addField(d, msbibEntry, BIBTEX + "Entry", bibTexEntry);

            addField(d, msbibEntry, "Tag", tag);
            addField(d, msbibEntry, "GUID", GUID);
            if (LCID >= 0) {
                addField(d, msbibEntry, "LCID", Integer.toString(LCID));
            }
            addField(d, msbibEntry, "Title", title);
            addField(d, msbibEntry, "Year", year);
            addField(d, msbibEntry, "ShortTitle", shortTitle);
            addField(d, msbibEntry, "Comments", comments);

            Element allAuthors = d.createElement(bcol + "Author");

            addAuthor(d, allAuthors, "Author", authors);
            addAuthor(d, allAuthors, "BookAuthor", bookAuthors);
            addAuthor(d, allAuthors, "Editor", editors);
            addAuthor(d, allAuthors, "Translator", translators);
            addAuthor(d, allAuthors, "ProducerName", producerNames);
            addAuthor(d, allAuthors, "Composer", composers);
            addAuthor(d, allAuthors, "Conductor", conductors);
            addAuthor(d, allAuthors, "Performer", performers);
            addAuthor(d, allAuthors, "Writer", writers);
            addAuthor(d, allAuthors, "Director", directors);
            addAuthor(d, allAuthors, "Compiler", compilers);
            addAuthor(d, allAuthors, "Interviewer", interviewers);
            addAuthor(d, allAuthors, "Interviewee", interviewees);
            addAuthor(d, allAuthors, "Inventor", inventors);
            addAuthor(d, allAuthors, "Counsel", counsels);

            msbibEntry.appendChild(allAuthors);

            if (pages != null) {
                addField(d, msbibEntry, "Pages", pages.toString("-"));
            }
            addField(d, msbibEntry, "Volume", volume);
            addField(d, msbibEntry, "NumberVolumes", numberOfVolumes);
            addField(d, msbibEntry, "Edition", edition);
            addField(d, msbibEntry, "StandardNumber", standardNumber);
            addField(d, msbibEntry, "Publisher", publisher);

            addAdrress(d, msbibEntry, address);

            addField(d, msbibEntry, "BookTitle", bookTitle);
            addField(d, msbibEntry, "ChapterNumber", chapterNumber);

            addField(d, msbibEntry, "JournalName", journalName);
            addField(d, msbibEntry, "Issue", issue);
            addField(d, msbibEntry, "PeriodicalTitle", periodicalTitle);
            addField(d, msbibEntry, "ConferenceName", conferenceName);

            addField(d, msbibEntry, "Department", department);
            addField(d, msbibEntry, "Institution", institution);
            addField(d, msbibEntry, "ThesisType", thesisType);
            addField(d, msbibEntry, "InternetSiteTitle", internetSiteTitle);

            addDate(d, msbibEntry, dateAccessed, "Accessed");

            /* SM 2010.10 added month export */
            addField(d, msbibEntry, "Month", month);

            addField(d, msbibEntry, "URL", url);
            addField(d, msbibEntry, "ProductionCompany", productionCompany);
            addField(d, msbibEntry, "PublicationTitle", publicationTitle);
            addField(d, msbibEntry, "Medium", medium);
            addField(d, msbibEntry, "AlbumTitle", albumTitle);
            addField(d, msbibEntry, "RecordingNumber", recordingNumber);
            addField(d, msbibEntry, "Theater", theater);
            addField(d, msbibEntry, "Distributor", distributor);
            addField(d, msbibEntry, "BroadcastTitle", broadcastTitle);
            addField(d, msbibEntry, "Broadcaster", broadcaster);
            addField(d, msbibEntry, "Station", station);
            addField(d, msbibEntry, "Type", type);
            addField(d, msbibEntry, "PatentNumber", patentNumber);
            addField(d, msbibEntry, "Court", court);
            addField(d, msbibEntry, "Reporter", reporter);
            addField(d, msbibEntry, "CaseNumber", caseNumber);
            addField(d, msbibEntry, "AbbreviatedCaseNumber", abbreviatedCaseNumber);

            addField(d, msbibEntry, BIBTEX + "Series", bibTex_Series);
            addField(d, msbibEntry, BIBTEX + "Abstract", bibTex_Abstract);
            addField(d, msbibEntry, BIBTEX + "KeyWords", bibTex_KeyWords);
            addField(d, msbibEntry, BIBTEX + "CrossRef", bibTex_CrossRef);
            addField(d, msbibEntry, BIBTEX + "HowPublished", bibTex_HowPublished);
            addField(d, msbibEntry, BIBTEX + "Affiliation", bibTex_Affiliation);
            addField(d, msbibEntry, BIBTEX + "Contents", bibTex_Contents);
            addField(d, msbibEntry, BIBTEX + "Copyright", bibTex_Copyright);
            addField(d, msbibEntry, BIBTEX + "Price", bibTex_Price);
            addField(d, msbibEntry, BIBTEX + "Size", bibTex_Size);

            /* SM: 2010.10 end intype, paper support */
            addField(d, msbibEntry, BIBTEX + "InType", bibTex_InType);
            addField(d, msbibEntry, BIBTEX + "Paper", bibTex_Paper);

            return msbibEntry;
        } catch (Exception e)
        {
            System.out.println("Exception caught..." + e);
            e.printStackTrace();
            throw new Error(e);
        }
        // return null;
    }

    private void parseSingleStandardNumber(String type, String bibtype, String standardNum, HashMap<String, String> hm) {
        // tested using http://www.javaregex.com/test.html
        Pattern p = Pattern.compile(':' + type + ":(.[^:]+)");
        Matcher m = p.matcher(standardNum);
        if (m.matches()) {
            hm.put(bibtype, m.group(1));
        }
    }

    private void parseStandardNumber(String standardNum, HashMap<String, String> hm) {
        if (standardNumber == null) {
            return;
        }
        parseSingleStandardNumber("ISBN", "isbn", standardNum, hm); /* SM: 2010.10: lower case */
        parseSingleStandardNumber("ISSN", "issn", standardNum, hm); /* SM: 2010.10: lower case */
        parseSingleStandardNumber("LCCN", "lccn", standardNum, hm); /* SM: 2010.10: lower case */
        parseSingleStandardNumber("MRN", "mrnumber", standardNum, hm);
        /* SM: 2010.10 begin DOI support */
        parseSingleStandardNumber("DOI", "doi", standardNum, hm);
        /* SM: 2010.10 end DOI support */
    }

    private void addAuthor(HashMap<String, String> hm, String type, List<PersonName> authorsLst) {
        if (authorsLst == null) {
            return;
        }
        String allAuthors = "";
        boolean First = true;
        for (PersonName name : authorsLst) {
            if (!First) {
                allAuthors += " and ";
            }
            allAuthors += name.getFullname();
            First = false;
        }
        hm.put(type, allAuthors);
    }

    //	public String mapMSBibToBibtexTypeString(String msbib) {		
    //		String bibtex = "other";
    //		if(msbib.equals("Book"))
    //			bibtex = "book";
    //		else if(msbib.equals("BookSection"))
    //			bibtex = "inbook";
    //		else if(msbib.equals("JournalArticle"))
    //			bibtex = "article";
    //		else if(msbib.equals("ArticleInAPeriodical"))
    //			bibtex = "article";
    //		else if(msbib.equals("ConferenceProceedings"))
    //			bibtex = "conference";
    //		else if(msbib.equals("Report"))
    //			bibtex = "techreport";
    //		else if(msbib.equals("InternetSite"))
    //			bibtex = "other";
    //		else if(msbib.equals("DocumentFromInternetSite"))
    //			bibtex = "other";
    //		else if(msbib.equals("DocumentFromInternetSite"))
    //			bibtex = "other";
    //		else if(msbib.equals("ElectronicSource"))
    //			bibtex = "other";
    //		else if(msbib.equals("Art"))
    //			bibtex = "other";
    //		else if(msbib.equals("SoundRecording"))
    //			bibtex = "other";
    //		else if(msbib.equals("Performance"))
    //			bibtex = "other";
    //		else if(msbib.equals("Film"))
    //			bibtex = "other";
    //		else if(msbib.equals("Interview"))
    //			bibtex = "other";
    //		else if(msbib.equals("Patent"))
    //			bibtex = "other";
    //		else if(msbib.equals("Case"))
    //			bibtex = "other";
    //		else if(msbib.equals("Misc"))
    //			bibtex = "misc";
    //		else
    //			bibtex = "misc";
    //
    //		return bibtex;
    //	}

    private BibtexEntryType mapMSBibToBibtexType(String msbib)
    {
        BibtexEntryType bibtex = BibtexEntryType.OTHER;
        if (msbib.equals("Book")) {
            bibtex = BibtexEntryType.BOOK;
        } else if (msbib.equals("BookSection")) {
            bibtex = BibtexEntryType.INBOOK;
        } else if (msbib.equals("JournalArticle") || msbib.equals("ArticleInAPeriodical")) {
            bibtex = BibtexEntryType.ARTICLE;
        } else if (msbib.equals("ConferenceProceedings")) {
            bibtex = BibtexEntryType.CONFERENCE;
        } else if (msbib.equals("Report")) {
            bibtex = BibtexEntryType.TECHREPORT;
        } else if (msbib.equals("InternetSite") || msbib.equals("DocumentFromInternetSite") || msbib.equals("ElectronicSource") || msbib.equals("Art") || msbib.equals("SoundRecording") || msbib.equals("Performance") || msbib.equals("Film") || msbib.equals("Interview") || msbib.equals("Patent") || msbib.equals("Case")) {
            bibtex = BibtexEntryType.OTHER;
        } else {
            bibtex = BibtexEntryType.MISC;
        }

        return bibtex;
    }

    public BibtexEntry getBibtexRepresentation() {
        //		BibtexEntry entry = new BibtexEntry(BibtexFields.DEFAULT_BIBTEXENTRY_ID, 
        //				Globals.getEntryType(mapMSBibToBibtexTypeString(sourceType)));

        //		BibtexEntry entry = new BibtexEntry(BibtexFields.DEFAULT_BIBTEXENTRY_ID, 
        //				mapMSBibToBibtexType(sourceType));

        BibtexEntry entry = null;
        if (tag == null) {
            entry = new BibtexEntry(BibtexFields.DEFAULT_BIBTEXENTRY_ID,
                    mapMSBibToBibtexType(sourceType));
        }
        else {
            entry = new BibtexEntry(tag,
                    mapMSBibToBibtexType(sourceType)); // id assumes an existing database so don't
        }

        // Todo: add check for BibTexEntry types
        //		BibtexEntry entry = new BibtexEntry();
        //		if(sourceType.equals("Book"))
        //			entry.setType(BibtexEntryType.BOOK);
        //		else if(sourceType.equals("BookSection"))
        //			entry.setType(BibtexEntryType.INBOOK);
        //		else if(sourceType.equals("JournalArticle"))
        //			entry.setType(BibtexEntryType.ARTICLE);
        //		else if(sourceType.equals("ArticleInAPeriodical"))
        //			entry.setType(BibtexEntryType.ARTICLE);
        //		else if(sourceType.equals("ConferenceProceedings"))
        //			entry.setType(BibtexEntryType.CONFERENCE);
        //		else if(sourceType.equals("Report"))
        //			entry.setType(BibtexEntryType.TECHREPORT);
        //		else if(sourceType.equals("InternetSite"))
        //			entry.setType(BibtexEntryType.OTHER);
        //		else if(sourceType.equals("DocumentFromInternetSite"))
        //			entry.setType(BibtexEntryType.OTHER);
        //		else if(sourceType.equals("DocumentFromInternetSite"))
        //			entry.setType(BibtexEntryType.OTHER);
        //		else if(sourceType.equals("ElectronicSource"))
        //			entry.setType(BibtexEntryType.OTHER);
        //		else if(sourceType.equals("Art"))
        //			entry.setType(BibtexEntryType.OTHER);
        //		else if(sourceType.equals("SoundRecording"))
        //			entry.setType(BibtexEntryType.OTHER);
        //		else if(sourceType.equals("Performance"))
        //			entry.setType(BibtexEntryType.OTHER);
        //		else if(sourceType.equals("Film"))
        //			entry.setType(BibtexEntryType.OTHER);
        //		else if(sourceType.equals("Interview"))
        //			entry.setType(BibtexEntryType.OTHER);
        //		else if(sourceType.equals("Patent"))
        //			entry.setType(BibtexEntryType.OTHER);
        //		else if(sourceType.equals("Case"))
        //			entry.setType(BibtexEntryType.OTHER);
        //		else if(sourceType.equals("Misc"))
        //			entry.setType(BibtexEntryType.MISC);
        //		else
        //			entry.setType(BibtexEntryType.MISC);

        HashMap<String, String> hm = new HashMap<String, String>();

        if (tag != null) {
            hm.put("bibtexkey", tag);
        }
        //		if(GUID != null)
        //			hm.put("GUID",GUID);
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
        //		if(thesisType !=null )
        //			hm.put("type",thesisType);
        //		if(internetSiteTitle !=null )
        //			hm.put("title",internetSiteTitle);
        if (dateAccessed != null) {
            hm.put(MSBIB + "accessed", dateAccessed);
        }
        if (url != null) {
            hm.put("url", url);
        }
        if (productionCompany != null) {
            hm.put(MSBIB + "productioncompany", productionCompany);
        }
        //		if(publicationTitle !=null )
        //			hm.put("title",publicationTitle);
        if (medium != null) {
            hm.put(MSBIB + "medium", medium);
        }
        //		if(albumTitle !=null )
        //			hm.put("title",albumTitle);
        if (recordingNumber != null) {
            hm.put(MSBIB + "recordingnumber", recordingNumber);
        }
        if (theater != null) {
            hm.put(MSBIB + "theater", theater);
        }
        if (distributor != null) {
            hm.put(MSBIB + "distributor", distributor);
        }
        //		if(broadcastTitle !=null )
        //			hm.put("title",broadcastTitle);
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
     * 
     * URL: http://cse-mjmcl.cse.bris.ac.uk/blog/2007/02/14/1171465494443.html
     *
     * @param in The String whose non-valid characters we want to remove.
     * @return The in String, stripped of non-valid characters.
     */
    private String stripNonValidXMLCharacters(String in) {
        StringBuilder out = new StringBuilder(); // Used to hold the output.
        char current; // Used to reference the current character.

        if ((in == null) || (in != null && in.isEmpty()))
         {
            return ""; // vacancy test.
        }
        for (int i = 0; i < in.length(); i++) {
            current = in.charAt(i); // NOTE: No IndexOutOfBoundsException caught here; it should not happen.
            if ((current == 0x9) ||
                    (current == 0xA) ||
                    (current == 0xD) ||
                    ((current >= 0x20) && (current <= 0xD7FF)) ||
                    ((current >= 0xE000) && (current <= 0xFFFD)) ||
                    ((current >= 0x10000) && (current <= 0x10FFFF))) {
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
        StringWriter sresult = new StringWriter();
        try {
            DOMSource source = new DOMSource(getDOMrepresentation());
            StreamResult result = new StreamResult(sresult);
            Transformer trans = TransformerFactory.newInstance().newTransformer();
            trans.setOutputProperty(OutputKeys.INDENT, "yes");
            trans.transform(source, result);
        } catch (Exception e) {
            throw new Error(e);
        }
        return sresult.toString();
    }

}

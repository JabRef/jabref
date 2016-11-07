package net.sf.jabref.logic.msbib;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.jabref.model.strings.StringUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * MSBib entry representation
 *
 * @see <a href="http://mahbub.wordpress.com/2007/03/24/details-of-microsoft-office-2007-bibliographic-format-compared-to-bibtex/">ms office 2007 bibliography format compared to bibtex</a>
 * @see <a href="http://mahbub.wordpress.com/2007/03/22/deciphering-microsoft-office-2007-bibliography-format/">deciphering ms office 2007 bibliography format</a>
 * @see <a href="http://www.ecma-international.org/publications/standards/Ecma-376.htm">ECMA Standard</a>
 */
class MSBibEntry {

    // MSBib fields and values
    public Map<String, String> fields = new HashMap<>();

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

    public PageNumbers pages;

    public String standardNumber;
    public String address;
    public String conferenceName;
    public String thesisType;
    public String internetSiteTitle;
    public String dateAccessed;
    public String publicationTitle;
    public String albumTitle;
    public String broadcastTitle;
    public String year;
    public String month;
    public String day;
    public String number;
    public String patentNumber;
    public String journalName;

    private String bibtexEntryType;

    /**
     * reduced subset, supports only "CITY , STATE, COUNTRY" <br>
     *  <b>\b(\w+)\s?[,]?\s?(\w+)\s?[,]?\s?(\w*)\b</b> <br>
     *  WORD SPACE , SPACE WORD SPACE (Can be zero or more) , SPACE WORD (Can be zero or more) <br>
     *  Matches both single locations (only city) like Berlin and full locations like Stroudsburg, PA, USA <br>
     *  tested using http://www.regexpal.com/
     */
    private final Pattern ADDRESS_PATTERN = Pattern.compile("\\b(\\w+)\\s?[,]?\\s?(\\w*)\\s?[,]?\\s?(\\w*)\\b");

    /**
     * Allows 20.3-2007|||20/3-  2007 etc.
     * <b>(\d{1,2})\s?[.,-/]\s?(\d{1,2})\s?[.,-/]\s?(\d{2,4})</b>
     * 1-2 DIGITS SPACE SEPERATOR SPACE 1-2 DIGITS SPACE SEPERATOR SPACE 2-4 DIGITS
     */
    private static final Pattern DATE_PATTERN = Pattern
            .compile("(\\d{1,2})\\s*[.,-/]\\s*(\\d{1,2})\\s*[.,-/]\\s*(\\d{2,4})");


    public MSBibEntry() {
        //empty
    }

    /**
     * Createa new {@link MsBibEntry} to import from an xml element
     * @param entry
     */
    public MSBibEntry(Element entry) {
        populateFromXml(entry);
    }

    public String getType() {
        return fields.get("SourceType");
    }

    public String getCiteKey() {
        return fields.get("Tag");
    }

    private String getXmlElementTextContent(String name, Element entry) {
        String value = null;
        NodeList nodeLst = entry.getElementsByTagNameNS("*", name);
        if (nodeLst.getLength() > 0) {
            value = nodeLst.item(0).getTextContent();
        }
        return value;
    }

    private void populateFromXml(Element entry) {
        for (int i = 0; i < entry.getChildNodes().getLength(); i++) {
            Node node = entry.getChildNodes().item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                String key = node.getLocalName();
                String value = node.getTextContent();

                if ("SourceType".equals(key)) {
                    this.bibtexEntryType = value;
                }
                fields.put(key, value);
            }
        }

        String temp = getXmlElementTextContent("Pages", entry);
        if (temp != null) {
            pages = new PageNumbers(temp);
        }

        standardNumber = getXmlElementTextContent("StandardNumber", entry);
        conferenceName = getXmlElementTextContent("ConferenceName", entry);

        String city = getXmlElementTextContent("City", entry);
        String state = getXmlElementTextContent("StateProvince", entry);
        String country = getXmlElementTextContent("CountryRegion", entry);

        StringBuilder addressBuffer = new StringBuilder();
        if (city != null) {
            addressBuffer.append(city);
        }
        if (((state != null) && !state.isEmpty()) && ((city != null) && !city.isEmpty())) {
            addressBuffer.append(",").append(' ');
            addressBuffer.append(state);
        }
        if ((country != null) && !country.isEmpty()) {
            addressBuffer.append(",").append(' ');
            addressBuffer.append(country);
        }
        address = addressBuffer.toString().trim();
        if (address.isEmpty() || ",".equals(address)) {
            address = null;
        }

        if ("Patent".equalsIgnoreCase(bibtexEntryType)) {
            number = getXmlElementTextContent("PatentNumber", entry);
        }
        journalName = getXmlElementTextContent("JournalName", entry);
        month = getXmlElementTextContent("Month", entry);
        internetSiteTitle = getXmlElementTextContent("InternetSiteTitle", entry);

        String monthAccessed = getXmlElementTextContent("MonthAccessed", entry);
        String dayAccessed = getXmlElementTextContent("DayAccessed", entry);
        String yearAccessed = getXmlElementTextContent("YearAccessed", entry);

        StringBuilder sbDateAccesed = new StringBuilder();
        if (monthAccessed != null) {
            sbDateAccesed.append(monthAccessed);
            sbDateAccesed.append(' ');
        }
        if (dayAccessed != null) {
            sbDateAccesed.append(dayAccessed);
            sbDateAccesed.append(", ");
        }
        if (yearAccessed != null) {
            sbDateAccesed.append(yearAccessed);
        }
        dateAccessed = sbDateAccesed.toString().trim();
        if (dateAccessed.isEmpty() || ",".equals(dateAccessed)) {
            dateAccessed = null;
        }

        NodeList nodeLst = entry.getElementsByTagNameNS("*", "Author");
        if (nodeLst.getLength() > 0) {
            getAuthors((Element) nodeLst.item(0));
        }
    }

    private void getAuthors(Element authorsElem) {
        authors = getSpecificAuthors("Author", authorsElem);
        bookAuthors = getSpecificAuthors("BookAuthor", authorsElem);
        editors = getSpecificAuthors("Editor", authorsElem);
        translators = getSpecificAuthors("Translator", authorsElem);
        producerNames = getSpecificAuthors("ProducerName", authorsElem);
        composers = getSpecificAuthors("Composer", authorsElem);
        conductors = getSpecificAuthors("Conductor", authorsElem);
        performers = getSpecificAuthors("Performer", authorsElem);
        writers = getSpecificAuthors("Writer", authorsElem);
        directors = getSpecificAuthors("Director", authorsElem);
        compilers = getSpecificAuthors("Compiler", authorsElem);
        interviewers = getSpecificAuthors("Interviewer", authorsElem);
        interviewees = getSpecificAuthors("Interviewee", authorsElem);
        inventors = getSpecificAuthors("Inventor", authorsElem);
        counsels = getSpecificAuthors("Counsel", authorsElem);
    }

    private List<PersonName> getSpecificAuthors(String type, Element authors) {
        List<PersonName> result = null;
        NodeList nodeLst = authors.getElementsByTagNameNS("*", type);
        if (nodeLst.getLength() <= 0) {
            return result;
        }
        nodeLst = ((Element) nodeLst.item(0)).getElementsByTagNameNS("*", "NameList");
        if (nodeLst.getLength() <= 0) {
            return result;
        }
        NodeList person = ((Element) nodeLst.item(0)).getElementsByTagNameNS("*", "Person");
        if (person.getLength() <= 0) {
            return result;
        }

        result = new LinkedList<>();
        for (int i = 0; i < person.getLength(); i++) {
            NodeList firstName = ((Element) person.item(i)).getElementsByTagNameNS("*", "First");
            NodeList lastName = ((Element) person.item(i)).getElementsByTagNameNS("*", "Last");
            NodeList middleName = ((Element) person.item(i)).getElementsByTagNameNS("*", "Middle");
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

    /**
     * Gets the dom representation for one entry, used for export
     * @param document XmlDocument
     * @return XmlElement represenation of one entry
     */
    public Element getEntryDom(Document document) {
        Element rootNode = document.createElementNS(MSBibDatabase.NAMESPACE, MSBibDatabase.PREFIX + "Source");

        for (Map.Entry<String, String> entry : fields.entrySet()) {
            addField(document, rootNode, entry.getKey(), entry.getValue());
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

        Element allAuthors = document.createElementNS(MSBibDatabase.NAMESPACE, MSBibDatabase.PREFIX + "Author");

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
        addField(document, rootNode, "Year", year);
        addField(document, rootNode, "Month", month);

        addField(document, rootNode, "JournalName", journalName);
        addField(document, rootNode, "PatentNumber", patentNumber);

        addField(document, rootNode, "Number", number);

        addField(document, rootNode, "StandardNumber", standardNumber);
        addField(document, rootNode, "ConferenceName", conferenceName);

        addAddress(document, rootNode, address);

        addField(document, rootNode, "ThesisType", thesisType);
        addField(document, rootNode, "InternetSiteTitle", internetSiteTitle);

        addField(document, rootNode, "PublicationTitle", publicationTitle);
        addField(document, rootNode, "AlbumTitle", albumTitle);
        addField(document, rootNode, "BroadcastTitle", broadcastTitle);

        return rootNode;
    }

    private void addField(Document document, Element parent, String name, String value) {
        if (value == null) {
            return;
        }
        Element elem = document.createElementNS(MSBibDatabase.NAMESPACE, MSBibDatabase.PREFIX + name);
        elem.appendChild(document.createTextNode(StringUtil.stripNonValidXMLCharacters(value)));
        parent.appendChild(elem);
    }

    private void addAuthor(Document document, Element allAuthors, String entryName, List<PersonName> authorsLst) {
        if (authorsLst == null) {
            return;
        }
        Element authorTop = document.createElementNS(MSBibDatabase.NAMESPACE, MSBibDatabase.PREFIX + entryName);
        Element nameList = document.createElementNS(MSBibDatabase.NAMESPACE, MSBibDatabase.PREFIX + "NameList");
        for (PersonName name : authorsLst) {
            Element person = document.createElementNS(MSBibDatabase.NAMESPACE, MSBibDatabase.PREFIX + "Person");
            addField(document, person, "Last", name.getSurname());
            addField(document, person, "Middle", name.getMiddlename());
            addField(document, person, "First", name.getFirstname());
            nameList.appendChild(person);
        }
        authorTop.appendChild(nameList);
        allAuthors.appendChild(authorTop);
    }

    private void addAddress(Document document, Element parent, String addressToSplit) {
        if (addressToSplit == null) {
            return;
        }

        Matcher matcher = ADDRESS_PATTERN.matcher(addressToSplit);
        if (matcher.matches() && (matcher.groupCount() >= 3)) {
            addField(document, parent, "City", matcher.group(1));
            addField(document, parent, "StateProvince", matcher.group(2));
            addField(document, parent, "CountryRegion", matcher.group(3));
        } else {
            addField(document, parent, "City", addressToSplit);
        }
    }
}

package org.jabref.logic.msbib;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.Date;
import org.jabref.model.entry.Month;
import org.jabref.model.strings.StringUtil;

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
    public List<MsBibAuthor> authors;
    public List<MsBibAuthor> bookAuthors;
    public List<MsBibAuthor> editors;
    public List<MsBibAuthor> translators;
    public List<MsBibAuthor> producerNames;
    public List<MsBibAuthor> composers;
    public List<MsBibAuthor> conductors;
    public List<MsBibAuthor> performers;
    public List<MsBibAuthor> writers;
    public List<MsBibAuthor> directors;
    public List<MsBibAuthor> compilers;
    public List<MsBibAuthor> interviewers;
    public List<MsBibAuthor> interviewees;
    public List<MsBibAuthor> inventors;

    public List<MsBibAuthor> counsels;

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

    public MSBibEntry() {
        // empty
    }

    /**
     * Createa new {@link MsBibEntry} to import from an xml element
     *
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

        Optional<Date> parsedDateAcessed = Date.parse(Optional.ofNullable(yearAccessed),
                Optional.ofNullable(monthAccessed),
                Optional.ofNullable(dayAccessed));

        parsedDateAcessed.map(Date::getNormalized).ifPresent(date -> dateAccessed = date);

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

    private List<MsBibAuthor> getSpecificAuthors(String type, Element authors) {
        List<MsBibAuthor> result = null;
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

            StringBuilder sb = new StringBuilder();

            if (firstName.getLength() > 0) {
                sb.append(firstName.item(0).getTextContent());
                sb.append(" ");
            }
            if (middleName.getLength() > 0) {
                sb.append(middleName.item(0).getTextContent());
                sb.append(" ");
            }
            if (lastName.getLength() > 0) {
                sb.append(lastName.item(0).getTextContent());
            }

            AuthorList authorList = AuthorList.parse(sb.toString());
            for (Author author : authorList.getAuthors()) {
                result.add(new MsBibAuthor(author));
            }
        }

        return result;
    }

    /**
     * Gets the dom representation for one entry, used for export
     *
     * @param document XmlDocument
     * @return XmlElement represenation of one entry
     */
    public Element getEntryDom(Document document) {
        Element rootNode = document.createElementNS(MSBibDatabase.NAMESPACE, MSBibDatabase.PREFIX + "Source");

        for (Map.Entry<String, String> entry : fields.entrySet()) {
            addField(document, rootNode, entry.getKey(), entry.getValue());
        }

        Optional.ofNullable(dateAccessed).ifPresent(field -> addDateAcessedFields(document, rootNode));

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
        addField(document, rootNode, "Day", day);

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

    // Add authors for export
    private void addAuthor(Document document, Element allAuthors, String entryName, List<MsBibAuthor> authorsLst) {
        if (authorsLst == null) {
            return;
        }
        Element authorTop = document.createElementNS(MSBibDatabase.NAMESPACE, MSBibDatabase.PREFIX + entryName);

        Optional<MsBibAuthor> personName = authorsLst.stream().filter(MsBibAuthor::isCorporate)
                                                     .findFirst();
        if (personName.isPresent()) {
            MsBibAuthor person = personName.get();

            Element corporate = document.createElementNS(MSBibDatabase.NAMESPACE,
                    MSBibDatabase.PREFIX + "Corporate");
            corporate.setTextContent(person.getFirstLast());
            authorTop.appendChild(corporate);
        } else {

            Element nameList = document.createElementNS(MSBibDatabase.NAMESPACE, MSBibDatabase.PREFIX + "NameList");
            for (MsBibAuthor name : authorsLst) {
                Element person = document.createElementNS(MSBibDatabase.NAMESPACE, MSBibDatabase.PREFIX + "Person");
                addField(document, person, "Last", name.getLastName());
                addField(document, person, "Middle", name.getMiddleName());
                addField(document, person, "First", name.getFirstName());
                nameList.appendChild(person);
            }
            authorTop.appendChild(nameList);
        }
        allAuthors.appendChild(authorTop);
    }

    private void addDateAcessedFields(Document document, Element rootNode) {
        Optional<Date> parsedDateAcesseField = Date.parse(dateAccessed);
        parsedDateAcesseField.flatMap(Date::getYear).map(Object::toString).ifPresent(yearAccessed -> {
            addField(document, rootNode, "Year" + "Accessed", yearAccessed);
        });

        parsedDateAcesseField.flatMap(Date::getMonth)
                             .map(Month::getFullName).ifPresent(monthAcessed -> {
            addField(document, rootNode, "Month" + "Accessed", monthAcessed);
        });
        parsedDateAcesseField.flatMap(Date::getDay).map(Object::toString).ifPresent(dayAccessed -> {
            addField(document, rootNode, "Day" + "Accessed", dayAccessed);
        });
    }

    private void addAddress(Document document, Element parent, String addressToSplit) {
        if (addressToSplit == null) {
            return;
        }

        Matcher matcher = ADDRESS_PATTERN.matcher(addressToSplit);

        if (addressToSplit.contains(",") && matcher.matches() && (matcher.groupCount() >= 3)) {
            addField(document, parent, "City", matcher.group(1));
            addField(document, parent, "StateProvince", matcher.group(2));
            addField(document, parent, "CountryRegion", matcher.group(3));
        } else {
            addField(document, parent, "City", addressToSplit);
        }
    }
}

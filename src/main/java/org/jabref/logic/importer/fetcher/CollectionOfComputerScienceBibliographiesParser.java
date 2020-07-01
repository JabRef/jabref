package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class CollectionOfComputerScienceBibliographiesParser implements Parser {
    @Override
    public List<BibEntry> parseEntries(InputStream inputStream) throws ParseException {
        try {
            Document document = buildDocumentFromInputStream(inputStream);
            // uncomment to generate test case xml
            // XMLUtil.printDocument(document);

            NodeList childNodes = document.getChildNodes();
            List<Element> itemElements = findItemElementsRecursively(childNodes);
            List<BibEntry> bibEntries = parseItemElements(itemElements);

            // uncomment to generate test case bib files
            // System.out.println(bibEntries);

            return bibEntries;
        } catch (ParserConfigurationException | SAXException | IOException exception) {
            throw new ParseException(exception);
        }
    }

    private Document buildDocumentFromInputStream(InputStream inputStream) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilder dbuild = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Reader reader = new InputStreamReader(inputStream, "UTF-8");
        InputSource is = new InputSource(reader);
        return dbuild.parse(is);
    }

    private List<Element> findItemElementsRecursively(NodeList nodeList) {
        List<Element> itemNodes = new LinkedList();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node child = nodeList.item(i);
            if (child.getNodeName().equals("item")
                    && child.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) child;
                itemNodes.add(element);
            } else {
                NodeList childNodes = child.getChildNodes();
                List<Element> childItemNodes = findItemElementsRecursively(childNodes);
                itemNodes.addAll(childItemNodes);
            }
        }

        return itemNodes;
    }

    private List<BibEntry> parseItemElements(List<Element> itemElements) {
        List<BibEntry> items = new LinkedList<>();
        for (Element itemElement : itemElements) {
            BibEntry bibEntry = parseItemElement(itemElement);
            items.add(bibEntry);
        }

        return items;
    }

    private BibEntry parseItemElement(Element item) {
        BibEntry bibEntry = new BibEntry();
        setFieldFromTag(bibEntry, item, StandardField.TITLE, "dc:title");
        setFieldFromTag(bibEntry, item, StandardField.AUTHOR, "dc:creator");
        setFieldFromTag(bibEntry, item, StandardField.DATE, "dc:date");
        setFieldFromTag(bibEntry, item, StandardField.URL, "link");
        return bibEntry;
    }

    private void setFieldFromTag(BibEntry bibEntry, Element item, StandardField field, String tagName) {
        Node element = item.getElementsByTagName(tagName).item(0);
        if (element == null) {
            return;
        }

        String value = element.getTextContent();
        bibEntry.setField(field, value);
    }
}

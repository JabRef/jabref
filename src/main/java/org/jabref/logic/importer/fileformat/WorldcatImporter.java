package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.util.FileType;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class WorldcatImporter extends Importer {

    private static final String DESCRIPTION = "Importer for Worldcat Open Search XML format";
    private static final String NAME = "WorldcatImporter";

    /**
     * Parse the reader to an xml document
     *
     * @param s the reader to be parsed
     * @return XML document representing the content of s
     * @throws IllegalArgumentException if s is badly formated or other exception occurs during parsing
     */
    private Document parse(BufferedReader s) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            return builder.parse(new InputSource(s));
        } catch (ParserConfigurationException e) {
            throw new IllegalArgumentException("Parser Config Exception: ", e);
        } catch (SAXException e) {
            throw new IllegalArgumentException("SAX Exception: ", e);
        } catch (IOException e) {
            throw new IllegalArgumentException("IO Exception: ", e);
        }
    }

    @Override
    public boolean isRecognizedFormat(BufferedReader input) throws IOException {
        try {
            Document doc = parse(input);
            return doc.getElementsByTagName("feed") != null;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Gets the text content of the given element. If the element was not found, an empty Optional is returned;
     *
     * @param xml the element do search trough
     * @param tag the tag to find
     */
    private Optional<String> getElementContent(Element xml, String tag) {
        NodeList nl = xml.getElementsByTagName(tag);
        if (nl == null) {
            return Optional.empty();
        }
        if (nl.getLength() == 0) {
            return Optional.empty();
        }
        return Optional.ofNullable(nl.item(0).getTextContent());
    }

    /**
     * Parse the xml entry to a bib entry
     *
     * @param xmlEntry the XML element from open search
     * @return the correspoinding bibentry
     */
    private BibEntry xmlEntryToBibEntry(Element xmlEntry) {
        BibEntry result = new BibEntry();

        getElementContent(xmlEntry, "dc:creator").ifPresent(content -> result.setField(StandardField.AUTHOR, content));
        getElementContent(xmlEntry, "dc:title").ifPresent(content -> result.setField(StandardField.TITLE, content));
        getElementContent(xmlEntry, "dc:date").ifPresent(content -> result.setField(StandardField.YEAR, content));
        getElementContent(xmlEntry, "dc:publisher").ifPresent(content -> result.setField(StandardField.PUBLISHER, content));
        getElementContent(xmlEntry, "dc:recordIdentifier").ifPresent(content -> result.setField(StandardField.URL, "http://worldcat.org/oclc/" + content));

        return result;
    }

    /**
     * Parse an XML documents with open search entries to a parserResult of the bibentries
     *
     * @param doc the main XML document from open search
     * @return the ParserResult containing the BibEntries collection
     */
    private ParserResult docToParserRes(Document doc) {
        Element feed = (Element) doc.getElementsByTagName("entries").item(0);
        NodeList entryXMLList = feed.getElementsByTagName("entry");

        List<BibEntry> bibList = new ArrayList<>(entryXMLList.getLength());
        for (int i = 0; i < entryXMLList.getLength(); i++) {
            Element xmlEntry = (Element) entryXMLList.item(i);
            BibEntry bibEntry = xmlEntryToBibEntry(xmlEntry);
            bibList.add(bibEntry);
        }

        return new ParserResult(bibList);
    }

    @Override
    public ParserResult importDatabase(BufferedReader input) throws IOException {
        Document parsedDoc = parse(input);
        return docToParserRes(parsedDoc);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public FileType getFileType() {
        return StandardFileType.XML;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }
}

package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fetcher.WorldcatFetcher;
import org.jabref.logic.net.URLDownload;
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

    private String WORLDCAT_READ_URL;
    
    public WorldcatImporter () {
        //Used the same key as Worldcat fether
        this.WORLDCAT_READ_URL = "http://www.worldcat.org/webservices/catalog/content/{OCLC-NUMBER}?recordSchema=info%3Asrw%2Fschema%2F1%2Fdc&wskey=" + WorldcatFetcher.API_KEY;
    }

    /**
     * Parse the reader to an xml document
     * @param s the reader to be parsed
     * @return XML document representing the content of s
     * @throws IllegalArgumentException if s is badly formated or other exception occurs during parsing
     */
    private Document parse (BufferedReader s) {
        try { 
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance ();
            DocumentBuilder builder = factory.newDocumentBuilder ();

            return builder.parse (new InputSource (s));
        } catch (ParserConfigurationException e) {
            throw new IllegalArgumentException ("Parser Config Exception: " + e.getMessage (), e);
        } catch (SAXException e) {
            throw new IllegalArgumentException ("SAX Exception: " + e.getMessage (), e);
        } catch (IOException e) {
            throw new IllegalArgumentException ("IO Exception: " + e.getMessage (), e);
        }
    }

    /**
     * Get more information about a article through its OCLC id. Picks the first 
     * element with this tag
     * @param id the oclc id
     * @return the XML element that contains all tags
     */
    private Element getSpecificInfoOnOCLC (String id) throws IOException {
        URLDownload urlDownload = new URLDownload (WORLDCAT_READ_URL.replace ("{OCLC-NUMBER}", id));
        URLDownload.bypassSSLVerification ();
        String resp = urlDownload.asString ();	

        Document mainDoc = parse (new BufferedReader (new StringReader (resp)));
        NodeList parentElemOfTags = mainDoc.getElementsByTagName ("oclcdcs");

        return (Element) parentElemOfTags.item (0);
    }

    @Override
    public boolean isRecognizedFormat (BufferedReader input) throws IOException {
        try {
            Document doc = parse (input);
            return doc.getElementsByTagName ("feed") != null;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Get the element of a tag in an XML element. Picks the first element
     * with this tag
     * @param xml the element do search trough
     * @param tag the tag to find
     * @return the tag element
     * @throws NullPointerException if there is no element by this tag
     */
    private Element getElementByTag (Element xml, String tag) throws NullPointerException { 
        NodeList nl = xml.getElementsByTagName (tag);
        return (Element) nl.item (0);
    }

    /**
     * Parse the xml entry to a bib entry
     * @param xmlEntry the XML element from open search
     * @return the correspoinding bibentry
     * @throws IOException if we cannot search Worldcat with the OCLC of the entry
     */
    private BibEntry xmlEntryToBibEntry (Element xmlEntry) throws IOException { 
        Element authorsElem = getElementByTag (xmlEntry, "author");
        String authors = getElementByTag (authorsElem, "name").getTextContent ();

        String title = getElementByTag (xmlEntry, "title").getTextContent ();

        String url = getElementByTag (xmlEntry, "link").getAttribute ("href");

        String oclc = xmlEntry.getElementsByTagName ("oclcterms:recordIdentifier").item (0).getTextContent ();
        Element detailedInfo = getSpecificInfoOnOCLC (oclc);

        String date = detailedInfo.getElementsByTagName ("dc:date").item (0).getTextContent ();

        String publisher = detailedInfo.getElementsByTagName ("dc:publisher").item (0).getTextContent ();

        BibEntry entry = new BibEntry ();

        entry.setField (StandardField.AUTHOR, authors);
        entry.setField (StandardField.TITLE, title);
        entry.setField (StandardField.URL, url);
        entry.setField (StandardField.YEAR, date);
        entry.setField (StandardField.JOURNAL, publisher);

        return entry;
    }

    /**
     * Parse an XML documents with open search entries to a parserResult of 
     * the bibentries
     * @param doc the main XML document from open search
     * @return the ParserResult containing the BibEntries collection
     * @throws IOException if {@link xmlEntryToBibEntry} throws 
     */
    private ParserResult docToParserRes (Document doc) throws IOException { 
        Element feed = (Element) doc.getElementsByTagName ("feed").item (0);
        NodeList entryXMLList = feed.getElementsByTagName ("entry");

        List<BibEntry> bibList = new ArrayList<>(entryXMLList.getLength ());
        for (int i = 0; i < entryXMLList.getLength (); i++) {
            Element xmlEntry = (Element) entryXMLList.item (i);
            BibEntry bibEntry = xmlEntryToBibEntry (xmlEntry);
            bibList.add (bibEntry);		
        }

        return new ParserResult (bibList);
    }

    @Override
    public ParserResult importDatabase (BufferedReader input) throws IOException {
        Document parsedDoc = parse (input);
        return docToParserRes (parsedDoc);
    }

    @Override
    public String getName () {
        return NAME;
    }

    @Override
    public FileType getFileType () {
        return StandardFileType.XML;
    }

    @Override
    public String getDescription () {
        return DESCRIPTION;
    }

}

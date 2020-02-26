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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class WorldcatImporter extends Importer {

	private String WORLDCAT_READ_URL;

	private static final Logger LOGGER = LoggerFactory.getLogger(WorldcatImporter.class);

	private final static String NAME = "WorldcatImporter";
	private final static String DESCRIPTION = "Takes valid XML from Worldcat Open Search and parses them to BibEntry";

	public WorldcatImporter(){
		this.WORLDCAT_READ_URL = "http://www.worldcat.org/webservices/catalog/content/{OCLC-NUMBER}?recordSchema=info%3Asrw%2Fschema%2F1%2Fdc&wskey=" + WorldcatFetcher.API_KEY;
	}

	private Document parse(BufferedReader s){
		try{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();

			return builder.parse(new InputSource(s));
		} catch(ParserConfigurationException e){
			throw new IllegalArgumentException("Parser Config Exception: " + e.getMessage(), e);
		} catch(SAXException e){
			throw new IllegalArgumentException("SAX Exception: " + e.getMessage(), e);
		} catch(IOException e){
			throw new IllegalArgumentException("IO Exception: " + e.getMessage(), e);
		}
	}

	private Element getSpecificInfoOnOCLC(String id) throws IOException {
		URLDownload urlDownload = new URLDownload(WORLDCAT_READ_URL.replace("{OCLC-NUMBER}", id));
		URLDownload.bypassSSLVerification();
		String resp = urlDownload.asString();	

		//Used when no API key is present. Comment out lines above as well
		// String resp = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><oclcdcs xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:oclcterms=\"http://purl.org/oclc/terms/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"> <dc:creator>Miller, Glenn.</dc:creator> <dc:date>2010</dc:date> <dc:description>Remastered versions of Glenn Miller's original recordings which bring a freshness to his beloved classics. The album also features a version of 'In the mood' with Jodie Prenger, winner of the BBC's 'I'll do anything'.</dc:description> <dc:description>In the mood -- Moonlight serenade -- Don't sit under the apple tree (with anyone else but me) -- Tuxedo junction -- A string of pearls -- Pennsylvania 6-5000 -- Chattanooga choo-choo -- American patrol -- (I've got a gal in) Kalamazoo -- On a little street in Singapore -- The St. Louis blues march -- A nightingale sang in Berkeley Square -- Star dust -- Little brown jug -- When you wish upon a star -- The woodpecker song -- G.I. jive -- Fools rush in -- Over there -- Blueberry hill -- Over the rainbow -- Serenade in blue -- When Johnny comes marching home -- In the mood (feat. Jodie Prenger).</dc:description> <dc:format>1 CD (72 min., 18 sec.) ; 4 3/4 in.</dc:format> <dc:language xsi:type=\"http://purl.org/dc/terms/ISO639-2\">eng</dc:language> <dc:publisher>Sony Music Entertainment</dc:publisher> <dc:subject xsi:type=\"http://purl.org/dc/terms/LCSH\">Dance orchestra music.</dc:subject> <dc:subject xsi:type=\"http://purl.org/dc/terms/LCSH\">Big band music.</dc:subject> <dc:subject xsi:type=\"http://purl.org/dc/terms/LCSH\">Popular music--1931-1940.</dc:subject> <dc:subject xsi:type=\"http://purl.org/dc/terms/LCSH\">Popular music--1941-1950.</dc:subject> <dc:subject xsi:type=\"http://purl.org/dc/terms/LCSH\">Compact discs.</dc:subject> <dc:title>The very best of Glenn Miller </dc:title> <dc:type>Sound</dc:type> <oclcterms:recordCreationDate xsi:type=\"http://purl.org/oclc/terms/marc008date\">101104</oclcterms:recordCreationDate> <oclcterms:recordIdentifier>754508587</oclcterms:recordIdentifier> </oclcdcs>";
		
		Document mainDoc = parse(new BufferedReader(new StringReader(resp)));
		NodeList parentElemOfTags = mainDoc.getElementsByTagName("oclcdcs");

		return (Element) parentElemOfTags.item(0);
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

	private Element getElementByTag(Element xml, String tag){
		NodeList nl = xml.getElementsByTagName(tag);
		return (Element) nl.item(0);
	}

	private BibEntry xmlEntryToBibEntry(Element xmlEntry) throws IOException{
		Element authorsElem = getElementByTag(xmlEntry, "author");
		String authors = getElementByTag(authorsElem, "name").getTextContent();

		String title = getElementByTag(xmlEntry, "title").getTextContent();

		String url = getElementByTag(xmlEntry, "link").getAttribute("href");

		String oclc = xmlEntry.getElementsByTagName("oclcterms:recordIdentifier").item(0).getTextContent();
		System.out.println(oclc + " , " + title);
		Element detailedInfo = getSpecificInfoOnOCLC(oclc);

		String date = detailedInfo.getElementsByTagName("dc:date").item(0).getTextContent();

		String publisher = detailedInfo.getElementsByTagName("dc:publisher").item(0).getTextContent();

		BibEntry entry = new BibEntry();

		entry.setField(StandardField.AUTHOR, authors);
		entry.setField(StandardField.TITLE, title);
		entry.setField(StandardField.URL, url);
		entry.setField(StandardField.YEAR, date);
		entry.setField(StandardField.JOURNAL, publisher);

		return entry;
	}

	private ParserResult docToParserRes(Document doc) throws IOException{
		Element feed = (Element) doc.getElementsByTagName("feed").item(0);
		NodeList entryXMLList = feed.getElementsByTagName("entry");

		List<BibEntry> bibList = new ArrayList<>(entryXMLList.getLength());
		for(int i = 0; i < entryXMLList.getLength(); i++){
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
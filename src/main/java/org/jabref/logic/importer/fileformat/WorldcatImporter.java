package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.util.FileType;
import org.jabref.logic.util.StandardFileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class WorldcatImporter extends Importer {

	private static final Logger LOGGER = LoggerFactory.getLogger(WorldcatImporter.class);

	private final static String NAME = "WorldcatImporter";
	private final static String DESCRIPTION = "Takes valid XML from Worldcat Open Search and parses them to BibEntry";
	

	private String stringifyReader(BufferedReader bf){
		StringBuilder sb = new StringBuilder();
		String ln;
		try {
			while((ln = bf.readLine()) != null){
				sb.append(ln);
			}
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
			throw new IllegalArgumentException("Bad argument");
		}
	}

	private Document parse(String s){
		try{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();

			return builder.parse(s);
		} catch(ParserConfigurationException e){
			throw new IllegalArgumentException("Parser Config Exception: " + e.getMessage(), e);
		} catch(SAXException e){
			throw new IllegalArgumentException("SAX Exception: " + e.getMessage(), e);
		} catch(IOException e){
			throw new IllegalArgumentException("IO Exception: " + e.getMessage(), e);
		}
	}

	@Override
	public boolean isRecognizedFormat(BufferedReader input) throws IOException {
		try {
			String strInput = stringifyReader(input);
			Document doc = parse(strInput);
			return doc.getElementsByTagName("feed") != null;
		} catch (IllegalArgumentException e) {

			return false;
		}
	}

	@Override
	public ParserResult importDatabase(BufferedReader input) throws IOException {
		// TODO Auto-generated method stub
		return null;
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
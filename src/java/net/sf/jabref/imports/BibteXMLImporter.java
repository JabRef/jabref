package net.sf.jabref.imports;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.sf.jabref.BibtexEntry;

/**
 * Importer for the Refer/Endnote format.
 *
 * check here for details on the format
 * http://www.ecst.csuchico.edu/~jacobsd/bib/formats/endnote.html
 */
public class BibteXMLImporter extends ImportFormat {

    /**
     * Return the name of this import format.
     */
    public String getFormatName() {
	return "BibTeXML";
    }

    /*
     *  (non-Javadoc)
     * @see net.sf.jabref.imports.ImportFormat#getCLIId()
     */
    public String getCLIId() {
      return "bibtexml";
    }
    

    /**
     * Check whether the source is in the correct format for this importer.
     */
    public boolean isRecognizedFormat(InputStream stream) throws IOException {

	// Our strategy is to look for the "<bibtex:file *" line.
	BufferedReader in = new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream));
	Pattern pat1 = Pattern
	    .compile("<bibtex:file .*");
	String str;
	while ((str = in.readLine()) != null){
	    if (pat1.matcher(str).find())
		return true;
	}
	return false;
    }

    /**
     * Parse the entries in the source, and return a List of BibtexEntry
     * objects.
     */
    public List<BibtexEntry> importEntries(InputStream stream) throws IOException {

	ArrayList<BibtexEntry> bibItems = new ArrayList<BibtexEntry>();

	// Obtain a factory object for creating SAX parsers
	SAXParserFactory parserFactory = SAXParserFactory.newInstance();
	// Configure the factory object to specify attributes of the parsers it
	// creates
	// parserFactory.setValidating(true);
	parserFactory.setNamespaceAware(true);	
	// Now create a SAXParser object


	try{
	    SAXParser parser = parserFactory.newSAXParser(); //May throw exceptions
	    BibTeXMLHandler handler = new BibTeXMLHandler();
	    // Start the parser. It reads the file and calls methods of the handler.
	    parser.parse(stream, handler);
	    // When you're done, report the results stored by your handler object
	    bibItems = handler.getItems();
	    
	}catch (javax.xml.parsers.ParserConfigurationException e1){
	    e1.printStackTrace();
	}catch (org.xml.sax.SAXException e2){
	    e2.printStackTrace();
	}catch (java.io.IOException e3){
	    e3.printStackTrace();
	}
	return bibItems;
	
    }
    
}

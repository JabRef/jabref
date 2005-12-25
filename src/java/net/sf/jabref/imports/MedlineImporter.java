package net.sf.jabref.imports;

import java.io.InputStream;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

/**
 * Importer for the Refer/Endnote format.
 *
 * check here for details on the format
 * http://www.ecst.csuchico.edu/~jacobsd/bib/formats/endnote.html
 */
public class MedlineImporter extends ImportFormat {

    /**
     * Return the name of this import format.
     */
    public String getFormatName() {
	return "Medline";
    }

    /*
     *  (non-Javadoc)
     * @see net.sf.jabref.imports.ImportFormat#getCLIId()
     */
    public String getCLIId() {
      return "medline";
    }
    
    /**
     * Check whether the source is in the correct format for this importer.
     */
    public boolean isRecognizedFormat(InputStream in) throws IOException {
	return true;
    }

    /**
     * Parse the entries in the source, and return a List of BibtexEntry
     * objects.
     */
    public List importEntries(InputStream stream) throws IOException {

	// Obtain a factory object for creating SAX parsers
	SAXParserFactory parserFactory = SAXParserFactory.newInstance();

	// Configure the factory object to specify attributes of the parsers it
	// creates
	parserFactory.setValidating(true);
	parserFactory.setNamespaceAware(true);
	
	// Now create a SAXParser object
	ArrayList bibItems = null;
	try{
	    SAXParser parser = parserFactory.newSAXParser(); //May throw exceptions
	    MedlineHandler handler = new MedlineHandler();
	    // Start the parser. It reads the file and calls methods of the handler.
	    parser.parse(stream, handler);
	    
	    // When you're done, report the results stored by your handler object
	    bibItems = handler.getItems();
	}catch (javax.xml.parsers.ParserConfigurationException e1){
	}catch (org.xml.sax.SAXException e2){
	}catch (java.io.IOException e3){
	}
	
	return bibItems;
	
    }
    
}

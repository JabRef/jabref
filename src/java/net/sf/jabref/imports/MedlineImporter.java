package net.sf.jabref.imports;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.sf.jabref.BibtexEntry;

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
     * (non-Javadoc)
     * 
     * @see net.sf.jabref.imports.ImportFormat#getCLIId()
     */
    public String getCLIId() {
        return "medline";
    }

    /**
     * Check whether the source is in the correct format for this importer.
     */
    public boolean isRecognizedFormat(InputStream stream) throws IOException {

        BufferedReader in = new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream));
        String str;
        int i=0;
        while (((str = in.readLine()) != null) && (i < 50)) {

			if (str.toLowerCase().indexOf("<pubmedarticle>") >= 0)
				return true;

            i++;
        }

		return false;
    }

    /**
     * Fetch and parse an medline item from eutils.ncbi.nlm.nih.gov.
     * 
     * @param id One or several ids, separated by ","
     * 
     * @return Will return an empty list on error.
     */
    public static List<BibtexEntry> fetchMedline(String id) {
        String baseUrl = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&retmode=xml&rettype=citation&id=" +
            id;
        try {
            URL url = new URL(baseUrl);
            URLConnection data = url.openConnection();
            return new MedlineImporter().importEntries(data.getInputStream());
        } catch (IOException e) {
            return new ArrayList<BibtexEntry>();
        }
    }

    /**
     * Parse the entries in the source, and return a List of BibtexEntry
     * objects.
     */
    public List<BibtexEntry> importEntries(InputStream stream) throws IOException {

        // Obtain a factory object for creating SAX parsers
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();

        // Configure the factory object to specify attributes of the parsers it
        // creates
        parserFactory.setValidating(true);
        parserFactory.setNamespaceAware(true);

        // Now create a SAXParser object
        ArrayList<BibtexEntry> bibItems = null;
        try {
            SAXParser parser = parserFactory.newSAXParser(); // May throw
            // exceptions
            MedlineHandler handler = new MedlineHandler();
            // Start the parser. It reads the file and calls methods of the
            // handler.
            parser.parse(stream, handler);

            // Switch this to true if you want to make a local copy for testing.
            if (false) {
                stream.reset();
                FileOutputStream out = new FileOutputStream(new File("/home/alver/ut.txt"));
                int c;
                while ((c = stream.read()) != -1) {
                    out.write((char) c);
                }
                out.close();
            }

            // When you're done, report the results stored by your handler
            // object
            bibItems = handler.getItems();
        } catch (javax.xml.parsers.ParserConfigurationException e1) {
            e1.printStackTrace();
        } catch (org.xml.sax.SAXException e2) {
            e2.printStackTrace();
        } catch (java.io.IOException e3) {
            e3.printStackTrace();
        }

        return bibItems;
    }

}

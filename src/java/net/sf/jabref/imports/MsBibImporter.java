package net.sf.jabref.imports;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.msbib.MSBibDatabase;

import org.w3c.dom.Document;


/**
 * Importer for the MS Office 2007 XML bibliography format
 * By S. M. Mahbub Murshed
 *
 * ...
 */
public class MsBibImporter extends ImportFormat {

    public boolean isRecognizedFormat(InputStream in) throws IOException {

        /*
            This method is available for checking if a file can be of the MSBib type.
            The effect of this method is primarily to avoid unnecessary processing of
            files when searching for a suitable import format. If this method returns
            false, the import routine will move on to the next import format.

            The correct behaviour is to return false if it is certain that the file is
            not of the MsBib type, and true otherwise. Returning true is the safe choice
            if not certain.
         */
    	Document docin = null;
    	try {
    	DocumentBuilder dbuild = DocumentBuilderFactory.
    								newInstance().
    								newDocumentBuilder();
   		docin = dbuild.parse(in);   		
    	} catch (Exception e) {
	   		return false;
    	}
    	if(docin!= null && docin.getDocumentElement().getTagName().contains("Sources") == false)
    		return false;
//   		NodeList rootLst = docin.getElementsByTagName("b:Sources");
//   		if(rootLst.getLength()==0)
//   			rootLst = docin.getElementsByTagName("Sources");
//   		if(rootLst.getLength()==0)
//   			return false;
    	// System.out.println(docin.getDocumentElement().getTagName());
        return true;
    }

    /**
	 * String used to identify this import filter on the command line.
	 * 
	 * @override
	 * @return "msbib"
	 */
	public String getCLIid() {
		return "msbib";
	}

    public List<BibtexEntry> importEntries(InputStream in) throws IOException {

        MSBibDatabase dbase = new MSBibDatabase();

        List<BibtexEntry> entries = dbase.importEntries(in);

        return entries;
    }

    public String getFormatName() {
        // This method should return the name of this import format.
        return "MSBib";
    }

}

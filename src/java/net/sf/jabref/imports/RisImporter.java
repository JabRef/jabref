package net.sf.jabref.imports;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.Reader;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Vector;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexEntryType;
import net.sf.jabref.Globals;
import net.sf.jabref.Util;

/**
 * Imports a Biblioscape Tag File. The format is described on
 * http://www.biblioscape.com/manual_bsp/Biblioscape_Tag_File.htm Several
 * Biblioscape field types are ignored. Others are only included in the BibTeX
 * field "comment".
 */
public class RisImporter implements ImportFormat {

    /**
     * Return the name of this import format.
     */
    public String getFormatName() {
	return "RIS";
    }


    /**
     * Check whether the source is in the correct format for this importer.
     */
    public boolean isRecognizedFormat(InputStream stream) throws IOException {

	// Our strategy is to look for the "AU  - *" line.
	BufferedReader in = new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream));
	Pattern pat1 = Pattern
	    .compile("AU  - .*");
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
    public List importEntries(InputStream stream) throws IOException {
	ArrayList bibitems = new ArrayList();
	StringBuffer sb = new StringBuffer();
	BufferedReader in = new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream));
	String str;
	while ((str = in.readLine()) != null){
	    sb.append(str);
	    sb.append("\n");
	}
	String[] entries = sb.toString().split("ER  -");

	HashMap hm = new HashMap();
	for (int i = 0; i < entries.length - 1; i++){
	    String Type = "", Author = "", StartPage = "", EndPage = "";
	    hm.clear();
	    
	    String[] fields = entries[i].split("\n");
	    for (int j = 0; j < fields.length; j++){
		if (fields[j].length() < 6) continue;
		else{
		    String lab = fields[j].substring(0, 2);
		    String val = fields[j].substring(6).trim();
		    if (lab.equals("TY")){
			if (val.equals("BOOK")) Type = "book";
			else if (val.equals("JOUR")) Type = "article";
			else Type = "other";
		    }else if (lab.equals("T1") || lab.equals("TI")) hm.put("title", val);//Title
		    // =
		    // val;
		    
		    else if (lab.equals("A1") || lab.equals("AU")){
			
			if (Author.equals("")) // don't add " and " for the first author
			    Author = val;
			else Author += " and " + val;
		    }else if (lab.equals("JA") || lab.equals("JF") || lab.equals("JO")) hm
											    .put("journal", val);
		    else if (lab.equals("SP")) StartPage = val;
		    
		    else if (lab.equals("EP")) EndPage = val;
		    
		    else if (lab.equals("VL")) hm.put("volume", val);
		    else if (lab.equals("IS")) hm.put("number", val);
		    else if (lab.equals("N2") || lab.equals("AB")) hm
								       .put("abstract", val);
		    else if (lab.equals("UR")) hm.put("url", val);
		    else if ((lab.equals("Y1") || lab.equals("PY")) && val.length() >= 4) hm
											      .put("year", val.substring(0, 4));
		    else if (lab.equals("KW")){
			if (!hm.containsKey("keywords")) hm.put("keywords", val);
			else{
			    String kw = (String) hm.get("keywords");
			    hm.put("keywords", kw + ", " + val);
			}
		    }
		}
	    }
	    // fix authors
	    Author = ImportFormatReader.fixAuthor_lastnameFirst(Author);
	    hm.put("author", Author);
	    
	    hm.put("pages", StartPage + "--" + EndPage);
	    BibtexEntry b = new BibtexEntry(Globals.DEFAULT_BIBTEXENTRY_ID, Globals
					    .getEntryType(Type)); // id assumes an existing database so don't
	    // create one here
	    b.setField(hm);
	    
	    bibitems.add(b);
	    
	}
	
	return bibitems;
    }
}



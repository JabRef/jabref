package net.sf.jabref.imports;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.Reader;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Globals;

/**
 * Importer for the ISI Web of Science format.
 */
public class IsiImporter implements ImportFormat {

    /**
     * Return the name of this import format.
     */
    public String getFormatName() {
	return "ISI";
    }

    /**
     * Check whether the source is in the correct format for this importer.
     */
    public boolean isRecognizedFormat(InputStream stream) throws IOException {

	// Our strategy is to look for the "%A  <author>" line.
	BufferedReader in = new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream));
	Pattern pat1 = Pattern
	    .compile("PY \\\\d{4}?");
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
	//Pattern fieldPattern = Pattern.compile("^AU |^TI |^SO |^DT |^C1 |^AB
	// |^ID |^BP |^PY |^SE |^PY |^VL |^IS ");
	String str;
	while ((str = in.readLine()) != null){
	    if (str.length() < 3) continue;
	    // begining of a new item
	    if (str.substring(0, 3).equals("PT ")){
		sb.append("::" + str);
	    }else{
		String beg = str.substring(0, 3).trim();
		// I could have used the fieldPattern regular expression instead
		// however this seems to be
		// quick and dirty and it works!
		if (beg.length() == 2){
		    sb.append(" ## ");// mark the begining of each field
		    sb.append(str);
		    
		}else{
		    sb.append("EOLEOL");// mark the end of each line
		    sb.append(str.substring(2, str.length()));//remove the initial " "
		    
		}
		
	    }
	}
	
	String[] entries = sb.toString().split("::");
	// skip the first entry as it is either empty or has document header
	
	HashMap hm = new HashMap();
	for (int i = 0; i < entries.length; i++){
	    String[] fields = entries[i].split(" ## ");
	    if (fields.length == 0) fields = entries[i].split("\n");
	    String Type = "", PT = "", pages = "";
	    hm.clear();
	    
	    for (int j = 0; j < fields.length; j++){
		//empty field don't do anything
		if (fields[j].length() <= 2) continue;
		String beg = fields[j].substring(0, 2);
		String value = fields[j].substring(2);
		value = value.trim();
		if (value.startsWith("-")) value = value.substring(1);
		value = value.trim();
		if (beg.equals("PT")){
		    PT = value.replaceAll("Journal", "article");
		    Type = "article"; //make all of them PT?
		}else if (beg.equals("TY")){
		    Type = "inproceedings";
		}else if (beg.equals("JO")) hm.put("booktitle", value);
		else if (beg.equals("AU")) hm.put("author",
						  ImportFormatReader.fixAuthor_lastnameFirst(value.replaceAll("EOLEOL", " and ")));
		else if (beg.equals("TI")) hm.put("title", value.replaceAll("EOLEOL",
									    " "));
		else if (beg.equals("SO")){ // journal name
		    hm.put("journal", value);
		}else if (beg.equals("ID")) hm.put("keywords", value.replaceAll(
										"EOLEOL", " "));
		else if (beg.equals("AB")) hm.put("abstract", value.replaceAll(
									       "EOLEOL", " "));
		else if (beg.equals("BP") || beg.equals("BR") || beg.equals("SP")) pages = value;
		else if (beg.equals("EP")){
		    int detpos = value.indexOf(' ');
		    // tweak for IEEE Explore
		    if (detpos != -1){
			value = value.substring(0, detpos);
		    }
		    pages = pages + "--" + value;
		}else if (beg.equals("AR")) pages = value;
		else if (beg.equals("IS")) hm.put("number", value);
		else if (beg.equals("PY")) hm.put("year", value);
		else if (beg.equals("VL")) hm.put("volume", value);
		else if (beg.equals("DT")){
		    Type = value;
		    if (!Type.equals("Article") && !PT.equals("Journal")) //Article"))
			Type = "misc";
		    else Type = "article";
		}//ignore
		else if (beg.equals("CR")) //cited references
		    hm.put("CitedReferences", value.replaceAll("EOLEOL", " ; ").trim());
	    }
	    if (!"".equals(pages))
		hm.put("pages", pages);
	    BibtexEntry b = new BibtexEntry(Globals.DEFAULT_BIBTEXENTRY_ID, Globals
					    .getEntryType(Type)); // id assumes an existing database so don't
	    // create one here
	    b.setField(hm);
	    
	    bibitems.add(b);
	}
	
	return bibitems;
	
	
    }

}

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
public class OvidImporter implements ImportFormat {

    public static Pattern ovid_src_pat = Pattern
	.compile("Source ([ \\w&\\-]+)\\.[ ]+([0-9]+)\\(([\\w\\-]+)\\):([0-9]+\\-?[0-9]+?)\\,.*([0-9][0-9][0-9][0-9])");
    
    public static Pattern ovid_src_pat_no_issue = Pattern
	.compile("Source ([ \\w&\\-]+)\\.[ ]+([0-9]+):([0-9]+\\-?[0-9]+?)\\,.*([0-9][0-9][0-9][0-9])");
    
    //   public static Pattern ovid_pat_inspec= Pattern.compile("Source ([
    // \\w&\\-]+)");


    /**
     * Return the name of this import format.
     */
    public String getFormatName() {
	return "Ovid";
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
	ArrayList bibitems = new ArrayList();
	StringBuffer sb = new StringBuffer();
	BufferedReader in = new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream));
	String line;
	while ((line = in.readLine()) != null){
	    if (line.length() > 0 && line.charAt(0) != ' '){
		sb.append("__NEWFIELD__");
	    }
	    sb.append(line);
	}

	String items[] = sb.toString().split("<[0-9]+>");
	
	for (int i = 1; i < items.length; i++){
	    HashMap h = new HashMap();
	    String[] fields = items[i].split("__NEWFIELD__");
	    for (int j = 0; j < fields.length; j++){
		fields[j] = fields[j].trim();
		if (fields[j].indexOf("Author") == 0
		    && fields[j].indexOf("Author Keywords") == -1
		    && fields[j].indexOf("Author e-mail") == -1){
		    String author;
		    boolean isComma = false;
		    if (fields[j].indexOf(";") > 0){ //LN FN; [LN FN;]*
			author = fields[j].substring(7, fields[j].length()).replaceAll(
										       "[^\\.A-Za-z,;\\- ]", "").replaceAll(";", " and ");
		    }else{// LN FN. [LN FN.]*
			isComma = true;
			author = fields[j].substring(7, fields[j].length()).replaceAll(
										       "\\.", " and").replaceAll(" and$", "");
			
		    }
		    if (author.split(" and ").length > 1){ // single author or no ";"
			h.put("author", ImportFormatReader.fixAuthor_lastnameFirst(author));
			/*
			 * if(isComma==false)
			 * 
			 * else h.put("author", fixAuthor_nocomma( author) );
			 */
		    }else h.put("author", author);
		}else if (fields[j].indexOf("Title") == 0) h.put("title", fields[j]
								 .substring(6, fields[j].length()).replaceAll("\\[.+\\]", ""));
		else if (fields[j].indexOf("Source") == 0){
		    System.out.println(fields[j]);
		    String s = fields[j];
		    Matcher matcher = ovid_src_pat.matcher(s);
		    boolean matchfound = matcher.find();
		    if (matchfound){
			h.put("journal", matcher.group(1));
			h.put("volume", matcher.group(2));
			h.put("issue", matcher.group(3));
			h.put("pages", matcher.group(4));
			h.put("year", matcher.group(5));
		    }else{// may be missing the issue
			matcher = ovid_src_pat_no_issue.matcher(s);
			matchfound = matcher.find();
			if (matchfound){
			    h.put("journal", matcher.group(1));
			    h.put("volume", matcher.group(2));
			    h.put("pages", matcher.group(3));
			    h.put("year", matcher.group(4));
			}
		    }
		    
		}else if (fields[j].indexOf("Abstract") == 0) h.put("abstract",
								    fields[j].substring(9, fields[j].length()));
		//else if(fields[j].indexOf("References")==0)
		//	h.put("references", fields[j].substring( 11,fields[j].length()));
	    }
	    BibtexEntry b = new BibtexEntry(Globals.DEFAULT_BIBTEXENTRY_ID, Globals
					    .getEntryType("article")); // id assumes an existing database so
	    // don't create one here
	    b.setField(h);
	    
	    bibitems.add(b);
	    
	}
	
	return bibitems;
    }
}



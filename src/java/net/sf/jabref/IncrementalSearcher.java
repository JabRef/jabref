package net.sf.jabref;

import java.util.regex.Pattern;

public class IncrementalSearcher {

    JabRefPreferences prefs;
    private String hitInField;

    public IncrementalSearcher(JabRefPreferences prefs) {
	this.prefs = prefs;
    }

    public String getField() {
	return hitInField;
    }

    public boolean search(String pattern, BibtexEntry bibtexEntry) {
	    hitInField = null;
	    //if (!prefs.getBoolean("caseSensitiveSearch"))
	    //    flags = Pattern.CASE_INSENSITIVE;
	    //Pattern pattern = Pattern.compile(searchString, flags);
	
	    Object[] fields = bibtexEntry.getAllFields();
	    return searchFields(fields, bibtexEntry, pattern);

    }

	protected boolean searchFields(Object[] fields, BibtexEntry bibtexEntry, 
				       String searchString) {
	    boolean found = false;
	    if (fields != null) {
		for(int i = 0 ; i < fields.length ; i++){
		    try {
			/*Globals.logger("Searching field '"+fields[i].toString()
				       +"' for '"
				       +pattern.toString()+"'.");*/
			if (bibtexEntry.getField(fields[i].toString()) != null) {
			    if (prefs.getBoolean("caseSensitiveSearch")) {
				if (bibtexEntry.getField(fields[i].toString())
				    .toString().indexOf(searchString) > -1)
				    found = true;
			    } else {
				if (bibtexEntry.getField(fields[i].toString())
				    .toString().toLowerCase()
				    .indexOf(searchString.toLowerCase()) > -1)
				    found = true;
			    }
			    
			    if (found) {
				hitInField = fields[i].toString();
				return true;
			    }
			}
		    }			
		    catch(Throwable t ){
			System.err.println("Searching error: "+t) ; 
		    }
		}  
	    }
	    return false;
	}
}


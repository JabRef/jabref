package net.sf.jabref;

import java.util.Set;


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
	    return searchFields(bibtexEntry.getAllFields(), bibtexEntry, pattern);
    }

	protected boolean searchFields(Set<String> fields, BibtexEntry bibtexEntry, 
				       String searchString) {
	    boolean found = false;
	    if (fields != null) {
	    	
	    	for (String field : fields){
		    try {
			/*Globals.logger("Searching field '"+fields[i].toString()
				       +"' for '"
				       +pattern.toString()+"'.");*/
			if (bibtexEntry.getField(field.toString()) != null) {
			    if (prefs.getBoolean("caseSensitiveSearch")) {
				if (bibtexEntry.getField(field.toString())
				    .toString().indexOf(searchString) > -1)
				    found = true;
			    } else {
				if (bibtexEntry.getField(field.toString())
				    .toString().toLowerCase()
				    .indexOf(searchString.toLowerCase()) > -1)
				    found = true;
			    }
			    
			    if (found) {
				hitInField = field.toString();
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


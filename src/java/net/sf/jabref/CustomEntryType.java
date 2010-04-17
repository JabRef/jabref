package net.sf.jabref;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

/**
 * This class is used to represent customized entry types.
 *
 */
public class CustomEntryType extends BibtexEntryType {

    private String name;
    private String[] req, opt;
    private String[][] reqSets = null; // Sets of either-or required fields, if any

    public CustomEntryType(String name_, String[] req_, String[] opt_) {
        name = name_;
        parseRequiredFields(req_);
        opt = opt_;
    }

    public CustomEntryType(String name_, String reqStr, String optStr) {
        name = name_;
        if (reqStr.length() == 0)
            req = new String[0];
        else {
            parseRequiredFields(reqStr);

        }
        if (optStr.length() == 0)
            opt = new String[0];
        else
            opt = optStr.split(";");
    }

    protected void parseRequiredFields(String reqStr) {
        String[] parts = reqStr.split(";");
        parseRequiredFields(parts);
    }

    protected void parseRequiredFields(String[] parts) {
        ArrayList<String> fields = new ArrayList<String>();
        ArrayList<String[]> sets = new ArrayList<String[]>();
        for (int i = 0; i < parts.length; i++) {
            String[] subParts = parts[i].split("/");
            for (int j = 0; j < subParts.length; j++) {
                fields.add(subParts[j]);
            }
            // Check if we have either/or fields:
            if (subParts.length > 1) {
                sets.add(subParts);
            }
        }
        req = fields.toArray(new String[fields.size()]);
        if (sets.size() > 0) {
            reqSets = sets.toArray(new String[sets.size()][]);
        }
    }

    public String getName() {
	return name;
    }

    public String[] getOptionalFields() {
	return opt;
    }
    public String[] getRequiredFields() {
	return req;
    }

    public String[] getRequiredFieldsForCustomization() {
        return getRequiredFieldsString().split(";");
    }

    //    public boolean isTemporary

    public String describeRequiredFields() {
	StringBuffer sb = new StringBuffer();
	for (int i=0; i<req.length; i++) {
	    sb.append(req[i]);
	    sb.append(((i<=req.length-1)&&(req.length>1))?", ":"");
	}
	return sb.toString();
    }

    public String describeOptionalFields() {
	StringBuffer sb = new StringBuffer();
	for (int i=0; i<opt.length; i++) {
	    sb.append(opt[i]);
	    sb.append(((i<=opt.length-1)&&(opt.length>1))?", ":"");
	}
	return sb.toString();
    }

    /**
     * Check whether this entry's required fields are set, taking crossreferenced entries and
     * either-or fields into account:
     * @param entry The entry to check.
     * @param database The entry's database.
     * @return True if required fields are set, false otherwise.
     */
    public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
        // First check if the bibtex key is set:
        if (entry.getField(BibtexFields.KEY_FIELD) == null)
                return false;
        // Then check other fields:
        boolean[] isSet = new boolean[req.length];
        // First check for all fields, whether they are set here or in a crossref'd entry:
	    for (int i=0; i<req.length; i++)
            isSet[i] = BibtexDatabase.getResolvedField(req[i], entry, database) != null;
        // Then go through all fields. If a field is not set, see if it is part of an either-or
        // set where another field is set. If not, return false:
    	for (int i=0; i<req.length; i++) {
            if (!isSet[i]) {
                if (!isCoupledFieldSet(req[i], entry, database))
                    return false;
            }
        }
        // Passed all fields, so return true:
        return true;
    }

    protected boolean isCoupledFieldSet(String field, BibtexEntry entry, BibtexDatabase database) {
        if (reqSets == null)
            return false;
        for (int i=0; i<reqSets.length; i++) {
            boolean takesPart = false, oneSet = false;
            for (int j=0; j<reqSets[i].length; j++) {
                // If this is the field we're looking for, note that the field is part of the set:
                if (reqSets[i][j].equalsIgnoreCase(field))
                    takesPart = true;
                // If it is a different field, check if it is set:
                else if (BibtexDatabase.getResolvedField(reqSets[i][j], entry, database) != null)
                    oneSet = true;
            }
            // Ths the field is part of the set, and at least one other field is set, return true:
            if (takesPart && oneSet)
                return true;
        }
        // No hits, so return false:
        return false;
    }

    /**
     * Get a String describing the required field set for this entry type.
     * @return Description of required field set for storage in preferences or bib file.
     */
    public String getRequiredFieldsString() {
        StringBuilder sb = new StringBuilder();
        int reqSetsPiv = 0;
        for (int i=0; i<req.length; i++) {
            if ((reqSets == null) || (reqSetsPiv == reqSets.length)) {
                sb.append(req[i]);
            }
            else if (req[i].equals(reqSets[reqSetsPiv][0])) {
                for (int j = 0; j < reqSets[reqSetsPiv].length; j++) {
                    sb.append(reqSets[reqSetsPiv][j]);
                    if (j < reqSets[reqSetsPiv].length-1)
                        sb.append("/");
                }
                // Skip next n-1 fields:
                i += reqSets[reqSetsPiv].length-1;
                reqSetsPiv++;
            }
            else sb.append(req[i]);
            if (i < req.length-1)
                sb.append(";");

        }
        return sb.toString();
    }


    public void save(Writer out) throws IOException {
	out.write("@comment{");
	out.write(GUIGlobals.ENTRYTYPE_FLAG);
	out.write(getName());
	out.write(": req[");
    out.write(getRequiredFieldsString());
	/*StringBuffer sb = new StringBuffer();
	for (int i=0; i<req.length; i++) {
	    sb.append(req[i]);
	    if (i<req.length-1)
		sb.append(";");
	}
	out.write(sb.toString());*/
	out.write("] opt[");
	StringBuilder sb = new StringBuilder();
	for (int i=0; i<opt.length; i++) {
	    sb.append(opt[i]);
	    if (i<opt.length-1)
		sb.append(";");
	}
	out.write(sb.toString());
	out.write("]}"+Globals.NEWLINE);
    }

    public static CustomEntryType parseEntryType(String comment) { 
	try {
	    //if ((comment.length() < 9+GUIGlobals.ENTRYTYPE_FLAG.length())
	    //	|| comment
	    //System.out.println(">"+comment+"<");
	    String rest;
	    rest = comment.substring(GUIGlobals.ENTRYTYPE_FLAG.length());
	    int nPos = rest.indexOf(':');
	    String name = rest.substring(0, nPos);
	    rest = rest.substring(nPos+2);

	    int rPos = rest.indexOf(']');
	    if (rPos < 4)
		throw new IndexOutOfBoundsException();
	    String reqFields = rest.substring(4, rPos);
	    //System.out.println(name+"\nr '"+reqFields+"'");
	    int oPos = rest.indexOf(']', rPos+1);
	    String optFields = rest.substring(rPos+6, oPos);
	    //System.out.println("o '"+optFields+"'");
	    return new CustomEntryType(name, reqFields, optFields);
	} catch (IndexOutOfBoundsException ex) {
	    Globals.logger("Ill-formed entrytype comment in BibTeX file.");
	    return null;
	}

    }
}

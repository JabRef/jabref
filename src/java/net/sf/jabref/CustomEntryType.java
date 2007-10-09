package net.sf.jabref;

import java.io.IOException;
import java.io.Writer;

/**
 * This class is used to represent customized entry types.
 *
 */
public class CustomEntryType extends BibtexEntryType {

    private String name;
    private String[] req, opt;

    public CustomEntryType(String name_, String[] req_, String[] opt_) {
	name = name_;
	req = req_;
	opt = opt_;
    }

    public CustomEntryType(String name_, String reqStr, String optStr) {
	name = name_;
	if (reqStr.length() == 0)
	    req = new String[0];
	else
	    req = reqStr.split(";");
	if (optStr.length() == 0)
	    opt = new String[0];
	else
	    opt = optStr.split(";");	
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

    public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
	for (int i=0; i<req.length; i++)
        if (BibtexDatabase.getResolvedField(req[i], entry, database) == null) return false;
    	return true;
    }

    public void save(Writer out) throws IOException {
	out.write("@comment{");
	out.write(GUIGlobals.ENTRYTYPE_FLAG);
	out.write(getName());
	out.write(": req[");
	StringBuffer sb = new StringBuffer();
	for (int i=0; i<req.length; i++) {
	    sb.append(req[i]);
	    if (i<req.length-1)
		sb.append(";");
	}
	out.write(sb.toString());
	out.write("] opt[");
	sb = new StringBuffer();
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

package net.sf.jabref;

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
	req = reqStr.split(";");
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

    public boolean hasAllRequiredFields(BibtexEntry entry) {
	for (int i=0; i<req.length; i++)
	    if (entry.getField(req[i]) == null) return false;
	return true;
    }
}
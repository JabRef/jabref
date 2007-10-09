package net.sf.jabref;

import java.io.Writer;

/**
 * This class is used to represent an unknown entry type, e.g. encountered
 * during bibtex parsing. The only known information is the type name.
 * This is useful if the bibtex file contains type definitions that are used
 * in the file - because the entries will be parsed before the type definitions
 * are found. In the meantime, the entries will be assigned an 
 * UnknownEntryType giving the name.
 */
public class UnknownEntryType extends BibtexEntryType {

    private String name;
    private String[] fields = new String[0];

    public UnknownEntryType(String name_) {
	name = name_;
    }

    public String getName() {
	return name;
    }

    public String[] getOptionalFields() {
	return fields;
    }
    public String[] getRequiredFields() {
	return fields;
    }


    public String describeRequiredFields() {
	return "unknown";
    }

    public String describeOptionalFields() {
	return "unknown";
    }

    public boolean hasAllRequiredFields(BibtexEntry entry, BibtexDatabase database) {
	return true;
    }

    public void save(Writer out) {
    }

}

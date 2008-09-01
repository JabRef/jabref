package net.sf.jabref.external;


/**
 * This subclass of ExternalFileType is used to mark types that are unknown.
 * This can be the case when a database is loaded which contains links to files
 * of a type that has not been defined on this JabRef instance.
 */
public class UnknownExternalFileType extends ExternalFileType {

    public UnknownExternalFileType(String name) {
        super(name, "", "", "", "unknown");
    }
    
}

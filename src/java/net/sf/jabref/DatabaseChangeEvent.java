package net.sf.jabref;


public class DatabaseChangeEvent {

    static class ChangeType {
    }

    public static ChangeType ADDED_ENTRY = new ChangeType(),
	REMOVED_ENTRY = new ChangeType(),
	CHANGED_ENTRY = new ChangeType(),
	CHANGING_ENTRY = new ChangeType();

    private BibtexEntry entry;
    private ChangeType type;
    private BibtexDatabase source;

    public DatabaseChangeEvent(BibtexDatabase source, ChangeType type, 
			       BibtexEntry entry) {
	this.source = source;
	this.type = type;
	this.entry = entry;
    }

    public BibtexDatabase getSource() {
	return source;
    }

    public BibtexEntry getEntry() {
	return entry;
    }

    public ChangeType getType() {
	return type;
    }
}

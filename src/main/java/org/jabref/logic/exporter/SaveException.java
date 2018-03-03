package org.jabref.logic.exporter;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;

/**
 * Exception thrown if saving goes wrong. If caused by a specific
 * entry, keeps track of which entry caused the problem.
 */
public class SaveException extends Exception {

    public static final SaveException FILE_LOCKED = new SaveException(
            "Could not save, file locked by another JabRef instance.",
            Localization.lang("Could not save, file locked by another JabRef instance."));
    public static final SaveException BACKUP_CREATION = new SaveException("Unable to create backup",
            Localization.lang("Unable to create backup"));

    private final BibEntry entry;
    private int status;
    private String localizedMessage;

    public SaveException(String message) {
        super(message);
        entry = null;
    }

    public SaveException(String message, String localizedMessage) {
        super(message);
        this.localizedMessage = localizedMessage;
        entry = null;
    }

    public SaveException(String message, int status) {
        super(message);
        entry = null;
        this.status = status;
    }

    public SaveException(String message, BibEntry entry) {
        super(message);
        this.entry = entry;
    }

    public SaveException(String message, String localizedMessage, BibEntry entry) {
        super(message);
        this.localizedMessage = localizedMessage;
        this.entry = entry;
    }

    public SaveException(Throwable base) {
        this(base.getMessage(), base.getLocalizedMessage());
    }

    public SaveException(Throwable base, BibEntry entry) {
        this(base.getMessage(), base.getLocalizedMessage(), entry);
    }

    public int getStatus() {
        return status;
    }

    public BibEntry getEntry() {
        return entry;
    }

    public boolean specificEntry() {
        return entry != null;
    }

    @Override
    public String getLocalizedMessage() {
        if (localizedMessage == null) {
            return getMessage();
        } else {
            return localizedMessage;
        }
    }
}

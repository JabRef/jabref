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

    private BibEntry entry;
    private int status;
    private String localizedMessage;

    public SaveException(String message) {
        super(message);
        entry = null;
    }

    public SaveException(String message, Throwable exception) {
        super(message, exception);
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

    public SaveException(String message, String localizedMessage, BibEntry entry, Throwable base) {
        super(message, base);
        this.localizedMessage = localizedMessage;
        this.entry = entry;
    }

    public SaveException(Throwable base) {
        super(base.getMessage(), base);
    }

    public SaveException(Throwable base, BibEntry entry) {
        this(base.getMessage(), base.getLocalizedMessage(), entry, base);
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

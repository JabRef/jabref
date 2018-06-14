package org.jabref.gui.push;

import java.util.List;

import org.jabref.gui.BasePanel;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.metadata.MetaData;

/**
 * Class that defines interaction with an external application in the form of "pushing" selected entries to it.
 */
public interface PushToApplication {

    String getName();

    String getApplicationName();

    String getTooltip();

    JabRefIcon getIcon();

    /**
     * The actual operation. This method will not be called on the event dispatch thread, so it should not do GUI
     * operations without utilizing invokeLater().
     *
     * @param database
     * @param entries
     * @param metaData
     */
    void pushEntries(BibDatabase database, List<BibEntry> entries, String keyString, MetaData metaData);

    /**
     * Reporting etc., this method is called on the event dispatch thread after pushEntries() returns.
     */
    void operationCompleted(BasePanel panel);

    /**
     * Check whether this operation requires BibTeX keys to be set for the entries. If true is returned an error message
     * will be displayed if keys are missing.
     *
     * @return true if BibTeX keys are required for this operation.
     */
    boolean requiresBibtexKeys();

}

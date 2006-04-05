package net.sf.jabref.external;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BasePanel;

import javax.swing.*;

/**
 * Class that defines interaction with an external application in the form of
 * "pushing" selected entries to it.
 */
public interface PushToApplication {

    public String getName();

    public String getTooltip();

    public Icon getIcon();

    public String getKeyStrokeName();
    /**
     * The actual operation. This method will not be called on the event dispatch
     * thread, so it should not do GUI operations without utilizing invokeLater().
     * @param entries
     */
    public void pushEntries(BibtexEntry[] entries, String keyString);

    /**
     * Reporting etc., this method is called on the event dispatch thread after
     * pushEntries() returns.
     */
    public void operationCompleted(BasePanel panel);

    /**
     * Check whether this operation requires BibTeX keys to be set for the entries.
     * If true is returned an error message will be displayed if keys are missing.
     * @return true if BibTeX keys are required for this operation.
     */
    public boolean requiresBibtexKeys();


}

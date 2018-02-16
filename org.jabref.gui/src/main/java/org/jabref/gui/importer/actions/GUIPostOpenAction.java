package org.jabref.gui.importer.actions;

import org.jabref.gui.BasePanel;
import org.jabref.logic.importer.ParserResult;

/**
 * This interface defines potential actions that may need to be taken after
 * opening a BIB file into JabRef. This can for instance be file upgrade actions
 * that should be offered due to new features in JabRef, and may depend on e.g.
 * which JabRef version the file was last written by.
 *
 * This interface is introduced in an attempt to add such functionality in a
 * flexible manner.
 */
public interface GUIPostOpenAction {

    /**
     * This method is queried in order to find out whether the action needs to be
     * performed or not.
     * @param pr The result of the BIB parse operation.
     * @return true if the action should be called, false otherwise.
     */
    boolean isActionNecessary(ParserResult pr);

    /**
     * This method is called after the new database has been added to the GUI, if
     * the isActionNecessary() method returned true.
     *
     * Note: if several such methods need to be called sequentially, it is
     *       important that all implementations of this method do not return
     *       until the operation is finished. If work needs to be off-loaded
     *       into a worker thread, use Spin to do this synchronously.
     *
     * @param panel The BasePanel where the database is shown.
     * @param pr The result of the BIB parse operation.
     */
    void performAction(BasePanel panel, ParserResult pr);
}

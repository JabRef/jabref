package org.jabref.gui.externalfiles;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class AutoSetLinks {

    private static final Log LOGGER = LogFactory.getLog(AutoSetLinks.class);

    private AutoSetLinks() {
    }

    /**
     * Shortcut method if links are set without using the GUI
     *
     * @param entries  the entries for which links should be set
     * @param databaseContext the database for which links are set
     */
    public static void autoSetLinks(List<BibEntry> entries, BibDatabaseContext databaseContext) {
        autoSetLinks(entries, null, null, databaseContext, null, null);
    }

    /**
     * Automatically add links for this set of entries, based on the globally stored list of external file types. The
     * entries are modified, and corresponding UndoEdit elements added to the NamedCompound given as argument.
     * Furthermore, all entries which are modified are added to the Set of entries given as an argument.
     * <p>
     * The entries' bibtex keys must have been set - entries lacking key are ignored. The operation is done in a new
     * thread, which is returned for the caller to wait for if needed.
     *
     * @param entries          A collection of BibEntry objects to find links for.
     * @param ce               A NamedCompound to add UndoEdit elements to.
     * @param changedEntries   MODIFIED, optional. A Set of BibEntry objects to which all modified entries is added.
     *                         This is used for status output and debugging
     * @param singleTableModel UGLY HACK. The table model to insert links into. Already existing links are not
     *                         duplicated or removed. This parameter has to be null if entries.count() != 1. The hack has been
     *                         introduced as a bibtexentry does not (yet) support the function getListTableModel() and the
     *                         FileListEntryEditor editor holds an instance of that table model and does not reconstruct it after the
     *                         search has succeeded.
     * @param databaseContext  The database providing the relevant file directory, if any.
     * @param callback         An ActionListener that is notified (on the event dispatch thread) when the search is finished.
     *                         The ActionEvent has id=0 if no new links were added, and id=1 if one or more links were added. This
     *                         parameter can be null, which means that no callback will be notified.
     * @param diag             An instantiated modal JDialog which will be used to display the progress of the automatically setting. This
     *                         parameter can be null, which means that no progress update will be shown.
     * @return the thread performing the automatically setting
     */
    public static Runnable autoSetLinks(final List<BibEntry> entries, final NamedCompound ce,
            final Set<BibEntry> changedEntries,
            final BibDatabaseContext databaseContext, final ActionListener callback, final JDialog diag) {
        final Collection<ExternalFileType> types = ExternalFileTypes.getInstance().getExternalFileTypeSelection();
        if (diag != null) {
            final JProgressBar prog = new JProgressBar(SwingConstants.HORIZONTAL, 0, types.size() - 1);
            final JLabel label = new JLabel(Localization.lang("Searching for files"));
            prog.setIndeterminate(true);
            prog.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            diag.setTitle(Localization.lang("Automatically setting file links"));
            diag.getContentPane().add(prog, BorderLayout.CENTER);
            diag.getContentPane().add(label, BorderLayout.SOUTH);

            diag.pack();
            diag.setLocationRelativeTo(diag.getParent());
        }

        Runnable r = () -> {

            AutoSetFileLinksUtil util = new AutoSetFileLinksUtil();

            FieldChange change = util.findassociatedNotLinkedFiles(entries, databaseContext);
            if (ce != null) {
                // store undo information
                UndoableFieldChange fieldChange = new UndoableFieldChange(change);
                ce.addEdit(fieldChange);
            }

            if (changedEntries != null) {
                changedEntries.add(change.getEntry());
            }

            final int id = change.getNewValue() != null ? 1 : 0;
            SwingUtilities.invokeLater(() -> {

                if (diag != null) {
                    diag.dispose();
                }
                if (callback != null) {
                    callback.actionPerformed(new ActionEvent(AutoSetLinks.class, id, ""));
                }

            });

        };

        SwingUtilities.invokeLater(() -> {
            // show dialog which will be hidden when the task is done
            if (diag != null) {
                diag.setVisible(true);
            }
        });

        return r;
    }

    /**
     * Automatically add links for this entry to the table model given as an argument, based on the globally stored list
     * of external file types. The entry itself is not modified. The entry's bibtex key must have been set.
     *
     * @param entry            The BibEntry to find links for.
     * @param singleTableModel The table model to insert links into. Already existing links are not duplicated or
     *                         removed.
     * @param databaseContext  The database providing the relevant file directory, if any.
     * @param callback         An ActionListener that is notified (on the event dispatch thread) when the search is finished.
     *                         The ActionEvent has id=0 if no new links were added, and id=1 if one or more links were added. This
     *                         parameter can be null, which means that no callback will be notified. The passed ActionEvent is
     *                         constructed with (this, id, ""), where id is 1 if something has been done and 0 if nothing has been
     *                         done.
     * @param diag             An instantiated modal JDialog which will be used to display the progress of the automatically setting. This
     *                         parameter can be null, which means that no progress update will be shown.
     * @return the runnable able to perform the automatically setting
     */
    public static Runnable autoSetLinks(final BibEntry entry,
            final BibDatabaseContext databaseContext, final ActionListener callback, final JDialog diag) {
        return autoSetLinks(Collections.singletonList(entry), null, null, databaseContext, callback,
                diag);
    }

}

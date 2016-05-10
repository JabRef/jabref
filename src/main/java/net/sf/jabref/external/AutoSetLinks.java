package net.sf.jabref.external;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.gui.FileListEntry;
import net.sf.jabref.gui.FileListTableModel;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.undo.UndoableFieldChange;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.model.entry.BibEntry;

public class AutoSetLinks {

    /**
     * Shortcut method if links are set without using the GUI
     *
     * @param entries  the entries for which links should be set
     * @param databaseContext the database for which links are set
     */
    public static void autoSetLinks(Collection<BibEntry> entries, BibDatabaseContext databaseContext) {
        autoSetLinks(entries, null, null, null, databaseContext, null, null);
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
    public static Runnable autoSetLinks(final Collection<BibEntry> entries, final NamedCompound ce, final Set<BibEntry> changedEntries, final FileListTableModel singleTableModel, final BibDatabaseContext databaseContext, final ActionListener callback, final JDialog diag) {
        final Collection<ExternalFileType> types = ExternalFileTypes.getInstance().getExternalFileTypeSelection();
        if (diag != null) {
            final JProgressBar prog = new JProgressBar(JProgressBar.HORIZONTAL, 0, types.size() - 1);
            final JLabel label = new JLabel(Localization.lang("Searching for files"));
            prog.setIndeterminate(true);
            prog.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            diag.setTitle(Localization.lang("Automatically setting file links"));
            diag.getContentPane().add(prog, BorderLayout.CENTER);
            diag.getContentPane().add(label, BorderLayout.SOUTH);
    
            diag.pack();
            diag.setLocationRelativeTo(diag.getParent());
        }
    
        Runnable r = new Runnable() {
    
            @Override
            public void run() {
                // determine directories to search in
                List<File> dirs = new ArrayList<>();
                List<String> dirsS = databaseContext.getFileDirectory();
                dirs.addAll(dirsS.stream().map(File::new).collect(Collectors.toList()));
    
                // determine extensions
                Collection<String> extensions = new ArrayList<>();
                for (final ExternalFileType type : types) {
                    extensions.add(type.getExtension());
                }
    
                // Run the search operation:
                Map<BibEntry, List<File>> result;
                if (Globals.prefs.getBoolean(JabRefPreferences.AUTOLINK_USE_REG_EXP_SEARCH_KEY)) {
                    String regExp = Globals.prefs.get(JabRefPreferences.REG_EXP_SEARCH_EXPRESSION_KEY);
                    result = RegExpFileSearch.findFilesForSet(entries, extensions, dirs, regExp);
                } else {
                    result = FileUtil.findAssociatedFiles(entries, extensions, dirs);
                }
    
                boolean foundAny = false;
                // Iterate over the entries:
                for (Entry<BibEntry, List<File>> entryFilePair : result.entrySet()) {
                    FileListTableModel tableModel;
                    String oldVal = entryFilePair.getKey().getField(Globals.FILE_FIELD);
                    if (singleTableModel == null) {
                        tableModel = new FileListTableModel();
                        if (oldVal != null) {
                            tableModel.setContent(oldVal);
                        }
                    } else {
                        assert entries.size() == 1;
                        tableModel = singleTableModel;
                    }
                    List<File> files = entryFilePair.getValue();
                    for (File f : files) {
                        f = FileUtil.shortenFileName(f, dirsS);
                        boolean alreadyHas = false;
                        //System.out.println("File: "+f.getPath());
                        for (int j = 0; j < tableModel.getRowCount(); j++) {
                            FileListEntry existingEntry = tableModel.getEntry(j);
                            //System.out.println("Comp: "+existingEntry.getLink());
                            if (new File(existingEntry.link).equals(f)) {
                                alreadyHas = true;
                                foundAny = true;
                                break;
                            }
                        }
                        if (!alreadyHas) {
                            foundAny = true;
                            Optional<ExternalFileType> type;
                            Optional<String> extension = FileUtil.getFileExtension(f);
                            if (extension.isPresent()) {
                                type = ExternalFileTypes.getInstance().getExternalFileTypeByExt(extension.get());
                            } else {
                                type = Optional.of(new UnknownExternalFileType(""));
                            }
                            FileListEntry flEntry = new FileListEntry(f.getName(), f.getPath(), type);
                            tableModel.addEntry(tableModel.getRowCount(), flEntry);
    
                            String newVal = tableModel.getStringRepresentation();
                            if (newVal.isEmpty()) {
                                newVal = null;
                            }
                            if (ce != null) {
                                // store undo information
                                UndoableFieldChange change = new UndoableFieldChange(entryFilePair.getKey(),
                                        Globals.FILE_FIELD, oldVal, newVal);
                                ce.addEdit(change);
                            }
                            // hack: if table model is given, do NOT modify entry
                            if (singleTableModel == null) {
                                entryFilePair.getKey().setField(Globals.FILE_FIELD, newVal);
                            }
                            if (changedEntries != null) {
                                changedEntries.add(entryFilePair.getKey());
                            }
                        }
                    }
                }
    
                // handle callbacks and dialog
                // FIXME: The ID signals if action was successful :/
                final int id = foundAny ? 1 : 0;
                SwingUtilities.invokeLater(new Runnable() {
    
                    @Override
                    public void run() {
                        if (diag != null) {
                            diag.dispose();
                        }
                        if (callback != null) {
                            callback.actionPerformed(new ActionEvent(this, id, ""));
                        }
                    }
                });
            }
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
    public static Runnable autoSetLinks(final BibEntry entry, final FileListTableModel singleTableModel, final BibDatabaseContext databaseContext, final ActionListener callback, final JDialog diag) {
        return autoSetLinks(Collections.singletonList(entry), null, null, singleTableModel, databaseContext, callback,
                diag);
    }

}

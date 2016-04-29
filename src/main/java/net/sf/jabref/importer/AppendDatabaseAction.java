/*  Copyright (C) 2003-2011 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.importer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefExecutorService;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.MetaData;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.FileDialogs;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.MergeDialog;
import net.sf.jabref.gui.actions.BaseAction;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.undo.UndoableInsertEntry;
import net.sf.jabref.gui.undo.UndoableInsertString;
import net.sf.jabref.importer.fileformat.ParseException;
import net.sf.jabref.logic.groups.AllEntriesGroup;
import net.sf.jabref.logic.groups.ExplicitGroup;
import net.sf.jabref.logic.groups.GroupHierarchyType;
import net.sf.jabref.logic.groups.GroupTreeNode;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.UpdateField;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.KeyCollisionException;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexString;
import net.sf.jabref.model.entry.IdGenerator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: May 18, 2006
 * Time: 9:49:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class AppendDatabaseAction implements BaseAction {

    private final JabRefFrame frame;
    private final BasePanel panel;
    private final List<File> filesToOpen = new ArrayList<>();

    private static final Log LOGGER = LogFactory.getLog(AppendDatabaseAction.class);


    public AppendDatabaseAction(JabRefFrame frame, BasePanel panel) {
        this.frame = frame;
        this.panel = panel;
    }

    @Override
    public void action() {

        filesToOpen.clear();
        final MergeDialog md = new MergeDialog(frame, Localization.lang("Append database"), true);
        md.setLocationRelativeTo(panel);
        md.setVisible(true);
        if (md.isOkPressed()) {
            List<String> chosen = FileDialogs.getMultipleFiles(frame,
                    new File(Globals.prefs.get(JabRefPreferences.WORKING_DIRECTORY)),
                    null, false);
            //String chosenFile = Globals.getNewFile(frame, new File(Globals.prefs.get("workingDirectory")),
            //                                       null, JFileChooser.OPEN_DIALOG, false);
            if (chosen.isEmpty()) {
                return;
            }
            for (String aChosen : chosen) {
                filesToOpen.add(new File(aChosen));
            }

            // Run the actual open in a thread to prevent the program
            // locking until the file is loaded.
            JabRefExecutorService.INSTANCE.execute(new Runnable() {

                @Override
                public void run() {
                    openIt(md.importEntries(), md.importStrings(),
                            md.importGroups(), md.importSelectorWords());
                }

            });
            //frame.getFileHistory().newFile(panel.fileToOpen.getPath());
        }

    }

    private void openIt(boolean importEntries, boolean importStrings,
            boolean importGroups, boolean importSelectorWords) {
        if (filesToOpen.isEmpty()) {
            return;
        }
        for (File file : filesToOpen) {
            try {
                Globals.prefs.put(JabRefPreferences.WORKING_DIRECTORY, file.getPath());
                // Should this be done _after_ we know it was successfully opened?
                Charset encoding = Globals.prefs.getDefaultEncoding();
                ParserResult pr = OpenDatabaseAction.loadDatabase(file, encoding);
                AppendDatabaseAction.mergeFromBibtex(frame, panel, pr, importEntries, importStrings,
                        importGroups, importSelectorWords);
                panel.output(Localization.lang("Imported from database") + " '" + file.getPath() + "'");
            } catch (IOException | KeyCollisionException ex) {
                LOGGER.warn("Could not open database", ex);
                JOptionPane.showMessageDialog(panel, ex.getMessage(), Localization.lang("Open database"),
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static void mergeFromBibtex(JabRefFrame frame, BasePanel panel, ParserResult pr,
            boolean importEntries, boolean importStrings,
            boolean importGroups, boolean importSelectorWords)
                    throws KeyCollisionException {

        BibDatabase fromDatabase = pr.getDatabase();
        List<BibEntry> appendedEntries = new ArrayList<>();
        List<BibEntry> originalEntries = new ArrayList<>();
        BibDatabase database = panel.getDatabase();

        NamedCompound ce = new NamedCompound(Localization.lang("Append database"));
        MetaData meta = pr.getMetaData();

        if (importEntries) { // Add entries
            boolean overwriteOwner = Globals.prefs.getBoolean(JabRefPreferences.OVERWRITE_OWNER);
            boolean overwriteTimeStamp = Globals.prefs.getBoolean(JabRefPreferences.OVERWRITE_TIME_STAMP);

            for (BibEntry originalEntry : fromDatabase.getEntries()) {
                BibEntry be = (BibEntry) originalEntry.clone();
                be.setId(IdGenerator.next());
                UpdateField.setAutomaticFields(be, overwriteOwner, overwriteTimeStamp);
                database.insertEntry(be);
                appendedEntries.add(be);
                originalEntries.add(originalEntry);
                ce.addEdit(new UndoableInsertEntry(database, be, panel));
            }
        }

        if (importStrings) {
            for (BibtexString bs : fromDatabase.getStringValues()) {
                if (!database.hasStringLabel(bs.getName())) {
                    database.addString(bs);
                    ce.addEdit(new UndoableInsertString(panel, database, bs));
                }
            }
        }

        if (importGroups) {
            GroupTreeNode newGroups = meta.getGroups();
            if (newGroups != null) {

                // ensure that there is always only one AllEntriesGroup
                if (newGroups.getGroup() instanceof AllEntriesGroup) {
                    // create a dummy group
                    ExplicitGroup group = null;
                    try {
                        group = new ExplicitGroup("Imported", GroupHierarchyType.INDEPENDENT);
                    } catch (ParseException e) {
                        LOGGER.error(e);
                    }
                    newGroups.setGroup(group);
                    group.add(appendedEntries);
                }

                // groupsSelector is always created, even when no groups
                // have been defined. therefore, no check for null is
                // required here
                frame.getGroupSelector().addGroups(newGroups, ce);
            }
        }

        if (importSelectorWords) {
            for (String s : meta) {
                if (s.startsWith(MetaData.SELECTOR_META_PREFIX)) {
                    panel.getBibDatabaseContext().getMetaData().putData(s, meta.getData(s));
                }
            }
        }

        ce.end();
        panel.undoManager.addEdit(ce);
        panel.markBaseChanged();
    }
}

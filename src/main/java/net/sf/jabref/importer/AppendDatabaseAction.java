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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.swing.JOptionPane;

import net.sf.jabref.*;
import net.sf.jabref.groups.structure.GroupHierarchyType;
import net.sf.jabref.gui.*;
import net.sf.jabref.groups.structure.AllEntriesGroup;
import net.sf.jabref.groups.structure.ExplicitGroup;
import net.sf.jabref.groups.GroupTreeNode;
import net.sf.jabref.gui.actions.BaseAction;
import net.sf.jabref.model.database.KeyCollisionException;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.undo.UndoableInsertEntry;
import net.sf.jabref.gui.undo.UndoableInsertString;
import net.sf.jabref.model.entry.IdGenerator;
import net.sf.jabref.gui.util.PositionWindow;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibtexDatabase;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.model.entry.BibtexString;
import net.sf.jabref.util.Util;

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


    public AppendDatabaseAction(JabRefFrame frame, BasePanel panel) {
        this.frame = frame;
        this.panel = panel;
    }

    @Override
    public void action() {

        filesToOpen.clear();
        final MergeDialog md = new MergeDialog(frame, Localization.lang("Append database"), true);
        PositionWindow.placeDialog(md, panel);
        md.setVisible(true);
        if (md.isOkPressed()) {
            String[] chosen = FileDialogs.getMultipleFiles(frame, new File(Globals.prefs.get(JabRefPreferences.WORKING_DIRECTORY)),
                    null, false);
            //String chosenFile = Globals.getNewFile(frame, new File(Globals.prefs.get("workingDirectory")),
            //                                       null, JFileChooser.OPEN_DIALOG, false);
            if (chosen == null) {
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
            } catch (Throwable ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog
                (panel, ex.getMessage(),
 Localization.lang("Open database"),
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static void mergeFromBibtex(JabRefFrame frame, BasePanel panel, ParserResult pr,
            boolean importEntries, boolean importStrings,
            boolean importGroups, boolean importSelectorWords)
                    throws KeyCollisionException {

        BibtexDatabase fromDatabase = pr.getDatabase();
        ArrayList<BibtexEntry> appendedEntries = new ArrayList<>();
        ArrayList<BibtexEntry> originalEntries = new ArrayList<>();
        BibtexDatabase database = panel.database();
        BibtexEntry originalEntry;
        NamedCompound ce = new NamedCompound(Localization.lang("Append database"));
        MetaData meta = pr.getMetaData();

        if (importEntries) { // Add entries
            boolean overwriteOwner = Globals.prefs.getBoolean(JabRefPreferences.OVERWRITE_OWNER);
            boolean overwriteTimeStamp = Globals.prefs.getBoolean(JabRefPreferences.OVERWRITE_TIME_STAMP);

            for (String key : fromDatabase.getKeySet()) {
                originalEntry = fromDatabase.getEntryById(key);
                BibtexEntry be = (BibtexEntry) originalEntry.clone();
                be.setId(IdGenerator.next());
                Util.setAutomaticFields(be, overwriteOwner, overwriteTimeStamp);
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
                    ExplicitGroup group = new ExplicitGroup("Imported", GroupHierarchyType.INDEPENDENT);
                    newGroups.setGroup(group);
                    for (BibtexEntry appendedEntry : appendedEntries) {
                        group.addEntry(appendedEntry);
                    }
                }

                // groupsSelector is always created, even when no groups
                // have been defined. therefore, no check for null is
                // required here
                frame.groupSelector.addGroups(newGroups, ce);
                // for explicit groups, the entries copied to the mother fromDatabase have to
                // be "reassigned", i.e. the old reference is removed and the reference
                // to the new fromDatabase is added.
                GroupTreeNode node;
                ExplicitGroup group;
                BibtexEntry entry;

                for (Enumeration<GroupTreeNode> e = newGroups
                        .preorderEnumeration(); e.hasMoreElements(); ) {
                    node = e.nextElement();
                    if (!(node.getGroup() instanceof ExplicitGroup)) {
                        continue;
                    }
                    group = (ExplicitGroup) node.getGroup();
                    for (int i = 0; i < originalEntries.size(); ++i) {
                        entry = originalEntries.get(i);
                        if (group.contains(entry)) {
                            group.removeEntry(entry);
                            group.addEntry(appendedEntries.get(i));
                        }
                    }
                }
                frame.groupSelector.revalidateGroups();
            }
        }

        if (importSelectorWords) {
            for (String s : meta) {
                if (s.startsWith(Globals.SELECTOR_META_PREFIX)) {
                    panel.metaData().putData(s, meta.getData(s));
                }
            }
        }

        ce.end();
        panel.undoManager.addEdit(ce);
        panel.markBaseChanged();
    }

}

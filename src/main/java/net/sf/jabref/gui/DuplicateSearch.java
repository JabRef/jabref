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
// created by : ?
//
// modified : r.nagel 2.09.2004
//            - new SearcherRunnable.setFinish() method
//            - replace thread.sleep in run() by wait() and notify() mechanism

package net.sf.jabref.gui;

import java.util.ArrayList;
import java.util.Vector;

import javax.swing.SwingUtilities;

import net.sf.jabref.*;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.undo.UndoableInsertEntry;
import net.sf.jabref.gui.undo.UndoableRemoveEntry;
import net.sf.jabref.gui.worker.CallBack;
import net.sf.jabref.bibtex.DuplicateCheck;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibtexEntry;
import spin.Spin;

public class DuplicateSearch implements Runnable {

    private final BasePanel panel;
    private BibtexEntry[] bes;
    private final Vector<BibtexEntry[]> duplicates = new Vector<BibtexEntry[]>();


    public DuplicateSearch(BasePanel bp) {
        panel = bp;
    }

    @Override
    public void run() {
        final NamedCompound ce = new NamedCompound(Localization.lang("duplicate removal"));
        int duplicateCounter = 0;

        boolean autoRemoveExactDuplicates = false;
        panel.output(Localization.lang("Searching for duplicates..."));
        Object[] keys = panel.database.getKeySet().toArray();
        if (keys.length < 2) {
            return;
        }
        bes = new BibtexEntry[keys.length];
        for (int i = 0; i < keys.length; i++) {
            bes[i] = panel.database.getEntryById((String) keys[i]);
        }

        SearcherRunnable st = new SearcherRunnable();
        JabRefExecutorService.INSTANCE.executeWithLowPriorityInOwnThread(st, "Searcher");
        int current = 0;

        final ArrayList<BibtexEntry> toRemove = new ArrayList<BibtexEntry>();
        final ArrayList<BibtexEntry> toAdd = new ArrayList<BibtexEntry>();
        while (!st.finished() || current < duplicates.size())
        {

            if (current >= duplicates.size())
            {
                // wait until the search thread puts something into duplicates vector
                // or finish its work
                synchronized (duplicates)
                {
                    try
                    {
                        duplicates.wait();
                    } catch (Exception ignored) {
                    }
                }
            } else // duplicates found
            {

                BibtexEntry[] be = duplicates.get(current);
                current++;
                if (!toRemove.contains(be[0]) && !toRemove.contains(be[1])) {
                    // Check if they are exact duplicates:
                    boolean askAboutExact = false;
                    if (DuplicateCheck.compareEntriesStrictly(be[0], be[1]) > 1) {
                        if (autoRemoveExactDuplicates) {
                            toRemove.add(be[1]);
                            duplicateCounter++;
                            continue;
                        } else {
                            askAboutExact = true;
                        }
                    }

                    DuplicateCallBack cb = new DuplicateCallBack(panel.frame, be[0], be[1],
                            askAboutExact ? DuplicateResolverDialog.DUPLICATE_SEARCH_WITH_EXACT :
                                    DuplicateResolverDialog.DUPLICATE_SEARCH);
                    ((CallBack) Spin.over(cb)).update();

                    duplicateCounter++;
                    int answer = cb.getSelected();
                    if (answer == DuplicateResolverDialog.KEEP_UPPER
                            || answer == DuplicateResolverDialog.AUTOREMOVE_EXACT) {
                        toRemove.add(be[1]);
                        if (answer == DuplicateResolverDialog.AUTOREMOVE_EXACT)
                         {
                            autoRemoveExactDuplicates = true; // Remember choice
                        }
                    } else if (answer == DuplicateResolverDialog.KEEP_LOWER) {
                        toRemove.add(be[0]);
                    } else if (answer == DuplicateResolverDialog.BREAK) {
                        st.setFinished(); // thread killing
                        current = Integer.MAX_VALUE;
                        duplicateCounter--; // correct counter
                    } else if (answer == DuplicateResolverDialog.KEEP_MERGE) {
                        toRemove.add(be[0]);
                        toRemove.add(be[0]);
                        toAdd.add(cb.getMergedEntry());
                    }
                }
            }
        }

        final int dupliC = duplicateCounter;
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                // Now, do the actual removal:
                if (!toRemove.isEmpty()) {
                    for (BibtexEntry entry : toRemove) {
                        panel.database.removeEntry(entry.getId());
                        ce.addEdit(new UndoableRemoveEntry(panel.database, entry, panel));
                    }
                    panel.markBaseChanged();
                }
                // and adding merged entries:
                if (!toAdd.isEmpty()) {
                    for (BibtexEntry entry : toAdd) {
                        panel.database.insertEntry(entry);
                        ce.addEdit(new UndoableInsertEntry(panel.database, entry, panel));
                    }
                    panel.markBaseChanged();
                }

                panel.output(Localization.lang("Duplicate pairs found") + ": " + duplicates.size()
                        + ' ' + Localization.lang("pairs processed") + ": " + dupliC);

                ce.end();
                panel.undoManager.addEdit(ce);

            }

        });

    }


    class SearcherRunnable implements Runnable {

        private volatile boolean finished;

        @Override
        public void run() {
            for (int i = 0; i < bes.length - 1 && !finished; i++) {
                for (int j = i + 1; j < bes.length && !finished; j++) {
                    boolean eq = DuplicateCheck.isDuplicate(bes[i], bes[j]);

                    // If (suspected) duplicates, add them to the duplicates vector.
                    if (eq) {
                        synchronized (duplicates) {
                            duplicates.add(new BibtexEntry[] {bes[i], bes[j]});
                            duplicates.notifyAll(); // send wake up all
                        }
                    }
                }
            }
            finished = true;
            // if no duplicates found, the graphical thread will never wake up
            synchronized (duplicates) {
                duplicates.notifyAll();
            }
        }

        public boolean finished() {
            return finished;
        }

        // Thread cancel option
        // no synchronized used because no "realy" critical situations expected
        public void setFinished()
        {
            finished = true;
        }
    }

    static class DuplicateCallBack implements CallBack {

        private int reply = -1;
        DuplicateResolverDialog diag;
        private final JabRefFrame frame;
        private final BibtexEntry one;
        private final BibtexEntry two;
        private final int dialogType;
        private BibtexEntry merged;


        public DuplicateCallBack(JabRefFrame frame, BibtexEntry one, BibtexEntry two,
                int dialogType) {

            this.frame = frame;
            this.one = one;
            this.two = two;
            this.dialogType = dialogType;
        }

        public int getSelected() {
            return reply;
        }

        public BibtexEntry getMergedEntry() {
            return merged;
        }

        @Override
        public void update() {
            diag = new DuplicateResolverDialog(frame, one, two, dialogType);
            diag.setVisible(true);
            diag.dispose();
            reply = diag.getSelected();
            merged = diag.getMergedEntry();
        }
    }

}

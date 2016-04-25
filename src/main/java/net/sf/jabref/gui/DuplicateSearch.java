/*  Copyright (C) 2003-2015 JabRef contributors.
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
import java.util.List;

import javax.swing.SwingUtilities;

import net.sf.jabref.JabRefExecutorService;
import net.sf.jabref.JabRefGUI;
import net.sf.jabref.gui.DuplicateResolverDialog.DuplicateResolverResult;
import net.sf.jabref.gui.DuplicateResolverDialog.DuplicateResolverType;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.undo.UndoableInsertEntry;
import net.sf.jabref.gui.undo.UndoableRemoveEntry;
import net.sf.jabref.gui.worker.CallBack;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.DuplicateCheck;
import net.sf.jabref.model.entry.BibEntry;

import spin.Spin;

public class DuplicateSearch implements Runnable {

    private final BasePanel panel;
    private List<BibEntry> bes;
    private final List<BibEntry[]> duplicates = new ArrayList<>();


    public DuplicateSearch(BasePanel bp) {
        panel = bp;
    }

    @Override
    public void run() {

        panel.output(Localization.lang("Searching for duplicates..."));

        bes = panel.getDatabase().getEntries();
        if (bes.size() < 2) {
            return;
        }

        SearcherRunnable st = new SearcherRunnable();
        JabRefExecutorService.INSTANCE.executeWithLowPriorityInOwnThread(st, "Searcher");
        int current = 0;

        final List<BibEntry> toRemove = new ArrayList<>();
        final List<BibEntry> toAdd = new ArrayList<>();

        int duplicateCounter = 0;
        boolean autoRemoveExactDuplicates = false;

        synchronized (duplicates) {
            while (!st.finished() || (current < duplicates.size())) {

                if (current >= duplicates.size()) {
                    // wait until the search thread puts something into duplicates vector
                    // or finish its work

                    try {
                        duplicates.wait();
                    } catch (InterruptedException ignored) {
                        // Ignore
                    }

                } else { // duplicates found
                    BibEntry[] be = duplicates.get(current);
                    current++;
                    if (!toRemove.contains(be[0]) && !toRemove.contains(be[1])) {
                        // Check if they are exact duplicates:
                        boolean askAboutExact = false;
                        if (DuplicateCheck.compareEntriesStrictly(be[0], be[1]) > 1) {
                            if (autoRemoveExactDuplicates) {
                                toRemove.add(be[1]);
                                duplicateCounter++;
                                continue;
                            }
                            askAboutExact = true;
                        }

                        DuplicateCallBack cb = new DuplicateCallBack(JabRefGUI.getMainFrame(), be[0], be[1],
                                askAboutExact ? DuplicateResolverType.DUPLICATE_SEARCH_WITH_EXACT : DuplicateResolverType.DUPLICATE_SEARCH);
                        ((CallBack) Spin.over(cb)).update();

                        duplicateCounter++;
                        DuplicateResolverResult answer = cb.getSelected();
                        if ((answer == DuplicateResolverResult.KEEP_UPPER)
                                || (answer == DuplicateResolverResult.AUTOREMOVE_EXACT)) {
                            toRemove.add(be[1]);
                            if (answer == DuplicateResolverResult.AUTOREMOVE_EXACT) {
                                autoRemoveExactDuplicates = true; // Remember choice
                            }
                        } else if (answer == DuplicateResolverResult.KEEP_LOWER) {
                            toRemove.add(be[0]);
                        } else if (answer == DuplicateResolverResult.BREAK) {
                            st.setFinished(); // thread killing
                            current = Integer.MAX_VALUE;
                            duplicateCounter--; // correct counter
                        } else if (answer == DuplicateResolverResult.KEEP_MERGE) {
                            toRemove.add(be[0]);
                            toRemove.add(be[1]);
                            toAdd.add(cb.getMergedEntry());
                        }
                    }
                }
            }
        }

        final NamedCompound ce = new NamedCompound(Localization.lang("duplicate removal"));

        final int dupliC = duplicateCounter;
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                // Now, do the actual removal:
                if (!toRemove.isEmpty()) {
                    for (BibEntry entry : toRemove) {
                        panel.getDatabase().removeEntry(entry);
                        ce.addEdit(new UndoableRemoveEntry(panel.getDatabase(), entry, panel));
                    }
                    panel.markBaseChanged();
                }
                // and adding merged entries:
                if (!toAdd.isEmpty()) {
                    for (BibEntry entry : toAdd) {
                        panel.getDatabase().insertEntry(entry);
                        ce.addEdit(new UndoableInsertEntry(panel.getDatabase(), entry, panel));
                    }
                    panel.markBaseChanged();
                }

                synchronized (duplicates) {
                    panel.output(Localization.lang("Duplicates found") + ": " + duplicates.size() + ' '
                            + Localization.lang("pairs processed") + ": " + dupliC);
                }
                ce.end();
                panel.undoManager.addEdit(ce);

            }

        });

    }


    class SearcherRunnable implements Runnable {

        private volatile boolean finished;

        @Override
        public void run() {
            for (int i = 0; (i < (bes.size() - 1)) && !finished; i++) {
                for (int j = i + 1; (j < bes.size()) && !finished; j++) {
                    BibEntry first = bes.get(i);
                    BibEntry second = bes.get(j);
                    boolean eq = DuplicateCheck.isDuplicate(first, second, panel.getBibDatabaseContext().getMode());

                    // If (suspected) duplicates, add them to the duplicates vector.
                    if (eq) {
                        synchronized (duplicates) {
                            duplicates.add(new BibEntry[]{first, second});
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
        // no synchronized used because no "really" critical situations expected
        public void setFinished() {
            finished = true;
        }
    }

    static class DuplicateCallBack implements CallBack {

        private DuplicateResolverResult reply = DuplicateResolverResult.NOT_CHOSEN;
        private final JabRefFrame frame;
        private final BibEntry one;
        private final BibEntry two;
        private final DuplicateResolverType dialogType;
        private BibEntry merged;


        public DuplicateCallBack(JabRefFrame frame, BibEntry one, BibEntry two, DuplicateResolverType dialogType) {

            this.frame = frame;
            this.one = one;
            this.two = two;
            this.dialogType = dialogType;
        }

        public DuplicateResolverResult getSelected() {
            return reply;
        }

        public BibEntry getMergedEntry() {
            return merged;
        }

        @Override
        public void update() {
            DuplicateResolverDialog diag = new DuplicateResolverDialog(frame, one, two, dialogType);
            diag.setVisible(true);
            diag.dispose();
            reply = diag.getSelected();
            merged = diag.getMergedEntry();
        }
    }

}

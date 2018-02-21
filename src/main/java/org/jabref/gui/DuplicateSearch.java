package org.jabref.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.SwingUtilities;

import org.jabref.JabRefExecutorService;
import org.jabref.JabRefGUI;
import org.jabref.gui.DuplicateResolverDialog.DuplicateResolverResult;
import org.jabref.gui.DuplicateResolverDialog.DuplicateResolverType;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableInsertEntry;
import org.jabref.gui.undo.UndoableRemoveEntry;
import org.jabref.gui.worker.CallBack;
import org.jabref.logic.bibtex.DuplicateCheck;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;

import spin.Spin;

public class DuplicateSearch implements Runnable {

    private final BasePanel panel;
    private List<BibEntry> bes;
    private final List<List<BibEntry>> duplicates = new ArrayList<>();


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
        JabRefExecutorService.INSTANCE.executeInterruptableTask(st, "DuplicateSearcher");
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
                    List<BibEntry> be = duplicates.get(current);
                    current++;
                    if (!toRemove.contains(be.get(0)) && !toRemove.contains(be.get(1))) {
                        // Check if they are exact duplicates:
                        boolean askAboutExact = false;
                        if (DuplicateCheck.compareEntriesStrictly(be.get(0), be.get(1)) > 1) {
                            if (autoRemoveExactDuplicates) {
                                toRemove.add(be.get(1));
                                duplicateCounter++;
                                continue;
                            }
                            askAboutExact = true;
                        }

                        DuplicateCallBack cb = new DuplicateCallBack(JabRefGUI.getMainFrame(), be.get(0), be.get(1),
                                askAboutExact ? DuplicateResolverType.DUPLICATE_SEARCH_WITH_EXACT : DuplicateResolverType.DUPLICATE_SEARCH);
                        ((CallBack) Spin.over(cb)).update();

                        duplicateCounter++;
                        DuplicateResolverResult answer = cb.getSelected();
                        if ((answer == DuplicateResolverResult.KEEP_LEFT)
                                || (answer == DuplicateResolverResult.AUTOREMOVE_EXACT)) {
                            toRemove.add(be.get(1));
                            if (answer == DuplicateResolverResult.AUTOREMOVE_EXACT) {
                                autoRemoveExactDuplicates = true; // Remember choice
                            }
                        } else if (answer == DuplicateResolverResult.KEEP_RIGHT) {
                            toRemove.add(be.get(0));
                        } else if (answer == DuplicateResolverResult.BREAK) {
                            st.setFinished(); // thread killing
                            current = Integer.MAX_VALUE;
                            duplicateCounter--; // correct counter
                        } else if (answer == DuplicateResolverResult.KEEP_MERGE) {
                            toRemove.addAll(be);
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
                panel.getUndoManager().addEdit(ce);

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
                            duplicates.add(Arrays.asList(first, second));
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

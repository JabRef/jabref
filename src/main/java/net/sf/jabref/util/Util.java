/*  Copyright (C) 2003-2016 JabRef contributors.
    Copyright (C) 2015 Oliver Kopp

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

// created by : Morten O. Alver 2003

package net.sf.jabref.util;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JOptionPane;

import net.sf.jabref.bibtex.InternalBibtexFields;
import net.sf.jabref.gui.worker.AbstractWorker;
import net.sf.jabref.gui.worker.CallBack;
import net.sf.jabref.gui.worker.Worker;
import net.sf.jabref.logic.groups.AbstractGroup;
import net.sf.jabref.logic.groups.KeywordGroup;
import net.sf.jabref.logic.l10n.Localization;

/**
 * utility functions
 */
public class Util {

    /**
     * Run an AbstractWorker's methods using Spin features to put each method on the correct thread.
     *
     * @param worker The worker to run.
     * @throws Throwable
     */
    public static void runAbstractWorker(AbstractWorker worker) throws Throwable {
        // This part uses Spin's features:
        Worker wrk = worker.getWorker();
        // The Worker returned by getWorker() has been wrapped
        // by Spin.off(), which makes its methods be run in
        // a different thread from the EDT.
        CallBack clb = worker.getCallBack();

        worker.init(); // This method runs in this same thread, the EDT.
        // Useful for initial GUI actions, like printing a message.

        // The CallBack returned by getCallBack() has been wrapped
        // by Spin.over(), which makes its methods be run on
        // the EDT.
        wrk.run(); // Runs the potentially time-consuming action
        // without freezing the GUI. The magic is that THIS line
        // of execution will not continue until run() is finished.
        clb.update(); // Runs the update() method on the EDT.
    }


    public static boolean warnAssignmentSideEffects(AbstractGroup group, Component parent) {
        return warnAssignmentSideEffects(Collections.singletonList(group), parent);
    }

    /**
     * Warns the user of undesired side effects of an explicit assignment/removal of entries to/from this group.
     * Currently there are four types of groups: AllEntriesGroup, SearchGroup - do not support explicit assignment.
     * ExplicitGroup - never modifies entries. KeywordGroup - only this modifies entries upon assignment/removal.
     * Modifications are acceptable unless they affect a standard field (such as "author") besides the "keywords" field.
     *
     * @param parent The Component used as a parent when displaying a confirmation dialog.
     * @return true if the assignment has no undesired side effects, or the user chose to perform it anyway. false
     * otherwise (this indicates that the user has aborted the assignment).
     */
    public static boolean warnAssignmentSideEffects(List<AbstractGroup> groups, Component parent) {
        List<String> affectedFields = new ArrayList<>();
        for (AbstractGroup group : groups) {
            if (group instanceof KeywordGroup) {
                KeywordGroup kg = (KeywordGroup) group;
                String field = kg.getSearchField().toLowerCase();
                if ("keywords".equals(field)) {
                    continue; // this is not undesired
                }
                int len = InternalBibtexFields.numberOfPublicFields();
                for (int i = 0; i < len; ++i) {
                    if (field.equals(InternalBibtexFields.getFieldName(i))) {
                        affectedFields.add(field);
                        break;
                    }
                }
            }
        }
        if (affectedFields.isEmpty()) {
            return true; // no side effects
        }

        // show a warning, then return
        StringBuilder message = new StringBuilder(
                Localization.lang("This action will modify the following field(s) in at least one entry each:"))
                        .append('\n');
        for (String affectedField : affectedFields) {
            message.append(affectedField).append('\n');
        }
        message.append(Localization.lang("This could cause undesired changes to your entries.")).append('\n')
                .append("It is recommended that you change the grouping field in your group definition to \"keywords\" or a non-standard name.")
                .append("\n\n").append(Localization.lang("Do you still want to continue?"));
        int choice = JOptionPane.showConfirmDialog(parent, message, Localization.lang("Warning"),
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return choice != JOptionPane.NO_OPTION;

        // if (groups instanceof KeywordGroup) {
        // KeywordGroup kg = (KeywordGroup) groups;
        // String field = kg.getSearchField().toLowerCase();
        // if (field.equals("keywords"))
        // return true; // this is not undesired
        // for (int i = 0; i < GUIGlobals.ALL_FIELDS.length; ++i) {
        // if (field.equals(GUIGlobals.ALL_FIELDS[i])) {
        // // show a warning, then return
        // String message = Globals ...
        // .lang(
        // "This action will modify the \"%0\" field "
        // + "of your entries.\nThis could cause undesired changes to "
        // + "your entries, so it is\nrecommended that you change the grouping
        // field "
        // + "in your group\ndefinition to \"keywords\" or a non-standard name."
        // + "\n\nDo you still want to continue?",
        // field);
        // int choice = JOptionPane.showConfirmDialog(parent, message,
        // Globals.lang("Warning"), JOptionPane.YES_NO_OPTION,
        // JOptionPane.WARNING_MESSAGE);
        // return choice != JOptionPane.NO_OPTION;
        // }
        // }
        // }
        // return true; // found no side effects
    }

}

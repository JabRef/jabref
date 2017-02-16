package org.jabref.gui.actions.bibsonomy;

import java.awt.event.ActionEvent;

import org.jabref.bibsonomy.BibSonomyProperties;
import org.jabref.gui.bibsonomy.SearchType;
import org.jabref.gui.worker.bibsonomy.ImportPostsByCriteriaWorker;

import org.bibsonomy.common.enums.GroupingEntity;

import org.jabref.bibsonomy.BibSonomyProperties;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.bibsonomy.SearchType;
import org.jabref.gui.worker.bibsonomy.ImportPostsByCriteriaWorker;


/**
 * Runs the {@link ImportPostsByCriteriaWorker} to import all posts of the user.
 */
public class ImportAllMyPostsAction extends AbstractBibSonomyAction {

    public ImportAllMyPostsAction(JabRefFrame jabRefFrame) {
        super(jabRefFrame);
    }

    public void actionPerformed(ActionEvent e) {
        ImportPostsByCriteriaWorker worker = new ImportPostsByCriteriaWorker(getJabRefFrame(), "", SearchType.FULL_TEXT, GroupingEntity.USER, BibSonomyProperties.getUsername(), true);
        performAsynchronously(worker);
    }

}

package org.bibsonomy.plugin.jabref.action;

import java.awt.event.ActionEvent;

import net.sf.jabref.gui.JabRefFrame;

import org.bibsonomy.common.enums.GroupingEntity;
import org.bibsonomy.plugin.jabref.BibSonomyProperties;
import org.bibsonomy.plugin.jabref.gui.SearchType;
import org.bibsonomy.plugin.jabref.worker.ImportPostsByCriteriaWorker;


/**
 * {@link ImportAllMyPostsAction} runs the {@link ImportPostsByCriteriaWorker} to import all posts of the user.
 *
 * @author Waldemar Biller <biller@cs.uni-kassel.de>
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

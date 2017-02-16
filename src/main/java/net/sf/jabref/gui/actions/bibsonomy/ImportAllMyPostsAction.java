package net.sf.jabref.gui.actions.bibsonomy;

import java.awt.event.ActionEvent;

import net.sf.jabref.bibsonomy.BibSonomyProperties;
import net.sf.jabref.gui.bibsonomy.SearchType;
import net.sf.jabref.gui.worker.bibsonomy.ImportPostsByCriteriaWorker;

import org.bibsonomy.common.enums.GroupingEntity;
import org.jabref.gui.JabRefFrame;


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

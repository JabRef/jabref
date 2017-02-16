package net.sf.jabref.gui.worker.bibsonomy;

import java.util.Collections;
import java.util.Optional;

import net.sf.jabref.bibsonomy.BibSonomyProperties;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.util.bibsonomy.LogicInterfaceFactory;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bibsonomy.model.logic.LogicInterface;

/**
 * Delete a Post from the service
 */
public class DeletePostsWorker extends AbstractBibSonomyWorker {

	private static final Log LOGGER = LogFactory.getLog(DeletePostsWorker.class);

	private BibEntry[] entries;

	public void run() {
		for (BibEntry entry : entries) {
			final Optional<String> intrahashOpt = entry.getField(FieldName.INTRAHASH);
			final Optional<String> usernameOpt = entry.getField(FieldName.USERNAME);

			if ((intrahashOpt.isPresent()) || intrahashOpt.get().isEmpty() || (usernameOpt.isPresent() && !intrahashOpt.isPresent() && !(BibSonomyProperties.getUsername().equals(usernameOpt.get())))) {
				try {
                    LogicInterface logic = LogicInterfaceFactory.getLogic(jabRefFrame.getCurrentBasePanel().getDatabaseContext());
                    logic.deletePosts(BibSonomyProperties.getUsername(), Collections.singletonList(intrahashOpt.get()));
					jabRefFrame.output(Localization.lang("Deleting post %0", intrahashOpt.get()));
					entry.clearField(FieldName.INTRAHASH);
				} catch (Exception ex) {
					LOGGER.error(Localization.lang("Failed deleting post %0", intrahashOpt.get()));
				}
			}
		}
		jabRefFrame.output(Localization.lang("Done"));
	}

	public DeletePostsWorker(JabRefFrame jabRefFrame, BibEntry[] entries) {
		super(jabRefFrame);
		this.entries = entries;
	}
}

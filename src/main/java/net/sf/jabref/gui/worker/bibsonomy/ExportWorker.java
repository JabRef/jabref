package net.sf.jabref.gui.worker.bibsonomy;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import net.sf.jabref.bibsonomy.BibSonomyProperties;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.actions.bibsonomy.ShowSettingsDialogAction;
import net.sf.jabref.gui.util.bibsonomy.WorkerUtil;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.bibsonomy.JabRefModelConverter;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bibsonomy.common.enums.PostUpdateOperation;
import org.bibsonomy.model.BibTex;
import org.bibsonomy.model.Post;
import org.bibsonomy.model.Resource;
import org.bibsonomy.model.User;
import org.bibsonomy.rest.exceptions.AuthenticationException;

/**
 * Export an entry to service
 */
public class ExportWorker extends AbstractBibSonomyWorker {

	private static final Log LOGGER = LogFactory.getLog(ExportWorker.class);

	private List<BibEntry> entries;

	public void run() {
		try {
			for (BibEntry entry : entries) {
				Optional<String> citeKeyOpt = entry.getCiteKeyOptional();
				if(citeKeyOpt.isPresent()) {
					jabRefFrame.output("Exporting post " + citeKeyOpt.get());
				}else {
					jabRefFrame.output("Exporting post");
				}

				// add private or public if groups is empty
				Optional<String> groupsOpt = entry.getField(FieldName.GROUPS);
				if (groupsOpt.isPresent() || groupsOpt.get().isEmpty()) {
					entry.setField(FieldName.GROUPS, BibSonomyProperties.getDefaultVisibilty());
				}

				entry.setField(FieldName.USERNAME, BibSonomyProperties.getUsername());
				Optional<String> ownerOpt = entry.getField(FieldName.OWNER);
				entry.clearField(FieldName.OWNER);

				Post<BibTex> post = JabRefModelConverter.convertEntry(entry);
				if (post.getUser() == null) {
					post.setUser(new User(BibSonomyProperties.getUsername()));
				}

				Optional<String> intrahashOpt = entry.getField(FieldName.INTRAHASH);
				if (intrahashOpt.isPresent() && !intrahashOpt.get().isEmpty()) {
					changePost(post);
				} else {
					createPost(post);
				}
				entry.setField(FieldName.INTRAHASH, post.getResource().getIntraHash());
				ownerOpt.ifPresent(owner -> entry.setField(FieldName.OWNER, owner));

				Optional<String> filesOpt = entry.getField(FieldName.FILE);
				if (filesOpt.isPresent() && !filesOpt.get().isEmpty() && intrahashOpt.isPresent()) {
					WorkerUtil.performAsynchronously(new UploadDocumentsWorker(jabRefFrame, intrahashOpt.get(), filesOpt.get()));
				}
			}
			jabRefFrame.output("Done.");
			return;
		} catch (AuthenticationException ex) {
			(new ShowSettingsDialogAction(jabRefFrame)).actionPerformed(null);
		} catch (Throwable ex) {
			LOGGER.error("Failed to export post ", ex);
		}
		jabRefFrame.output(Localization.lang("Failed"));
	}

	private void changePost(Post<? extends Resource> post) throws Exception {
		final List<String> hashes = getLogic().updatePosts(Collections.singletonList(post), PostUpdateOperation.UPDATE_ALL);
		if (hashes.size() != 1) {
			throw new IllegalStateException("changePosts returned " + hashes.size() + " hashes");
		}
		post.getResource().setIntraHash(hashes.get(0));
	}

	private void createPost(Post<? extends Resource> post) throws Exception {
		final List<String> hashes = getLogic().createPosts(Collections.singletonList(post));
		if (hashes.size() != 1) {
			throw new IllegalStateException("createPosts returned " + hashes.size() + " hashes");
		}
		post.getResource().setIntraHash(hashes.get(0));
	}

	public ExportWorker(JabRefFrame jabRefFrame, List<BibEntry> entries) {
		super(jabRefFrame);
		this.entries = entries;
	}
}

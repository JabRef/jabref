package org.jabref.gui.worker.bibsonomy;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.swing.JOptionPane;

import org.jabref.bibsonomy.BibSonomyProperties;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.bibsonomy.SearchType;
import org.jabref.gui.importer.ImportInspectionDialog;
import org.jabref.gui.util.bibsonomy.BibSonomyCallBack;
import org.jabref.gui.util.bibsonomy.LogicInterfaceFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.bibsonomy.JabRefModelConverter;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bibsonomy.common.enums.GroupingEntity;
import org.bibsonomy.model.BibTex;
import org.bibsonomy.model.Post;
import org.bibsonomy.model.Resource;
import org.bibsonomy.model.logic.LogicInterface;
import org.bibsonomy.rest.exceptions.AuthenticationException;

/**
 * Import a posts from the service
 */
public class ImportPostsByCriteriaWorker extends AbstractBibSonomyWorker {

	private static final Log LOGGER = LogFactory.getLog(ImportPostsByCriteriaWorker.class);

	private SearchType type;

	private String criteria;

	private GroupingEntity grouping;

	private String groupingValue;

	private ImportInspectionDialog dialog;

	private boolean ignoreRequestSize;

	public ImportPostsByCriteriaWorker(JabRefFrame jabRefFrame, String criteria, SearchType type, GroupingEntity grouping, String groupingValue, boolean ignoreRequestSize) {
		super(jabRefFrame);
		this.criteria = criteria;
		this.type = type;
		this.grouping = grouping;
		this.groupingValue = groupingValue;

		this.dialog = new ImportInspectionDialog(jabRefFrame, jabRefFrame.getCurrentBasePanel(), Localization.lang("Import from BibSonomy"), false);

		this.ignoreRequestSize = ignoreRequestSize;

		dialog.setLocationRelativeTo(jabRefFrame);
		dialog.addCallBack(new BibSonomyCallBack(this));
	}

	public void run() {

		dialog.setVisible(true);
		dialog.setProgress(0, 0);

		int numberOfPosts = 0, start = 0, end = BibSonomyProperties.getNumberOfPostsPerRequest(), cycle = 0, numberOfPostsPerRequest = BibSonomyProperties.getNumberOfPostsPerRequest();
		boolean continueFetching = false;
		do {
			List<String> tags = null;
			String search = null;
			switch (type) {
				case TAGS:
					if (criteria.contains(" ")) {
						tags = Arrays.asList(criteria.split("\\s"));
					} else {
						tags = Collections.singletonList(criteria);
					}
					break;
				case FULL_TEXT:
					search = criteria;
					break;
			}

			try {
                LogicInterface logic = LogicInterfaceFactory.getLogic(jabRefFrame.getCurrentBasePanel().getDatabaseContext());
				final Collection<Post<BibTex>> result = logic.getPosts(BibTex.class, grouping, groupingValue, tags, null, search, null, null, null, null, null, start, end);
				for (Post<? extends Resource> post : result) {
					dialog.setProgress(numberOfPosts++, numberOfPostsPerRequest);
					Optional<BibEntry> entry = JabRefModelConverter.convertPostOptional(post);

					if(entry.isPresent()){
						BibEntry bibEntry = entry.get();
						// clear fields if the fetched posts does not belong to the user
						Optional<String> optUserName = bibEntry.getField(FieldName.USERNAME);
						if (optUserName.isPresent()) {
							if (!BibSonomyProperties.getUsername().equals(optUserName.get())) {
								bibEntry.clearField(FieldName.INTRAHASH);
								bibEntry.clearField(FieldName.FILE);
								bibEntry.clearField(FieldName.OWNER);
							}
						}
						dialog.addEntry(bibEntry);
					}
				}

				if (!continueFetching) {
					if (!ignoreRequestSize) {

						if (!BibSonomyProperties.getIgnoreMorePostsWarning()) {

							if (numberOfPosts == numberOfPostsPerRequest) {
								int status = JOptionPane.showOptionDialog(dialog, "<html>There are probably more than " + BibSonomyProperties.getNumberOfPostsPerRequest()
												+ " posts available. Continue importing?<br>You can stop importing entries by hitting the Stop button on the import dialog.", "More posts available",
										JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, null, JOptionPane.YES_OPTION);

								switch (status) {
									case JOptionPane.YES_OPTION:
										continueFetching = true;
										break;

									case JOptionPane.NO_OPTION:
										this.stopFetching();
										break;
									default:
										break;
								}
							}
						}
					}
				}
				start = ((cycle + 1) * numberOfPostsPerRequest);
				end = ((cycle + 2) * numberOfPostsPerRequest);

				cycle++;
			} catch (AuthenticationException ex) {
				LOGGER.error(ex.getLocalizedMessage(), ex);
				// TODO: we could open the settings dialog here. This should be done via the eventbus somehow
			}
		} while (fetchNext() && numberOfPosts >= numberOfPostsPerRequest);

		dialog.entryListComplete();
	}
}

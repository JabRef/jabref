package org.bibsonomy.plugin.jabref.worker;

import java.net.URLEncoder;
import java.util.Optional;

import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

import ca.odell.glazedlists.BasicEventList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bibsonomy.model.BibTex;
import org.bibsonomy.model.Document;
import org.bibsonomy.model.Post;
import org.bibsonomy.model.Resource;
import org.bibsonomy.plugin.jabref.BibSonomyProperties;
import org.bibsonomy.plugin.jabref.action.ShowSettingsDialogAction;
import org.bibsonomy.rest.exceptions.AuthenticationException;
import org.bibsonomy.util.file.FileUtil;

/**
 * Download all private documents of a post from the service
 *
 * @author Waldemar Biller <biller@cs.uni-kassel.de>
 */
public class DownloadDocumentsWorker extends AbstractBibSonomyWorker {

	private static final Log LOGGER = LogFactory.getLog(DownloadDocumentsWorker.class);

	private static final String BIBTEX_FILE_FIELD = "file";

	private BibEntry entry;
	private boolean isImport;

	public DownloadDocumentsWorker(JabRefFrame jabRefFrame, BibEntry entry, boolean isImport) {
		super(jabRefFrame);
		this.entry = entry;
		this.isImport = isImport;
	}

	public void run() {

		if (isImport && !BibSonomyProperties.getDownloadDocumentsOnImport()) {
			return;
		}

		Optional<String> intrahashOpt = entry.getField(FieldName.INTRAHASH);
		if (intrahashOpt.isPresent() && !intrahashOpt.get().isEmpty()) {
			final Post<? extends Resource> post;
			try {
				post = getLogic().getPostDetails(intrahashOpt.get(), BibSonomyProperties.getUsername()); // client.executeQuery(getPostDetailsQuery);
			} catch (AuthenticationException e) {
				(new ShowSettingsDialogAction(jabRefFrame)).actionPerformed(null);
				return;
			} catch (Exception e) {
				LOGGER.error("Failed getting details for post " + intrahashOpt.get(), e);
				jabRefFrame.output(Localization.lang("Failed getting details for post."));
				return;
			}
			Resource r = post.getResource();
			if (!(r instanceof BibTex)) {
				LOGGER.warn("requested resource with intrahash '" + intrahashOpt.get() + "' is not bibtex");
				jabRefFrame.output(Localization.lang("Error: Invalid Resourcetype."));
				return;
			}
			for (Document document : ((BibTex) r).getDocuments()) {
				jabRefFrame.output(Localization.lang("Downloading: %0", document.getFileName()));
				try {
					getLogic().getDocument(BibSonomyProperties.getUsername(), intrahashOpt.get(), URLEncoder.encode(document.getFileName(), "UTF-8"));
				} catch (Exception ex) {
					LOGGER.error("Failed downloading file: " + document.getFileName(), ex);
				}

				Optional<String> citeKeyOpt = entry.getCiteKeyOptional();
				try {
					BasicEventList<BibEntry> list = new BasicEventList<>();
					String downloadedFileBibTex = ":" + document.getFileName() + ":" + FileUtil.getFileExtension(document.getFileName()).toUpperCase();

					Optional<String> entryFileValueOpt = entry.getField(BIBTEX_FILE_FIELD);

					list.getReadWriteLock().writeLock().lock();
					list.add(entry);
					if (entryFileValueOpt.isPresent() && !entryFileValueOpt.get().isEmpty()) {

						if (!entryFileValueOpt.get().contains(downloadedFileBibTex))
							entry.setField(BIBTEX_FILE_FIELD, entryFileValueOpt.get() + ";" + downloadedFileBibTex);
					} else entry.setField(BIBTEX_FILE_FIELD, downloadedFileBibTex);
					list.getReadWriteLock().writeLock().lock();

				} catch (AuthenticationException e) {
					(new ShowSettingsDialogAction(jabRefFrame)).actionPerformed(null);
				} catch (Exception e) {
					if (citeKeyOpt.isPresent()) {
						LOGGER.error("Failed adding file to entry " + citeKeyOpt.get(), e);
					} else {
						LOGGER.error("Failed adding file to entry", e);
					}
				}
			}
		}
		jabRefFrame.output(Localization.lang("Done"));
	}
}

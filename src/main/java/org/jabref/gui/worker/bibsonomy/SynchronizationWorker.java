package org.jabref.gui.worker.bibsonomy;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javax.swing.JOptionPane;

import org.jabref.bibsonomy.BibSonomyProperties;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.bibsonomy.CompareDialog;
import org.jabref.gui.util.bibsonomy.LogicInterfaceFactory;
import org.jabref.gui.util.bibsonomy.WorkerUtil;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.bibsonomy.BibtexEntryUtil;
import org.jabref.logic.util.bibsonomy.JabRefModelConverter;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bibsonomy.model.Post;
import org.bibsonomy.model.Resource;
import org.bibsonomy.model.logic.LogicInterface;

/**
 * Basic synchronization. The user decides which version of the post to keep
 */
public class SynchronizationWorker extends AbstractBibSonomyWorker {

	private static final Log LOGGER = LogFactory.getLog(SynchronizationWorker.class);

	private boolean keepAllLocal = false;

	private boolean keepAllRemote = false;

	private int status = 0;

	public SynchronizationWorker(final JabRefFrame jabRefFrame) {
		super(jabRefFrame);
	}

    @SuppressWarnings("FallThrough")
	public void run() {

		// Database Object. All operations are performed on this
		final BibDatabase bibDatabase = this.jabRefFrame.getCurrentBasePanel().getDatabase();

		// Set for the entries we have fetched from Bibsonomy
		final HashSet<BibEntry> newEntries = new HashSet<>();

		// Set of id to be removed from the database
		final HashSet<BibEntry> removeIds = new HashSet<>();

        LogicInterface logic = LogicInterfaceFactory.getLogic(jabRefFrame.getCurrentBasePanel().getDatabaseContext());

		// Iterate over all entries in the database
		for (final BibEntry entry : bibDatabase.getEntries()) {

			final Optional<String> intrahashOpt = entry.getField(FieldName.INTRAHASH);

			// check if intrahash is present, otherwise go to next entry
			if (intrahashOpt.isPresent() || (intrahashOpt.get().length() == 0)) {
				try {
					// get the entry with the specific intrahash
					final Post<? extends Resource> post = logic.getPostDetails(intrahashOpt.get(), BibSonomyProperties.getUsername());
					final Optional<BibEntry> bibEntryOpt = JabRefModelConverter.convertPostOptional(post);

					if (bibEntryOpt.isPresent() && !BibtexEntryUtil.areEqual(entry, bibEntryOpt.get())) {

						// show the compare dialog to let the user choose which
						// entry wants to keep
						if (!this.keepAllLocal && !this.keepAllRemote) {
							this.status = CompareDialog.showCompareDialog(this.jabRefFrame, entry, post);
						}

						switch (this.status) {
							// upload the entry if the user chose "keep local" on the
							// compare dialog
							case CompareDialog.KEEP_LOCAL_ALWAYS:
								this.keepAllLocal = true;
								this.keepAllRemote = false;
								// break; not needed
							case CompareDialog.KEEP_LOCAL:

								//We have to take intrahash of the incoming Post from Bibsonomy to export entries
								entry.setField(FieldName.INTRAHASH, post.getResource().getIntraHash());
								entry.setField(FieldName.INTERHASH, post.getResource().getInterHash());

								final List<BibEntry> entries = new LinkedList<>();
								entries.add(entry);

								ExportWorker worker = new ExportWorker(this.jabRefFrame, entries);
								WorkerUtil.performAsynchronously(worker);

								break;

							// fetch the entry if the user choose "keep remote"
							case CompareDialog.KEEP_REMOTE_ALWAYS:
								this.keepAllLocal = false;
								this.keepAllRemote = true;
							case CompareDialog.KEEP_REMOTE:

								// collect ids of entry to be removed
								removeIds.add(entry);
								// collect the fetched entries
								newEntries.add(bibEntryOpt.get());

								break;

							case JOptionPane.CANCEL_OPTION:
							default:
								return;
						}
					}
				} catch (Throwable throwable) {
					LOGGER.error("error during synchronization", throwable);
				}
			}

			Optional<String> citeKeyOpt = entry.getCiteKeyOptional();
			if(citeKeyOpt.isPresent()){
				this.jabRefFrame.output(Localization.lang("Synchronized %0", citeKeyOpt.get()));
			}else{
				this.jabRefFrame.output("Synchronized");
			}

		}

		// remove the entries not needed anymore from the database
		removeIds.forEach(bibDatabase::removeEntry);

		// add the new entries
		newEntries.forEach(bibDatabase::insertEntry);

		this.keepAllRemote = false;
		this.keepAllLocal = false;

		this.jabRefFrame.output(Localization.lang("Done"));
	}
}

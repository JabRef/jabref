/**
 *  
 *  JabRef Bibsonomy Plug-in - Plugin for the reference management 
 * 		software JabRef (http://jabref.sourceforge.net/) 
 * 		to fetch, store and delete entries from BibSonomy.
 *   
 *  Copyright (C) 2008 - 2011 Knowledge & Data Engineering Group, 
 *                            University of Kassel, Germany
 *                            http://www.kde.cs.uni-kassel.de/
 *  
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.bibsonomy.plugin.jabref.worker;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JOptionPane;

import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.JabRefFrame;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bibsonomy.model.Post;
import org.bibsonomy.model.Resource;
import org.bibsonomy.model.logic.LogicInterface;
import org.bibsonomy.plugin.jabref.PluginProperties;
import org.bibsonomy.plugin.jabref.gui.CompareDialog;
import org.bibsonomy.plugin.jabref.util.BibtexEntryUtil;
import org.bibsonomy.plugin.jabref.util.JabRefModelConverter;
import org.bibsonomy.plugin.jabref.util.WorkerUtil;

/**
 * Basic synchronization. The user decides which version of the post to keep
 * 
 * @author Waldemar Biller <wbi@cs.uni-kassel.de>
 * 
 */
public class SynchronizationWorker extends AbstractPluginWorker {

	private static final Log LOG = LogFactory.getLog(SynchronizationWorker.class);

	private boolean keepAllLocal = false;

	private boolean keepAllRemote = false;

	private int status = 0;

	public SynchronizationWorker(final JabRefFrame jabRefFrame) {
		super(jabRefFrame);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.jabref.Worker#run()
	 */
	public void run() {

		// Database Object. All operations are performed on this
		final BibtexDatabase db = this.jabRefFrame.basePanel().database();

		// Set for the entries we have fetched from Bibsonomy
		final HashSet<BibtexEntry> newEntries = new HashSet<BibtexEntry>();

		// Set of id to be removed from the database
		final HashSet<String> removeIds = new HashSet<String>();

		LogicInterface logic = getLogic();
		// Iterate over all entries in the database
		for (final BibtexEntry entry : db.getEntries()) {

			final String intrahash = entry.getField("intrahash");

			// check if intrahash is present, otherwise go to next entry
			if ((intrahash == null) || (intrahash.length() == 0)) {
				// TODO: new entries shall not be added to bibsonomy? 
				continue;
			}

			try {
				// get the entry with the specific intrahash
				final Post<? extends Resource> post = logic.getPostDetails(intrahash, PluginProperties.getUsername());

				if (!BibtexEntryUtil.areEqual(entry, JabRefModelConverter.convertPost(post))) {
					
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
					case CompareDialog.KEEP_LOCAL:
						
						//We have to take intrahash of the incoming Post from Bibsonomy to export entries
						entry.setField("intrahash", post.getResource().getIntraHash());
						//FIXME - also Interhash?
						entry.setField("interhash", post.getResource().getInterHash());
						
						final List<BibtexEntry> entries = new LinkedList<BibtexEntry>();
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
						removeIds.add(entry.getId());
						// collect the fetched entries
						newEntries.add(JabRefModelConverter.convertPost(post));

						break;

					case JOptionPane.CANCEL_OPTION:
					default:
						return;
					}
				}
			} catch (final Exception e) {
				LOG.error("error during synchronization", e);
			} catch (Throwable e) {
				LOG.error("error during synchronization", e);
			}

			this.jabRefFrame.output("Synchronized " + entry.getCiteKey());

		}

		// remove the entries not needed anymore from the database
		for (final String id : removeIds) {
			db.removeEntry(id);
		}

		// add the new entries
		for (final BibtexEntry e : newEntries) {
			db.insertEntry(e);
		}

		this.keepAllRemote = false;
		this.keepAllLocal = false;

		this.jabRefFrame.output("Done.");
	}
}

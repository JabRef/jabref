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

import java.util.Collections;
import java.util.List;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.JabRefFrame;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bibsonomy.common.enums.PostUpdateOperation;
import org.bibsonomy.model.BibTex;
import org.bibsonomy.model.Post;
import org.bibsonomy.model.Resource;
import org.bibsonomy.model.User;
import org.bibsonomy.plugin.jabref.PluginProperties;
import org.bibsonomy.plugin.jabref.action.ShowSettingsDialogAction;
import org.bibsonomy.plugin.jabref.util.JabRefModelConverter;
import org.bibsonomy.plugin.jabref.util.WorkerUtil;
import org.bibsonomy.rest.exceptions.AuthenticationException;

/**
 * Export an entry to service
 * 
 * @author Waldemar Biller <biller@cs.uni-kassel.de>
 * 
 */
public class ExportWorker extends AbstractPluginWorker {

	private static final Log LOG = LogFactory.getLog(ExportWorker.class);

	private List<BibtexEntry> entries;

	public void run() {
		try {
			for (BibtexEntry entry : entries) {
				String intrahash = entry.getField("intrahash");
				jabRefFrame.output("Exporting post " + entry.getCiteKey());

				// add private or public if groups is empty
				if (entry.getField("groups") == null || "".equals(entry.getField("groups"))) {
					entry.setField("groups", PluginProperties.getDefaultVisibilty());
				}

				entry.setField("username", PluginProperties.getUsername());
				String owner = entry.getField("owner");
				entry.clearField("owner");

				Post<BibTex> post = JabRefModelConverter.convertEntry(entry);
				if (post.getUser() == null) {
					post.setUser(new User(PluginProperties.getUsername()));
				}

				if (intrahash != null && !"".equals(intrahash)) {
					changePost(post);
				} else {
					createPost(post);
				}
				entry.setField("intrahash", post.getResource().getIntraHash());
				entry.setField("owner", owner);

				String files = entry.getField("file");
				if (files != null && !"".equals(files)) {
					WorkerUtil.performAsynchronously(new UploadDocumentsWorker(jabRefFrame, entry.getField("intrahash"), files));
				}
			}
			jabRefFrame.output("Done.");
			return;
		} catch (AuthenticationException ex) {
			(new ShowSettingsDialogAction(jabRefFrame)).actionPerformed(null);
		} catch (Exception ex) {
			LOG.error("Failed to export post ", ex);
		} catch (Throwable ex) {
			LOG.error("Failed to export post ", ex);
		}
		jabRefFrame.output("Failed.");
	}

	private void changePost(Post<? extends Resource> post) throws Exception {
		final List<String> hashes = getLogic().updatePosts(Collections.<Post<? extends Resource>> singletonList(post), PostUpdateOperation.UPDATE_ALL);
		if (hashes.size() != 1) {
			throw new IllegalStateException("changePosts returned " + hashes.size() + " hashes");
		}
		post.getResource().setIntraHash(hashes.get(0));
	}

	private void createPost(Post<? extends Resource> post) throws Exception {
		final List<String> hashes = getLogic().createPosts(Collections.<Post<? extends Resource>> singletonList(post));
		if (hashes.size() != 1) {
			throw new IllegalStateException("createPosts returned " + hashes.size() + " hashes");
		}
		post.getResource().setIntraHash(hashes.get(0));
	}

	public ExportWorker(JabRefFrame jabRefFrame, List<BibtexEntry> entries) {
		super(jabRefFrame);
		this.entries = entries;
	}
}

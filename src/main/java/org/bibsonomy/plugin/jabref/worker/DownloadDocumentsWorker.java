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

import java.net.URLEncoder;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.JabRefFrame;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bibsonomy.model.BibTex;
import org.bibsonomy.model.Document;
import org.bibsonomy.model.Post;
import org.bibsonomy.model.Resource;
import org.bibsonomy.plugin.jabref.PluginProperties;
import org.bibsonomy.plugin.jabref.action.ShowSettingsDialogAction;
import org.bibsonomy.rest.exceptions.AuthenticationException;
import org.bibsonomy.util.file.FileUtil;

import ca.odell.glazedlists.BasicEventList;

/**
 * Download all private documents of a post from the service
 * @author Waldemar Biller <biller@cs.uni-kassel.de>
 *
 */
public class DownloadDocumentsWorker extends AbstractPluginWorker {

	private static final Log LOG = LogFactory.getLog(DownloadDocumentsWorker.class);
	
	private static final String BIBTEX_FILE_FIELD = "file";
	
	
	private BibtexEntry entry;
	private boolean isImport;
	
	public DownloadDocumentsWorker(JabRefFrame jabRefFrame, BibtexEntry entry, boolean isImport) {
		super(jabRefFrame);
		this.entry = entry;
		this.isImport = isImport;
	}

	public void run() {
		
		if (isImport && !PluginProperties.getDownloadDocumentsOnImport()) {
			return;
		}

		
		String intrahash = entry.getField("intrahash");
		if (intrahash != null && !"".equals(intrahash)) {
			final Post<? extends Resource> post;
			try {
				post = getLogic().getPostDetails(intrahash, PluginProperties.getUsername()); // client.executeQuery(getPostDetailsQuery);
			} catch (AuthenticationException e) {
				(new ShowSettingsDialogAction(jabRefFrame)).actionPerformed(null);
				return;
			} catch (Exception e) {
				LOG.error("Failed getting details for post " + intrahash, e);
				jabRefFrame.output("Failed getting details for post.");
				return;
			}
			Resource r = post.getResource();
			if (!(r instanceof BibTex)) {
				LOG.warn("requested resource with intrahash '" + intrahash + "' is not bibtex");
				jabRefFrame.output("Error: Invalid Resourcetype.");
				return;
			}
			for (Document document : ((BibTex) r).getDocuments()) {
				jabRefFrame.output("Downloading: " + document.getFileName());
				try {
					getLogic().getDocument(PluginProperties.getUsername(), intrahash, URLEncoder.encode(document.getFileName(), "UTF-8"));
				} catch (Exception ex) {
					LOG.error("Failed downloading file: " + document.getFileName(), ex);
				}
				
				try {
					BasicEventList<BibtexEntry> list = new BasicEventList<BibtexEntry>();
					String downloadedFileBibTex = ":" + document.getFileName() + ":" + FileUtil.getFileExtension(document.getFileName()).toUpperCase();
					
					String entryFileValue = entry.getField(BIBTEX_FILE_FIELD);
					
					list.getReadWriteLock().writeLock().lock();
					list.add(entry);
					if(entryFileValue != null && !"".equals(entryFileValue)) {
						
						if(!entryFileValue.contains(downloadedFileBibTex))
							entry.setField(BIBTEX_FILE_FIELD, entryFileValue + ";" + downloadedFileBibTex);
					} else entry.setField(BIBTEX_FILE_FIELD, downloadedFileBibTex);
					list.getReadWriteLock().writeLock().lock();
					
				} catch (AuthenticationException e) {
					(new ShowSettingsDialogAction(jabRefFrame)).actionPerformed(null);
				} catch (Exception e) {
					LOG.error("Failed adding file to entry " + entry.getCiteKey(), e);
				}
				
			}
		}
		
		jabRefFrame.output("Done.");
	}
}

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

import java.util.Arrays;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.JabRefFrame;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bibsonomy.plugin.jabref.PluginProperties;

/**
 * Delete a Post from the service
 * @author Waldemar Biller <biller@cs.uni-kassel.de>
 *
 */
public class DeletePostsWorker extends AbstractPluginWorker {

	private static final Log LOG = LogFactory.getLog(DeletePostsWorker.class);
	
	private BibtexEntry[] entries;

	public void run() {
		for (BibtexEntry entry : entries) {
			final String intrahash = entry.getField("intrahash");
			final String username = entry.getField("username");
			if ((intrahash == null) || ("".equals(intrahash)) || ((intrahash != null) && !(PluginProperties.getUsername().equals(username)))) {
				continue;
			}
			
			try {
				getLogic().deletePosts(PluginProperties.getUsername(), Arrays.asList(intrahash));
				jabRefFrame.output("Deleting post " + intrahash);
				entry.clearField("intrahash");
			} catch (Exception ex) {
				LOG.error("Failed deleting post " + intrahash);
			}
		}
		jabRefFrame.output("Done.");
	}

	public DeletePostsWorker(JabRefFrame jabRefFrame, BibtexEntry[] entries) {
		super(jabRefFrame);
		this.entries = entries;
	}
}

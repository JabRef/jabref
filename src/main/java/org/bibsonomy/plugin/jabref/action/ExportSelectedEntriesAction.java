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

package org.bibsonomy.plugin.jabref.action;

import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.JabRefFrame;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bibsonomy.plugin.jabref.util.CheckTagsUtil;
import org.bibsonomy.plugin.jabref.worker.ExportWorker;

/**
 * {@link ExportSelectedEntriesAction} checks for entries without keywords and does 
 * an export of all selected entries to the service.
 * @author Waldemar Biller <biller@cs.uni-kassel.de>
 *
 */
public class ExportSelectedEntriesAction extends AbstractPluginAction {

	private static final Log LOG = LogFactory
			.getLog(ExportSelectedEntriesAction.class);

	private static final long serialVersionUID = -3680150888244016437L;

	public void actionPerformed(ActionEvent e) {
		/*
		 * fetch selected entries
		 */
		List<BibtexEntry> entries = Arrays.asList(getJabRefFrame().basePanel()
				.getSelectedEntries());
		/*
		 * check if they all have keywords
		 */
		CheckTagsUtil ctu = new CheckTagsUtil(entries, getJabRefFrame());
		switch (ctu.hasAPostNoTagsAssigned()) {
		case JOptionPane.YES_OPTION:
			// tags missing & user has explicitly chosen to add default tag
			ctu.assignDefaultTag();
		case JOptionPane.DEFAULT_OPTION:
			// this means that all posts have tags
			ExportWorker worker = new ExportWorker(getJabRefFrame(), entries);
			performAsynchronously(worker);
			break;
		default:
			// happens when tags are missing, and user wants to cancel export
			LOG.debug("Selected post have no tags assigned");
		}

	}

	public ExportSelectedEntriesAction(JabRefFrame jabRefFrame) {

		super(jabRefFrame, "Export selected entries", new ImageIcon(
				ExportSelectedEntriesAction.class
						.getResource("/images/document--arrow.png")));
	}
}
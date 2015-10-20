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
import java.util.Collection;
import java.util.List;

import javax.swing.JOptionPane;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexFields;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.gui.ImportInspectionDialog;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bibsonomy.common.enums.GroupingEntity;
import org.bibsonomy.model.BibTex;
import org.bibsonomy.model.Post;
import org.bibsonomy.model.Resource;
import org.bibsonomy.plugin.jabref.PluginProperties;
import org.bibsonomy.plugin.jabref.action.ShowSettingsDialogAction;
import org.bibsonomy.plugin.jabref.gui.SearchType;
import org.bibsonomy.plugin.jabref.util.JabRefModelConverter;
import org.bibsonomy.plugin.jabref.util.PluginCallBack;
import org.bibsonomy.rest.exceptions.AuthenticationException;

/**
 * Import a posts from the service
 * 
 * @author Waldemar Biller <biller@cs.uni-kassel.de>
 * 
 */
public class ImportPostsByCriteriaWorker extends AbstractPluginWorker {

	private static final Log LOG = LogFactory.getLog(ImportPostsByCriteriaWorker.class);

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
		this.dialog = new ImportInspectionDialog(jabRefFrame, jabRefFrame.basePanel(), BibtexFields.DEFAULT_INSPECTION_FIELDS, "Import from BibSonomy", false);

		this.ignoreRequestSize = ignoreRequestSize;

		dialog.setLocationRelativeTo(jabRefFrame);
		dialog.addCallBack(new PluginCallBack(this));
	}

	public void run() {

		dialog.setVisible(true);
		dialog.setProgress(0, 0);

		int numberOfPosts = 0, start = 0, end = PluginProperties.getNumberOfPostsPerRequest(), cycle = 0, numberOfPostsPerRequest = PluginProperties.getNumberOfPostsPerRequest();
		boolean continueFetching = false;
		do {
			numberOfPosts = 0;

			List<String> tags = null;
			String search = null;
			switch (type) {
			case TAGS:
				if (criteria.contains(" ")) {
					tags = Arrays.asList(criteria.split("\\s"));
				} else {
					tags = Arrays.asList(new String[] { criteria });
				}
				break;
			case FULL_TEXT:
				search = criteria;
				break;
			}

			try {

				final Collection<Post<BibTex>> result = getLogic().getPosts(BibTex.class, grouping, groupingValue, tags, null, search, null, null, null, null, start, end);
				for (Post<? extends Resource> post : result) {
					dialog.setProgress(numberOfPosts++, numberOfPostsPerRequest);
					BibtexEntry entry = JabRefModelConverter.convertPost(post);

					// clear fields if the fetched posts does not belong to the
					// user
					if (!PluginProperties.getUsername().equals(entry.getField("username"))) {
						entry.clearField("intrahash");
						entry.clearField("interhash");
						entry.clearField("file");
						entry.clearField("owner");
					}
					dialog.addEntry(entry);
				}

				if (!continueFetching) {
					if (!ignoreRequestSize) {

						if (!PluginProperties.getIgnoreMorePostsWarning()) {

							if (numberOfPosts == numberOfPostsPerRequest) {
								int status = JOptionPane.showOptionDialog(dialog, "<html>There are probably more than " + PluginProperties.getNumberOfPostsPerRequest()
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
				(new ShowSettingsDialogAction((JabRefFrame) dialog.getOwner())).actionPerformed(null);
			} catch (Exception ex) {
				LOG.error("Failed to import posts", ex);
			}
		} while (fetchNext() && numberOfPosts >= numberOfPostsPerRequest);

		dialog.entryListComplete();
	}
}

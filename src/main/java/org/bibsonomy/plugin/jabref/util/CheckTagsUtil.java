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

package org.bibsonomy.plugin.jabref.util;

import static org.bibsonomy.util.ValidationUtils.present;

import java.util.List;

import javax.swing.JOptionPane;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.JabRefFrame;

import org.bibsonomy.plugin.jabref.PluginProperties;

/**
 * {@link CheckTagsUtil} check a list of posts, if a posts has no tags assigned
 * @author Waldemar Biller <biller@cs.uni-kassel.de>
 *
 */
public class CheckTagsUtil {

	/*
	 * constants
	 */
	private static final String DEFAULT_TAG = "jabref:noKeywordAssigned";
	private static final int MAX_NUM_MISSING_TAGS = 10;
	private static final String KEYWORD_FIELD = "keywords";
	private static final String TITLE_FIELD = "title";

	/** the entries to be checked */
	private List<BibtexEntry> entries;
	/** jabref frame */
	private JabRefFrame jabRefFrame;

	/**
	 * Contstructor
	 * 
	 * @param entries
	 *            - the entries to be checked
	 * @param jabRefFrame
	 *            - jabref frame
	 */
	public CheckTagsUtil(List<BibtexEntry> entries, JabRefFrame jabRefFrame) {

		this.entries = entries;
		this.jabRefFrame = jabRefFrame;
	}

	/**
	 * loop through list of posts and check if tags are present for each
	 * 
	 * @return
	 */
	public int hasAPostNoTagsAssigned() {
		int numPostsMissingTags = 0;
		String postsMissingTags = "";
		for (BibtexEntry entry : entries) {
			if (!present(entry.getField(KEYWORD_FIELD))) {
				/*
				 * if the user has chosen to not to be warned when exporting
				 * entries without keywords, we add the default tag silently.
				 */
				if (PluginProperties.ignoreNoTagsAssigned()) {
					assignDefaultTag(entry);
				} else {
					numPostsMissingTags++;
					postsMissingTags += entry.getField(TITLE_FIELD) + "\n";
				}
			}
		}
		/*
		 * if posts without tags are present, ask the user what to do (can
		 * happen only when user setting "ignoreNoTagsAssigned" is FALSE, see
		 * comment above)
		 */
		if (numPostsMissingTags > 0 && jabRefFrame != null) {
			String message;
			if (numPostsMissingTags <= MAX_NUM_MISSING_TAGS) {
				message = "The following selected entries have no keywords assigned: \n\n"
						+ postsMissingTags
						+ "\nDo you want to continue exporting them? If you choose yes, "
						+ "then the keyword '"
						+ DEFAULT_TAG
						+ "' will be added as default tag.";
			} else {
				message = "More than "
						+ MAX_NUM_MISSING_TAGS
						+ " selected entries have no keywords assigned; "
						+ "Do you want to continue exporting them? If you choose yes, "
						+ "then the keyword '" + DEFAULT_TAG
						+ "' will be added as default tag.";
			}
			return JOptionPane.showConfirmDialog(jabRefFrame, message,
					"Post is missing tags", JOptionPane.YES_NO_OPTION);
		}
		/*
		 * all posts have assigned tags -> return OK
		 */
		return JOptionPane.DEFAULT_OPTION;
	}

	/**
	 * add default tag to each post which has no tags assigned.
	 */
	public void assignDefaultTag() {
		for (BibtexEntry entry : entries) {
			if (!present(entry.getField(KEYWORD_FIELD)))
				entry.setField(KEYWORD_FIELD, DEFAULT_TAG);
		}
	}

	/**
	 * assign default tag to a post.
	 * 
	 * @param post
	 *            - the post to assign the default tag to.
	 */
	public static void assignDefaultTag(BibtexEntry entry) {
		entry.setField(KEYWORD_FIELD, DEFAULT_TAG);
	}
}

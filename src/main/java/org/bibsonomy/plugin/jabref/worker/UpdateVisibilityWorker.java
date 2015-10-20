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

import java.util.List;

import javax.swing.JComboBox;

import net.sf.jabref.JabRefFrame;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bibsonomy.common.enums.GroupingEntity;
import org.bibsonomy.model.Group;
import org.bibsonomy.model.User;
import org.bibsonomy.plugin.jabref.PluginProperties;
import org.bibsonomy.plugin.jabref.action.ShowSettingsDialogAction;
import org.bibsonomy.plugin.jabref.gui.GroupingComboBoxItem;
import org.bibsonomy.rest.exceptions.AuthenticationException;

/**
 * Fetch the users groups and add them to the "import posts from..." field
 * 
 * @author Waldemar Biller <biller@cs.uni-kassel.de>
 * 
 */
public class UpdateVisibilityWorker extends AbstractPluginWorker {

	private static final Log LOG = LogFactory.getLog(UpdateVisibilityWorker.class);

	private JComboBox<? super GroupingComboBoxItem> visibility;
	private List<GroupingComboBoxItem> defaultGroupings;

	public UpdateVisibilityWorker(JabRefFrame jabRefFrame, JComboBox<? super GroupingComboBoxItem> visibility, List<GroupingComboBoxItem> defaultGroupings) {
		super(jabRefFrame);
		this.visibility = visibility;
		this.defaultGroupings = defaultGroupings;
	}

	public void run() {
		GroupingComboBoxItem item = (GroupingComboBoxItem) visibility.getSelectedItem();
		
		visibility.removeAllItems();
		if (defaultGroupings != null) {
			for (GroupingComboBoxItem defaultGrouping : defaultGroupings) {
				visibility.addItem(defaultGrouping);
			}
		}

		try {
			User user = getLogic().getUserDetails(PluginProperties.getUsername());

			for (Group g : user.getGroups()) {
				visibility.addItem(new GroupingComboBoxItem(GroupingEntity.GROUP, g.getName()));
			}

			if (item != null) {
				int count = visibility.getItemCount();
				for (int i = 0; i < count; i++) {
					GroupingComboBoxItem currentItem = (GroupingComboBoxItem) visibility.getItemAt(i);
					if (currentItem.getValue().equals(item.getValue())) {
						visibility.setSelectedIndex(i);
					}
				}

			}

		} catch (AuthenticationException ex) {
			(new ShowSettingsDialogAction(jabRefFrame)).actionPerformed(null);
		} catch (Exception ex) {
			LOG.error("Failed to get user details for user: " + PluginProperties.getUsername(), ex);
		}
	}

}

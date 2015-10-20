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
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;

import net.sf.jabref.JabRefFrame;

import org.bibsonomy.common.enums.GroupingEntity;
import org.bibsonomy.plugin.jabref.PluginProperties;
import org.bibsonomy.plugin.jabref.gui.GroupingComboBoxItem;
import org.bibsonomy.plugin.jabref.worker.RefreshTagListWorker;
import org.bibsonomy.plugin.jabref.worker.UpdateVisibilityWorker;

/**
 * {@link RefreshTagListAction} runs the {@link RefreshTagListWorker} to refresh the tag cloud
 * @author Waldemar Biller <biller@cs.uni-kassel.de>
 *
 */
public class RefreshTagListAction extends AbstractPluginAction {

	private static final long serialVersionUID = 3285344367883492911L;
	
	private static List<GroupingComboBoxItem> defaultGroupings;
	
	private JEditorPane tagCloud;

	private JComboBox<? super GroupingComboBoxItem> groupingComboBox;

	public void actionPerformed(ActionEvent e) {
	
		// refresh the tag cloud
		RefreshTagListWorker worker = new RefreshTagListWorker(getJabRefFrame(), tagCloud, ((GroupingComboBoxItem) groupingComboBox.getSelectedItem()).getKey(), ((GroupingComboBoxItem) groupingComboBox.getSelectedItem()).getValue());
		performAsynchronously(worker);
		
		// update the "import posts from.." combo box
		UpdateVisibilityWorker visWorker = new UpdateVisibilityWorker(getJabRefFrame(), groupingComboBox, getDefaultGroupings());
		performAsynchronously(visWorker);
	}

	public RefreshTagListAction(JabRefFrame jabRefFrame, JEditorPane tagCloud, JComboBox<? super GroupingComboBoxItem> groupingComboBox) {
		
		super(jabRefFrame, "Refresh", new ImageIcon(RefreshTagListAction.class.getResource("/images/arrow-circle-225.png")));
		this.tagCloud = tagCloud;

		this.groupingComboBox = groupingComboBox;
	}
	
	private static List<GroupingComboBoxItem> getDefaultGroupings() {
		if (defaultGroupings == null) {
			defaultGroupings = new ArrayList<GroupingComboBoxItem>();
			defaultGroupings.add(new GroupingComboBoxItem(GroupingEntity.ALL, "all users"));
			defaultGroupings.add(new GroupingComboBoxItem(GroupingEntity.USER, PluginProperties.getUsername()));
		}
		return defaultGroupings;
	}
}

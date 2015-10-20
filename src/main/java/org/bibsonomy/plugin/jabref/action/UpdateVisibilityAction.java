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
import java.util.List;

import javax.swing.JComboBox;

import net.sf.jabref.JabRefFrame;

import org.bibsonomy.plugin.jabref.gui.GroupingComboBoxItem;
import org.bibsonomy.plugin.jabref.worker.UpdateVisibilityWorker;

/**
 * {@link UpdateVisibilityAction} runs the {@link UpdateVisibilityWorker}
 * @author Waldemar Biller <biller@cs.uni-kassel.de>
 *
 */
public class UpdateVisibilityAction extends AbstractPluginAction {

	private static final long serialVersionUID = 2487196975142597818L;
	
	private JComboBox<GroupingComboBoxItem> visibility;
	private List<GroupingComboBoxItem> defaultGroupings;

	public UpdateVisibilityAction(JabRefFrame jabRefFrame, JComboBox<GroupingComboBoxItem> visibility,List<GroupingComboBoxItem> defaultGroupings) {
		super(jabRefFrame, null, null);
		
		this.visibility = visibility;
		this.defaultGroupings = defaultGroupings;
	}

	public void actionPerformed(ActionEvent e) {
		
		UpdateVisibilityWorker worker = new UpdateVisibilityWorker(getJabRefFrame(), visibility, defaultGroupings);
		performAsynchronously(worker);
	}

}

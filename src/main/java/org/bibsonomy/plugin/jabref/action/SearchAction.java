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

import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JTextField;

import net.sf.jabref.JabRefFrame;

import org.bibsonomy.plugin.jabref.gui.GroupingComboBoxItem;
import org.bibsonomy.plugin.jabref.gui.SearchType;
import org.bibsonomy.plugin.jabref.gui.SearchTypeComboBoxItem;
import org.bibsonomy.plugin.jabref.worker.ImportPostsByCriteriaWorker;

/**
 * {@link SearchAction} runs the {@link ImportPostsByCriteriaWorker} with the values of the search text box
 * @author Waldemar Biller <biller@cs.uni-kassel.de>
 *
 */
public class SearchAction extends AbstractPluginAction {

	private static final long serialVersionUID = -2051315699879554553L;

	private JTextField searchTextField;

	private JComboBox<?> searchTypeComboBox;

	private JComboBox<?> groupingComboBox;

	public void actionPerformed(ActionEvent e) {
		
		SearchType st = ((SearchTypeComboBoxItem) searchTypeComboBox.getSelectedItem()).getKey();
		String criteria = searchTextField.getText();
		
		if(criteria != null) {
			ImportPostsByCriteriaWorker worker = new ImportPostsByCriteriaWorker(getJabRefFrame(), criteria, st, ((GroupingComboBoxItem) groupingComboBox.getSelectedItem()).getKey(), ((GroupingComboBoxItem) groupingComboBox.getSelectedItem()).getValue(), false);
			performAsynchronously(worker);
		}
	}

	public SearchAction(JabRefFrame jabRefFrame, JTextField searchTextField, JComboBox<?> searchTypeComboBox, JComboBox<?> groupingComboBox) {
		
		super(jabRefFrame, "", new ImageIcon(SearchAction.class.getResource("/images/magnifier.png")));
		
		this.searchTextField = searchTextField;
		this.searchTypeComboBox = searchTypeComboBox;
		this.groupingComboBox = groupingComboBox;
	}
}

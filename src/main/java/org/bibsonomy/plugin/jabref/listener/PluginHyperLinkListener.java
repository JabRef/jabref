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

package org.bibsonomy.plugin.jabref.listener;

import java.awt.event.ActionEvent;
import java.util.StringTokenizer;

import javax.swing.JComboBox;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.event.HyperlinkListener;

import net.sf.jabref.JabRefFrame;

import org.bibsonomy.plugin.jabref.action.AbstractPluginAction;
import org.bibsonomy.plugin.jabref.gui.GroupingComboBoxItem;
import org.bibsonomy.plugin.jabref.gui.SearchType;
import org.bibsonomy.plugin.jabref.worker.ImportPostsByCriteriaWorker;

/**
 * {@link PluginHyperLinkListener} runs the {@link ImportPostsByCriteriaWorker} as soon as 
 * the user clicks on a hyperlink in the tag cloud
 * @author Waldemar Biller <biller@cs.uni-kassel.de>
 *
 */
public class PluginHyperLinkListener  extends AbstractPluginAction implements HyperlinkListener {

	private static final long serialVersionUID = -2030390936610286041L;

	private JComboBox<?> visibilityComboBox;

	public void hyperlinkUpdate(HyperlinkEvent e) {
		
		if(e.getEventType() == EventType.ACTIVATED) {
			StringTokenizer tokenizer = new StringTokenizer(e.getDescription(), " ");
			
			if(tokenizer.hasMoreElements()) {
				
				String criteria = tokenizer.nextToken();
				ImportPostsByCriteriaWorker worker = new ImportPostsByCriteriaWorker(getJabRefFrame(), criteria, SearchType.TAGS, ((GroupingComboBoxItem) visibilityComboBox.getSelectedItem()).getKey(), ((GroupingComboBoxItem) visibilityComboBox.getSelectedItem()).getValue(), false);
				performAsynchronously(worker);
			}
			
		}
	}
	
	public PluginHyperLinkListener(JabRefFrame jabRefFrame, JComboBox<?> visibilityComboBox) {
		
		super(jabRefFrame, null, null);
		this.visibilityComboBox = visibilityComboBox;
	}

	public void actionPerformed(ActionEvent e) {}

}

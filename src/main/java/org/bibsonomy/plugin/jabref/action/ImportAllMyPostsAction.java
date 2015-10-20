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

import org.bibsonomy.common.enums.GroupingEntity;
import org.bibsonomy.plugin.jabref.PluginProperties;
import org.bibsonomy.plugin.jabref.gui.SearchType;
import org.bibsonomy.plugin.jabref.worker.ImportPostsByCriteriaWorker;

import net.sf.jabref.JabRefFrame;

/**
 * {@link ImportAllMyPostsAction} runs the {@link ImportPostsByCriteriaWorker} to import all posts of the user.
 * @author Waldemar Biller <biller@cs.uni-kassel.de>
 *
 */
public class ImportAllMyPostsAction extends AbstractPluginAction {

	public ImportAllMyPostsAction(JabRefFrame jabRefFrame) {
		
		super(jabRefFrame);
	}

	private static final long serialVersionUID = -6627950788884668738L;

	public void actionPerformed(ActionEvent e) {
		
		ImportPostsByCriteriaWorker worker = new ImportPostsByCriteriaWorker(getJabRefFrame(), "", SearchType.FULL_TEXT, GroupingEntity.USER, PluginProperties.getUsername(), true);
		performAsynchronously(worker);
	}

}

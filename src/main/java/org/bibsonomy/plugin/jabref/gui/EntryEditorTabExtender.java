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

package org.bibsonomy.plugin.jabref.gui;

import org.bibsonomy.plugin.jabref.PluginGlobals;
import org.bibsonomy.plugin.jabref.PluginProperties;

import net.sf.jabref.EntryEditor;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;


/**
 * {@link EntryEditorTabExtender} extends the {@link EntryEditor} with custom tabs.
 * @author Waldemar Biller <biller@cs.uni-kassel.de>
 *
 */
public class EntryEditorTabExtender {

	public static void extend() {
		
		boolean generalTab = false, bibsonomyTab = false, extraTab = false;
		int lastTabId = 0, extraTabID = -1;
		
		JabRefPreferences preferences = JabRefPreferences.getInstance();
		if(preferences.hasKey(JabRefPreferences.CUSTOM_TAB_NAME)) {
			
			
			while(preferences.hasKey(JabRefPreferences.CUSTOM_TAB_NAME + lastTabId)) {
				
				if(preferences.get(JabRefPreferences.CUSTOM_TYPE_NAME + lastTabId).equals(Globals.lang("General")))
					generalTab = true;
				
				if(preferences.get(JabRefPreferences.CUSTOM_TAB_NAME + lastTabId).equals(PluginGlobals.PLUGIN_NAME))
					bibsonomyTab = true;
				
				if("Extra".equals(preferences.get(JabRefPreferences.CUSTOM_TAB_NAME + lastTabId))) {
					extraTab = true; extraTabID = lastTabId;
				}
				
				lastTabId++;
			}
		}
		
		if(!generalTab) {
			
			preferences.put(JabRefPreferences.CUSTOM_TAB_FIELDS + lastTabId, "crossref;file;doi;url;citeseerurl;comment;owner;timestamp");
			preferences.put(JabRefPreferences.CUSTOM_TAB_NAME + lastTabId, Globals.lang("General"));
			lastTabId++;
		}
		
		if (!bibsonomyTab) {
			preferences.put(JabRefPreferences.CUSTOM_TAB_FIELDS + lastTabId, "interhash;intrahash;keywords;groups;privnote");
			preferences.put(JabRefPreferences.CUSTOM_TAB_NAME + lastTabId, "Bibsonomy");
			lastTabId++;
		}

		if (!extraTab) {
			preferences.put(JabRefPreferences.CUSTOM_TAB_FIELDS + lastTabId, PluginProperties.getExtraTabFields());
			preferences.put(JabRefPreferences.CUSTOM_TAB_NAME + lastTabId, "Extra");
		}

		if (extraTab) {
			if (!preferences.get(JabRefPreferences.CUSTOM_TAB_FIELDS + extraTabID).equals(
					PluginProperties.getExtraTabFields())) {
				preferences.put(JabRefPreferences.CUSTOM_TAB_FIELDS + extraTabID,
						PluginProperties.getExtraTabFields());
			}
		}
	}
}

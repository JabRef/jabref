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

package org.bibsonomy.plugin.jabref;

/**
 * Provide some default values for configuration of the plugin.
 * @author Waldemar Biller <biller@cs.uni-kassel.de>
 *
 */
public class PluginGlobals {

	public static final String PLUGIN_NAME = "BibSonomy";

	public static final String API_URL = "http://www.bibsonomy.org/api/";
	
	public static final String API_USERNAME = "jabreftest";
	
	public static final String API_KEY = "4cc8425ab4dfcce2c5d1b5a96d2c7134";
	
	public static final String PLUGIN_NUMBER_OF_POSTS_PER_REQUEST = "20";
	
	public static final String PLUGIN_FILE_DIRECTORY = System.getProperty("user.dir");
}

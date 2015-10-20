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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bibsonomy.common.enums.GroupingEntity;
import org.bibsonomy.model.enums.Order;

/**
 * {@link PluginProperties} read and write the plugin properties file.
 * @author Waldemar Biller <biller@cs.uni-kassel.de>
 *
 */
public class PluginProperties extends Properties {

	private static final long serialVersionUID = 5420450194355211249L;
	
	private static final Log LOG = LogFactory.getLog(PluginProperties.class);
	
	/**
	 * API properties
	 */
	private static final String API_URL = "api.url";
	private static final String API_USERNAME = "api.username";
	private static final String API_KEY = "api.key";
	
	/**
	 * Plugin properties
	 */
	private static final String PLUGIN_SAVE_API_KEY = "plugin.saveapikey";
	private static final String PLUGIN_DOCUMENTS_IMPORT = "plugin.documents.import";
	private static final String PLUGIN_DOCUMENTS_EXPORT = "plugin.documents.export";
	private static final String PLUGIN_TAGS_REFRESH_ON_STARTUP = "plugin.tags.refreshonstartup";
	private static final String PLUGIN_TAGS_IGNORE_NO_TAGS = "plugin.tags.ignorenotags";
	private static final String PLUGIN_NUMBER_OF_POSTS_PER_REQUEST = "plugin.request.size";
	private static final String PLUGIN_IGNORE_WARNING_MORE_POSTS = "plugin.request.size.ignorewarning";
	private static final String PLUGIN_EXTRA_TAB_FIELDS = "plugin.tabs.extra";
	private static final String PLUGIN_VISIBILITY = "plugin.visibilty";
	private static final String PLUGIN_TAG_CLOUD_SIZE = "plugin.tagcloud.size";
	private static final String PLUGIN_SIDE_PANE_VISIBILITY_TYPE = "plugin.sidepane.visibility.type";
	private static final String PLUGIN_SIDE_PANE_VISIBILITY_NAME = "plugin.sidepane.visibility.name";
	private static final String PLUGIN_TAG_CLOUD_ORDER = "plugin.tagcloud.order";
	
	/**
	 * Singleton of {@link PluginProperties}
	 */
	private static PluginProperties INSTANCE = null;
			
	/**
	 * location of properties file; JabRef stores the plugin itself also in this
	 * directory.
	 */
	private static final String PATH_TO_PROPERTIES_FILE = System.getProperty("user.home") + "/.jabref/plugins/";
	private static final String PROPERTIES_FILE = PATH_TO_PROPERTIES_FILE + "bibsonomy-plugin.properties";

	/**
	 * Get the singleton of {@link PluginProperties}. 
	 * Will create one if INSTANCE is null
	 * @return singleton of {@link PluginProperties}
	 */
	public static PluginProperties getInstance() {
		
		if(INSTANCE == null)
			INSTANCE = new PluginProperties();
		
		return INSTANCE;
	}
	
	/**
	 * The constructor reads the properties from the file system.
	 */
	private PluginProperties() {
		
		try {
			File propertiesFile = new File(PROPERTIES_FILE);
			if(propertiesFile.exists() && propertiesFile.isFile())
				load(new FileInputStream(propertiesFile));
		} catch(Exception e) {
			LOG.error("Error loading properties file", e);
		}
	}
	
	/**
	 * Saves the properties to the file system. 
	 * Checks if the option to store the API is checked.
	 */
	public static void save() {
		
		String apiKey = getApiKey();
		if(getStoreApiKey() == false)
			setApiKey(""); //set the api key to empty string 
						   //if user does not want to save api key
		try {
			
			getInstance().store(new FileOutputStream(PROPERTIES_FILE), "");
		} catch(Exception ex) {
			
			LOG.error("Failed saving properties file");
		}
		//set api key back to its actual value
		setApiKey(apiKey);
	}

	public static boolean ignoreNoTagsAssigned() {
		
		return Boolean.valueOf(getInstance().getProperty(PLUGIN_TAGS_IGNORE_NO_TAGS, "false"));
	}

	public static String getUsername() {
		
		return getInstance().getProperty(API_USERNAME, PluginGlobals.API_USERNAME);
	}

	public static String getApiKey() {
	
		return getInstance().getProperty(API_KEY, PluginGlobals.API_KEY);
	}

	public static String getApiUrl() {
		
		return getInstance().getProperty(API_URL, PluginGlobals.API_URL);
	}

	public static boolean getDownloadDocumentsOnImport() {
		
		return Boolean.parseBoolean(getInstance().getProperty(PLUGIN_DOCUMENTS_IMPORT, "true"));
	}
	
	public static int getNumberOfPostsPerRequest() {
		
		return Integer.parseInt(getInstance().getProperty(PLUGIN_NUMBER_OF_POSTS_PER_REQUEST, PluginGlobals.PLUGIN_NUMBER_OF_POSTS_PER_REQUEST));
	}
	
	public static boolean getIgnoreMorePostsWarning() {
		
		return Boolean.parseBoolean(getInstance().getProperty(PLUGIN_IGNORE_WARNING_MORE_POSTS, "false"));
	}

	public static String getExtraTabFields() {
		
		return getInstance().getProperty(PLUGIN_EXTRA_TAB_FIELDS, "issn;isbn");
	}

	public static String getDefaultVisibilty() {
		
		return getInstance().getProperty(PLUGIN_VISIBILITY, "public");
	}

	public static boolean getStoreApiKey() {
		
		return Boolean.parseBoolean(getInstance().getProperty(PLUGIN_SAVE_API_KEY, "true"));
	}

	public static boolean getUpdateTagsOnStartUp() {
		
		return Boolean.parseBoolean(getInstance().getProperty(PLUGIN_TAGS_REFRESH_ON_STARTUP, "false"));
	}

	public static boolean getUploadDocumentsOnExport() {
		
		return Boolean.parseBoolean(getInstance().getProperty(PLUGIN_DOCUMENTS_EXPORT, "true"));
	}

	public static int getTagCloudSize() {
		
		return Integer.parseInt(getInstance().getProperty(PLUGIN_TAG_CLOUD_SIZE, "100"));
	}

	public static void setUsername(String text) {
		getInstance().setProperty(API_USERNAME, text);
	}

	public static void setApiKey(String text) {
		getInstance().setProperty(API_KEY, text);
	}

	public static void setStoreApiKey(boolean selected) {
		getInstance().setProperty(PLUGIN_SAVE_API_KEY, String.valueOf(selected));
	}

	public static void setNumberOfPostsPerRequest(int value) {
		
		getInstance().setProperty(PLUGIN_NUMBER_OF_POSTS_PER_REQUEST, String.valueOf(value));
	}

	public static void setTagCloudSize(int value) {
		
		getInstance().setProperty(PLUGIN_TAG_CLOUD_SIZE, String.valueOf(value));
		
	}

	public static void setIgnoreNoTagsAssigned(boolean selected) {
		
		getInstance().setProperty(PLUGIN_TAGS_IGNORE_NO_TAGS, String.valueOf(selected));
	}

	public static void setUpdateTagsOnStartup(boolean selected) {
		
		getInstance().setProperty(PLUGIN_TAGS_REFRESH_ON_STARTUP, String.valueOf(selected));
	}

	public static void setUploadDocumentsOnExport(boolean selected) {
		
		getInstance().setProperty(PLUGIN_DOCUMENTS_EXPORT, String.valueOf(selected));
		
	}

	public static void setDownloadDocumentsOnImport(boolean selected) {

		getInstance().setProperty(PLUGIN_DOCUMENTS_IMPORT, String.valueOf(selected));
	}

	public static void setDefaultVisisbility(String key) {
		getInstance().setProperty(PLUGIN_VISIBILITY, key);
	}

	public static void setIgnoreMorePostsWarning(boolean selected) {
		getInstance().setProperty(PLUGIN_IGNORE_WARNING_MORE_POSTS, String.valueOf(selected));
	}

	public static void setExtraFields(String text) {
		getInstance().setProperty(PLUGIN_EXTRA_TAB_FIELDS, text);
	}
	
	public static GroupingEntity getSidePaneVisibilityType() {
		
		return GroupingEntity.getGroupingEntity(getInstance().getProperty(PLUGIN_SIDE_PANE_VISIBILITY_TYPE, "ALL"));
	}
	
	public static String getSidePaneVisibilityName() {
		
		return getInstance().getProperty(PLUGIN_SIDE_PANE_VISIBILITY_NAME, "all users");
	}
	
	public static void setSidePaneVisibilityType(GroupingEntity entity) {
		
		getInstance().setProperty(PLUGIN_SIDE_PANE_VISIBILITY_TYPE, entity.toString());
	}
	
	public static void setSidePaneVisibilityName(String value) {
		
		getInstance().setProperty(PLUGIN_SIDE_PANE_VISIBILITY_NAME, value);
	}

	public static Order getTagCloudOrder() {
		
		String order = getInstance().getProperty(PLUGIN_TAG_CLOUD_ORDER, "FREQUENCY");
		return Order.getOrderByName(order);
	}
	
	public static void setTagCloudOrder(Order order) {
		
		getInstance().setProperty(PLUGIN_TAG_CLOUD_ORDER, order.toString());
	}

	public static void setApiUrl(String text) {
		getInstance().setProperty(API_URL, text);
	}
	
}

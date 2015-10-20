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

import java.io.File;

import net.sf.jabref.JabRefFrame;
import net.sf.jabref.JabRefPreferences;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bibsonomy.model.Document;
import org.bibsonomy.model.logic.LogicInterface;
import org.bibsonomy.plugin.jabref.PluginProperties;

/**
 * Upload documents from the file system directly to the service.
 * @author Waldemar Biller <biller@cs.uni-kassel.de>
 *
 */
public class UploadDocumentsWorker extends AbstractPluginWorker {
	
	private static final Log LOG = LogFactory.getLog(ExportWorker.class);

	private static final String DEFAULT_FILE_DIRECTORY = System.getProperty("user.dir");
	
	private static String[] fileLocations = new String[] { 
		"",
		JabRefPreferences.getInstance().get("fileDirectory", DEFAULT_FILE_DIRECTORY) + System.getProperty("file.separator"),
		JabRefPreferences.getInstance().get("pdfDirectory", DEFAULT_FILE_DIRECTORY) + System.getProperty("file.separator"), 
		JabRefPreferences.getInstance().get("psDirectory", DEFAULT_FILE_DIRECTORY) + System.getProperty("file.separator"),
		DEFAULT_FILE_DIRECTORY + System.getProperty("file.separator")
	};
	
	
	private String intrahash;
	private String files;

	public void run() {
		// abort upload if user disabled the option
		if (!PluginProperties.getUploadDocumentsOnExport()) {
			return;
		}
		
		LogicInterface logic = getLogic();
		// split the filey by ; character
		for (String file : files.split(";")) {			
			// get file name
			int firstColonPosition = file.indexOf(":");
			int lastColonPosition = file.lastIndexOf(":");
			String fileName = file.substring(firstColonPosition + 1, lastColonPosition);
			
			try {
				
				for(String location : fileLocations) {

					// replace all \: in the file name with : - this issues the windows file names
					fileName = fileName.replaceAll("\\\\:", ":");
					
					File f = new File(location + fileName);
					if(f.exists()) {
						
						// upload the document
						
						
						final Document doc = new Document();
						doc.setFile(f);
						doc.setUserName(PluginProperties.getUsername());
						logic.createDocument(doc, intrahash);
						
						jabRefFrame.output("Uploading document " + fileName);
						
						break;
					}
					
				}
			} catch (Exception ex) {
				
				LOG.error("Failed to upload document " + fileName);
			}
		}
		jabRefFrame.output("Done.");
	}

	public UploadDocumentsWorker(JabRefFrame jabRefFrame, String intrahash, String files) {
		super(jabRefFrame);
		this.intrahash = intrahash;
		this.files = files;
	}
}

/*
 * Created on 08.03.2013
 */
package org.bibsonomy.plugin.jabref.util;


import net.sf.jabref.GUIGlobals;
import net.sf.jabref.JabRefFrame;

import org.bibsonomy.plugin.jabref.PluginGlobals;
import org.bibsonomy.rest.client.util.MultiDirectoryFileFactory;

public class JabRefFileFactory extends MultiDirectoryFileFactory {
	
	private final JabRefFrame jabRefFrame;
	
	public JabRefFileFactory(JabRefFrame jabRefFrame) {
		super(null,null,null);
		this.jabRefFrame = jabRefFrame;
	}
	
	@Override
	public String getPsDirectory() {
		String psFileDir = JabRefGlobalsHelper.getDBSpecificPSDirectory(jabRefFrame);
		if (psFileDir != null) return psFileDir;
		
		return getFileDirectory();
	}
	
	@Override
	public String getPdfDirectory() { 
		String pdfFileDir = JabRefGlobalsHelper.getDBSpecificPDFDirectory(jabRefFrame);
		if (pdfFileDir != null) return pdfFileDir;
		
		return getFileDirectory();
	}

	@Override
	public String getFileDirectory() {
		String fileDir = JabRefGlobalsHelper.getDBSpecificUserFileDirIndividual(jabRefFrame);
		if (fileDir != null) return fileDir;
		
		fileDir = JabRefGlobalsHelper.getDBSpecificUserFileDir(jabRefFrame);
		if (fileDir != null) return fileDir;
		
		String[] fileDirs = jabRefFrame.basePanel().metaData().getFileDirectory(GUIGlobals.FILE_FIELD);
		if ((fileDirs != null) && (fileDirs.length > 0)) {
			if (fileDirs[0] != null) {
				return fileDirs[0];
			}
		}
		
		return PluginGlobals.PLUGIN_FILE_DIRECTORY;	
	}
	
}

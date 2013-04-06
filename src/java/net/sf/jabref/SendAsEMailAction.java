/*  Copyright (C) 2003-2011 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.logging.Logger;

import net.sf.jabref.export.LatexFieldFormatter;
import net.sf.jabref.gui.FileListEntry;
import net.sf.jabref.gui.FileListTableModel;

/**
 * Sends the selected entry as email - by Oliver Kopp
 * 
 * It uses the mailto:-mechanism
 * 
 * Microsoft Outlook does not support attachments via mailto
 * Therefore, the folder(s), where the file(s) belonging to the entry are stored,
 * are opened. This feature is disabled by default and can be switched on at
 * preferences/external programs   
 */
public class SendAsEMailAction extends AbstractWorker {
    private static final Logger logger = Logger.getLogger(SendAsEMailAction.class.getName());

	String message = null;
	private JabRefFrame frame;

	public SendAsEMailAction(JabRefFrame frame) {
		this.frame = frame;
	}

	public void run() {
        if (!Desktop.isDesktopSupported()) {
        	message = Globals.lang("Error creating email");
        	return;
        }
		
		BasePanel panel = frame.basePanel();
		if (panel == null)
			return;
		if (panel.getSelectedEntries().length == 0) {
			message = Globals.lang("No entries selected.");
			return;
		}

		StringWriter sw = new StringWriter();
		BibtexEntry[] bes = panel.getSelectedEntries();

		LatexFieldFormatter ff = new LatexFieldFormatter();

		ArrayList<String> attachments = new ArrayList<String>();
		
		// open folders is needed to indirectly support email programs, which cannot handle
		//   the unofficial "mailto:attachment" property 
		boolean openFolders = JabRefPreferences.getInstance().getBoolean("openFoldersOfAttachedFiles");
		
		for (BibtexEntry entry : bes) {
			try {
				entry.write(sw, ff, true);
                FileListTableModel tm = new FileListTableModel();
                tm.setContent(entry.getField("file"));
                for (int i=0; i< tm.getRowCount(); i++) {
                	FileListEntry flEntry = tm.getEntry(i);
                	
                	File f = Util.expandFilename(flEntry.getLink(), frame.basePanel().metaData().getFileDirectory(GUIGlobals.FILE_FIELD));
                	if (f != null) {
                		// file exists
                		attachments.add(f.getPath());
                		if (openFolders) {
                			try {
                				Util.openFolderAndSelectFile(f.getAbsolutePath());
                			} catch (IOException e) {
                				logger.fine(e.getMessage());
                			}
                		}
                   	}
                }
			} catch (Exception e) {
				e.printStackTrace();
				message = Globals.lang("Error creating email");
				return;
			}
		}
		
		String mailTo = "?Body=".concat(sw.getBuffer().toString());
        mailTo = mailTo.concat("&Subject=");
        mailTo = mailTo.concat(JabRefPreferences.getInstance().get(JabRefPreferences.EMAIL_SUBJECT));
        for (String path: attachments) {
        	mailTo = mailTo.concat("&Attachment=\"").concat(path);
        	mailTo = mailTo.concat("\"");
        }
        
        URI uriMailTo = null;
        try {
			uriMailTo = new URI("mailto", mailTo, null);
		} catch (URISyntaxException e1) {
			e1.printStackTrace();
			message = Globals.lang("Error creating email");
			return;
		}

        Desktop desktop = Desktop.getDesktop();
        try {
			desktop.mail(uriMailTo);
		} catch (IOException e) {
			e.printStackTrace();
			message = Globals.lang("Error creating email");
			return;
		}

		message = String.format("%s: %d",
				Globals.lang("Entries added to an email"), bes.length);
	}

	public void update() {
		frame.output(message);
	}

}

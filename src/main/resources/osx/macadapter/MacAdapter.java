/*  Copyright (C) 2015 JabRef contributors.
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
package osx.macadapter;

import java.io.File;
import java.util.List;

import net.sf.jabref.JabRefFrame;

public class MacAdapter implements PreferencesHandler, AboutHandler, QuitHandler, OpenFilesHandler {

	private JabRefFrame parentFrame = null;
	
	public void registerMacEvents(JabRefFrame inputFrame) {
		parentFrame = inputFrame;
		Application.getApplication().setOpenFileHandler(this);
		Application.getApplication().setAboutHandler(this);
		Application.getApplication().setPreferencesHandler(this);
		Application.getApplication().setQuitHandler(this);
		FullScreenUtilities.setWindowCanFullScreen(parentFrame, true);
	}
	
	@Override
	// The OSXAdapter calls this method when a ".bib" file has been double-clicked from the Finder.	
	public void openFiles(OpenFilesEvent event) {
		if (parentFrame != null) {
			List<File> files = event.getFiles();
		
			for (int i=0; i<files.size(); i++) {
                parentFrame.openAction(files.get(i).getAbsolutePath());
            }
		}
	} 

	@Override
	public void handleQuitRequestWith(QuitEvent evt, QuitResponse resp) {
		if (parentFrame != null) {
			if (parentFrame.quit()) {
                resp.performQuit();
            } else {
                resp.cancelQuit();
            }
		}
	}

	@Override
	public void handleAbout(AboutEvent arg0) {
		if (parentFrame != null) {
            parentFrame.about();
        }
	}

	@Override
	public void handlePreferences(PreferencesEvent arg0) {
		if (parentFrame != null) {
            parentFrame.preferences();
        }
	} 	
}

package osx.macadapter;

import java.io.File;
import java.util.List;

import net.sf.jabref.JabRefFrame;

import com.apple.eawt.*;
import com.apple.eawt.AppEvent.AboutEvent;
import com.apple.eawt.AppEvent.OpenFilesEvent;
import com.apple.eawt.AppEvent.PreferencesEvent;
import com.apple.eawt.AppEvent.QuitEvent;

public class MacAdapter implements PreferencesHandler, AboutHandler, QuitHandler, OpenFilesHandler {

	private JabRefFrame parentFrame;
	
	public MacAdapter(JabRefFrame inputFrame) {
		parentFrame = inputFrame;
		Application.getApplication().setOpenFileHandler(this);
	}
	
	@Override
	// The OSXAdapter calls this method when a ".bib" file has been double-clicked from the Finder.	
	public void openFiles(OpenFilesEvent event) {
		List<File> files = event.getFiles();
		
		for (int i=0; i<files.size(); i++) {
			parentFrame.openAction(files.get(i).getAbsolutePath());
		}
	} 

	@Override
	public void handleQuitRequestWith(QuitEvent arg0, QuitResponse arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleAbout(AboutEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handlePreferences(PreferencesEvent arg0) {
		// TODO Auto-generated method stub
		
	} 	
}

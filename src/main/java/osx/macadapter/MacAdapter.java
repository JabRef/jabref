package osx.macadapter;

import java.io.File;
import java.util.List;

import org.jabref.gui.JabRefFrame;

import com.apple.eawt.AboutHandler;
import com.apple.eawt.AppEvent;
import com.apple.eawt.AppEvent.AboutEvent;
import com.apple.eawt.AppEvent.OpenFilesEvent;
import com.apple.eawt.AppEvent.PreferencesEvent;
import com.apple.eawt.AppEvent.QuitEvent;
import com.apple.eawt.AppReOpenedListener;
import com.apple.eawt.Application;
import com.apple.eawt.FullScreenUtilities;
import com.apple.eawt.OpenFilesHandler;
import com.apple.eawt.PreferencesHandler;
import com.apple.eawt.QuitHandler;
import com.apple.eawt.QuitResponse;

public class MacAdapter implements PreferencesHandler, AboutHandler, QuitHandler, OpenFilesHandler, AppReOpenedListener {

    private JabRefFrame parentFrame;

    public void registerMacEvents(JabRefFrame inputFrame) {
        parentFrame = inputFrame;
        Application.getApplication().setOpenFileHandler(this);
        Application.getApplication().setAboutHandler(this);
        Application.getApplication().setPreferencesHandler(this);
        Application.getApplication().setQuitHandler(this);
        Application.getApplication().addAppEventListener(this);
        FullScreenUtilities.setWindowCanFullScreen(parentFrame, true);
    }

    @Override
    // The OSXAdapter calls this method when a ".bib" file has been double-clicked from the Finder.
    public void openFiles(OpenFilesEvent event) {
        if (parentFrame == null) {
            return;
        }

        List<File> files = event.getFiles();

        for (File file : files) {
            parentFrame.openAction(file.getAbsolutePath());
        }
    }

    @Override
    public void handleQuitRequestWith(QuitEvent evt, QuitResponse resp) {
        if (parentFrame == null) {
            return;
        }

        if (parentFrame.quit()) {
            resp.performQuit();
        } else {
            resp.cancelQuit();
        }
    }

    @Override
    public void handleAbout(AboutEvent arg0) {
        if (parentFrame == null) {
            return;
        }

        parentFrame.about();
    }

    @Override
    public void handlePreferences(PreferencesEvent arg0) {
        if (parentFrame == null) {
            return;
        }

        parentFrame.showPreferencesDialog();
    }

    @Override
    public void appReOpened(AppEvent.AppReOpenedEvent appReOpenedEvent) {
        parentFrame.setVisible(true);
    }
}

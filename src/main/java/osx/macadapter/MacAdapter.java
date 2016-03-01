package osx.macadapter;/*  Copyright (C) 2015 JabRef contributors.
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

import com.apple.eawt.*;
import com.apple.eawt.AppEvent.AboutEvent;
import com.apple.eawt.AppEvent.OpenFilesEvent;
import com.apple.eawt.AppEvent.PreferencesEvent;
import com.apple.eawt.AppEvent.QuitEvent;
import net.sf.jabref.gui.JabRefFrame;

import java.io.File;
import java.util.List;

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

        parentFrame.preferences();
    }

    @Override
    public void appReOpened(AppEvent.AppReOpenedEvent appReOpenedEvent) {
        parentFrame.setVisible(true);
    }
}

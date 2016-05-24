package net.sf.jabref.gui.net;

import java.awt.Component;
import java.io.InputStream;
import java.net.URL;

import javax.swing.ProgressMonitorInputStream;

import net.sf.jabref.logic.net.URLDownload;

public class MonitoredURLDownload {

    public static URLDownload buildMonitoredDownload(final Component component, URL source) {
        return new URLDownload(source) {

            @Override
            protected InputStream monitorInputStream(InputStream in) {
                return new ProgressMonitorInputStream(component, "Downloading " + this.getSource(), in);
            }
        };
    }
}

package net.sf.jabref.gui.net;

import net.sf.jabref.logic.net.URLDownload;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;
import java.net.URL;

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

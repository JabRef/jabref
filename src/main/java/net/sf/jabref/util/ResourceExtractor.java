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
package net.sf.jabref.util;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import net.sf.jabref.gui.net.MonitoredURLDownload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jabref.JabRef;
import net.sf.jabref.gui.worker.Worker;
import net.sf.jabref.logic.net.URLDownload;

/**
 * This class performs the somewhat weird action of extracting a file from within the running JabRef jar,
 * and storing it to the given File. It may prove useful e.g. for extracting Endnote export/import filters which
 * are needed for full integration with the export filter in JabRef, so we can bundle these for the user even though
 * they are not used by JabRef directly.
 *
 *
 *
 * @author alver
 */
public class ResourceExtractor implements Worker {

    private final URL resource;
    private final Component parent;
    private final File destination;

    private static final Log LOGGER = LogFactory.getLog(ResourceExtractor.class);


    /** Creates a new instance of ResourceExtractor */
    public ResourceExtractor(final Component parent, final String filename, File destination) {
        resource = JabRef.class.getResource(filename);
        //System.out.println(filename+"\n"+resource);
        this.parent = parent;
        this.destination = destination;
    }

    @Override
    public void run() {
        URLDownload ud = MonitoredURLDownload.buildMonitoredDownload(parent, resource);
        try {
            ud.downloadToFile(destination);
        } catch (IOException ex) {
            LOGGER.info("Error extracting resource.", ex);
        }
    }
}

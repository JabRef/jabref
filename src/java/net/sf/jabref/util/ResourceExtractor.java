/*
 * ResourceExtractor.java
 *
 * Created on January 20, 2005, 10:37 PM
 */

package net.sf.jabref.util;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRef;
import net.sf.jabref.Worker;
import net.sf.jabref.net.URLDownload;
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
    
    final URL resource;
    final Component parent;
    final File destination;
    
    /** Creates a new instance of ResourceExtractor */
    public ResourceExtractor(final Component parent, final String filename, File destination) {
         resource = JabRef.class.getResource(filename);
         //System.out.println(filename+"\n"+resource);
         this.parent = parent;
         this.destination = destination;
    }
    
    public void run() {
        URLDownload ud = new URLDownload(parent, resource, destination);
        try {
            ud.download();
        } catch (IOException ex) {
            Globals.logger("Error extracting resource: "+ex.getMessage());            
        }
    }
}

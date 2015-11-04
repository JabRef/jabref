/*  Copyright (C) 2003-2015 JabRef contributors.
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
package net.sf.jabref.gui.help;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Stack;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import net.sf.jabref.gui.GUIGlobals;
import net.sf.jabref.JabRef;
import net.sf.jabref.JabRefPreferences;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class HelpContent extends JTextPane {

    private static final Log LOGGER = LogFactory.getLog(HelpContent.class);

    private final JScrollPane pane;

    private final Stack<URL> history;
    private final Stack<URL> forw;

    private final JabRefPreferences prefs;


    public HelpContent(JabRefPreferences prefs_) {
        super();
        pane = new JScrollPane(this, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        pane.setDoubleBuffered(true);
        prefs = prefs_;
        history = new Stack<>();
        forw = new Stack<>();
        setEditorKitForContentType("text/html", new MyEditorKit());
        setContentType("text/html");
        setText("");
        setEditable(false);

        addHyperlinkListener(new HyperlinkListener() {

            private boolean lastStatusUpdateWasALink = false;

            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                String link = e.getDescription();
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    lastStatusUpdateWasALink = false;
                    // Handles Anchors
                    if (link.startsWith("#")) {
                        scrollToReference(link.substring(1));
                    } else {
                        if (link.startsWith("http")) {
                            //  open all external URLs externally
                            JabRef.jrf.openBrowser(link);
                        } else {
                            // nothing has to be done: JTextFrame handles internal links by itself
                        }
                    }
                } else if (e.getEventType() == HyperlinkEvent.EventType.ENTERED) {
                    // show the link in the status bar - similar to Firefox behavior
                    JabRef.jrf.setStatus(link);
                    lastStatusUpdateWasALink = true;
                } else {
                    if (lastStatusUpdateWasALink) {
                        // remove the link from the status bar
                        JabRef.jrf.setStatus("");
                        lastStatusUpdateWasALink = false;
                    }
                }
            }
        });
    }

    public boolean back() {
        if (!history.empty()) {
            URL prev = (history.pop());
            forw.push(getPage());
            setPageOnly(prev);
        }
        return !history.empty();
    }

    public boolean forward() {
        if (!forw.empty()) {
            URL next = (forw.pop());
            history.push(getPage());
            setPageOnly(next);
        }
        return !forw.empty();
    }

    public void reset() {
        forw.removeAllElements();
        history.removeAllElements();
    }

    // .getResource is called at resourceOwner. This method is available at all Class objects
    @SuppressWarnings("rawtypes")
    public void setPage(String filename, Class resourceOwner) {

        // Check for anchor
        int indexOf = filename.indexOf('#');
        String file;
        String reference;

        if (indexOf != -1) {
            file = filename.substring(0, indexOf);
            reference = filename.substring(indexOf + 1);
        } else {
            file = filename;
            reference = "";
        }

        String middle = prefs.get(JabRefPreferences.LANGUAGE) + '/';

        URL old = getPage();
        try {
            // First check in specified language
            URL resource = resourceOwner.getResource(GUIGlobals.helpPre + middle + file);

            // If not available fallback to english
            if (resource == null) {
                resource = resourceOwner.getResource(GUIGlobals.helpPre + file);
            }

            // If still not available print a warning
            if (resource == null) {
                // TODO show warning to user
                HelpContent.LOGGER.error("Could not find html-help for file '" + file + "'.");
                return;
            }
            setPageOnly(new URL(resource.toString() + '#' + reference));

        } catch (IOException ex) {
            LOGGER.warn("Problem when finding help files.", ex);
        }

        forw.removeAllElements();
        if (old != null) {
            history.push(old);
        }

    }

    /**
     * Convenience method for setPage(String)
     */
    @Override
    public void setPage(URL url) {
        File f = new File(url.getPath());
        setPage(f.getName(), JabRef.class);
    }

    private void setPageOnly(URL url) {
        try {
            super.setPage(url);
        } catch (IOException ex) {
            if (url == null) {
                System.out.println("Error: Help file not set");
            } else {
                System.out.println("Error: Help file not found '" + url.getFile() + '\'');
            }
        }
    }

    public JComponent getPane() {
        return pane;
    }

    /*public void paintComponent(Graphics g) {
    	Graphics2D g2 = (Graphics2D) g;
    	g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    	g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    	super.paintComponent(g2);
    }*/

}

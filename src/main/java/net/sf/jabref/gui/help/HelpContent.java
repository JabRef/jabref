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

import net.sf.jabref.JabRef;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.gui.GUIGlobals;

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
                if (e.getEventType() == HyperlinkEvent.EventType.ENTERED) {
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
            setPageOnly(prev, null);
        }
        return !history.empty();
    }

    public boolean forward() {
        if (!forw.empty()) {
            URL next = (forw.pop());
            history.push(getPage());
            setPageOnly(next, null);
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
        String anchorName = null;

        if (indexOf != -1) {
            file = filename.substring(0, indexOf);
            anchorName = filename.substring(indexOf + 1);
        } else {
            file = filename;
        }

        String middle = prefs.get(JabRefPreferences.LANGUAGE) + '/';

        URL old = getPage();

        // First check in specified language
        URL resource = resourceOwner.getResource(GUIGlobals.helpPre + middle + file);

        // If not available fallback to english
        if (resource == null) {
            resource = resourceOwner.getResource(GUIGlobals.helpPre + "en/" + file);
            LOGGER.info("No localization available for file '" + file + "'. Falling back to English.");
        }

        // If still not available print a warning
        if (resource == null) {
            // TODO show warning to user
            LOGGER.error("Could not find html-help for file '" + file + "'.");
            return;
        }

        setPageOnly(resource, anchorName);

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
        if ("file".equals(url.getProtocol())) {
            // Creating file by url.toString() and using file.getName() preserves anchors
            File file = new File(url.toString());
            setPage(file.getName(), JabRef.class);
        } else {
            //  open all external URLs externally
            JabRef.jrf.openBrowser(url.toString());
        }
    }

    private void setPageOnly(URL baseUrl, String anchorName) {
        try {
            URL url;
            if(anchorName!=null) {
                url = new URL(baseUrl.toString()+"#"+anchorName);
            } else {
                url = baseUrl;
            }
            super.setPage(url);
            // if anchor is present - scroll to it
            String stringUrl = url.toString();
            if (stringUrl.contains("#")) {
                scrollToReference(stringUrl.substring(stringUrl.indexOf("#")));
            }
        } catch (IOException ex) {
            if (baseUrl == null) {
                LOGGER.error("Error: Help file not set");
            } else {
                LOGGER.error("Error: Help file not found '" + baseUrl.getFile() + '\'');
            }
        }
    }

    public JComponent getPane() {
        return pane;
    }

}

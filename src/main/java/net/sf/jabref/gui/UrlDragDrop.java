/*
 Copyright (C) 2004 E. Putrycz

 All programs in this directory and
 subdirectories are published under the GNU General Public License as
 described below.

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or (at
 your option) any later version.

 This program is distributed in the hope that it will be useful, but
 WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 USA

 Further information about the GNU GPL is available at:
 http://www.gnu.org/copyleft/gpl.ja.html

 */
package net.sf.jabref.gui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import javax.swing.JOptionPane;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.entryeditor.EntryEditor;
import net.sf.jabref.gui.fieldeditors.FieldEditor;
import net.sf.jabref.gui.net.MonitoredURLDownload;
import net.sf.jabref.logic.l10n.Localization;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Erik Putrycz erik.putrycz-at-nrc-cnrc.gc.ca
 */

public class UrlDragDrop implements DropTargetListener {

    private static final Log LOGGER = LogFactory.getLog(UrlDragDrop.class);

    private final FieldEditor feditor;

    private final EntryEditor editor;

    private final JabRefFrame frame;


    public UrlDragDrop(final EntryEditor editor, final JabRefFrame frame, final FieldEditor feditor) {
        this.editor = editor;
        this.feditor = feditor;
        this.frame = frame;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.awt.dnd.DropTargetListener#dragEnter(java.awt.dnd.DropTargetDragEvent)
     */
    @Override
    public void dragEnter(DropTargetDragEvent dtde) {
        // Do nothing
    }

    /*
     * (non-Javadoc)
     *
     * @see java.awt.dnd.DropTargetListener#dragOver(java.awt.dnd.DropTargetDragEvent)
     */
    @Override
    public void dragOver(DropTargetDragEvent dtde) {
        // Do nothing
    }

    /*
     * (non-Javadoc)
     *
     * @see java.awt.dnd.DropTargetListener#dropActionChanged(java.awt.dnd.DropTargetDragEvent)
     */
    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {
        // Do nothing
    }

    /*
     * (non-Javadoc)
     *
     * @see java.awt.dnd.DropTargetListener#dragExit(java.awt.dnd.DropTargetEvent)
     */
    @Override
    public void dragExit(DropTargetEvent dte) {
        // Do nothing
    }


    private static class JOptionChoice {

        private final String label;

        private final int id;


        public JOptionChoice(final String label, final int id) {
            this.label = label;
            this.id = id;
        }

        @Override
        public String toString() {
            return label;
        }

        public int getId() {
            return id;
        }

    }


    /*
     * (non-Javadoc)
     *
     * @see java.awt.dnd.DropTargetListener#drop(java.awt.dnd.DropTargetDropEvent)
     */

    @Override
    public void drop(DropTargetDropEvent dtde) {
        Transferable tsf = dtde.getTransferable();
        dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
        //try with an URL
        DataFlavor dtURL = null;
        try {
            dtURL = new DataFlavor("application/x-java-url; class=java.net.URL");
        } catch (ClassNotFoundException e) {
            LOGGER.warn("Could not find DropTargetDropEvent class.", e);
        }
        try {
            URL url = (URL) tsf.getTransferData(dtURL);
            JOptionChoice res = (JOptionChoice) JOptionPane
                    .showInputDialog(editor, "",
                            Localization.lang("Select action"),
                            JOptionPane.QUESTION_MESSAGE, null,
                            new JOptionChoice[] {
                                    new JOptionChoice(
                                            Localization.lang("Insert URL"), 0),
                                    new JOptionChoice(
                                            Localization.lang("Download file"), 1)},
                            new JOptionChoice(Localization.lang("Insert URL"), 0));
            if (res != null) {
                switch (res.getId()) {
                //insert URL
                case 0:
                    feditor.setText(url.toString());
                    editor.updateField(feditor);
                    break;
                //download file
                case 1:
                    try {
                        //auto filename:
                        File file = new File(new File(Globals.prefs.get("pdfDirectory")),
                                editor.getEntry().getCiteKey() + ".pdf");
                        frame.output(Localization.lang("Downloading..."));
                        MonitoredURLDownload.buildMonitoredDownload(editor, url).downloadToFile(file);
                        frame.output(Localization.lang("Download completed"));
                        feditor.setText(file.toURI().toURL().toString());
                        editor.updateField(feditor);

                    } catch (IOException ioex) {
                        LOGGER.error("Error while downloading file.", ioex);
                        JOptionPane.showMessageDialog(editor, Localization.lang("File download"),
                                Localization.lang("Error while downloading file:" + ioex.getMessage()),
                                JOptionPane.ERROR_MESSAGE);
                    }
                    break;
                default:
                    LOGGER.warn("Unknown selection (should not happen)");
                    break;
                }
            }
            return;
        } catch (UnsupportedFlavorException nfe) {
            // not an URL then...
            LOGGER.warn("Could not parse URL.", nfe);
        } catch (IOException ioex) {
            LOGGER.warn("Could not perform drag and drop.", ioex);
        }

        try {
            //try with a File List
            @SuppressWarnings("unchecked")
            List<File> filelist = (List<File>) tsf
                    .getTransferData(DataFlavor.javaFileListFlavor);
            if (filelist.size() > 1) {
                JOptionPane
                        .showMessageDialog(editor,
                                Localization.lang("Only one item is supported"),
                                Localization.lang("Drag and Drop Error"),
                                JOptionPane.ERROR_MESSAGE);
                return;
            }
            File fl = filelist.get(0);
            feditor.setText(fl.toURI().toURL().toString());
            editor.updateField(feditor);

        } catch (UnsupportedFlavorException nfe) {
            JOptionPane.showMessageDialog(editor,
                    Localization.lang("Operation not supported"),
                    Localization.lang("Drag and Drop Error"), JOptionPane.ERROR_MESSAGE);
            LOGGER.warn("Could not perform drag and drop.", nfe);
        } catch (IOException ioex) {
            LOGGER.warn("Could not perform drag and drop.", ioex);
        }

    }

}

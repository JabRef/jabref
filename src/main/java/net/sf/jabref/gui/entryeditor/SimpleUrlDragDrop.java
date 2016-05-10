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
package net.sf.jabref.gui.entryeditor;

import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URL;

import javax.swing.JOptionPane;

import net.sf.jabref.gui.fieldeditors.FieldEditor;
import net.sf.jabref.logic.l10n.Localization;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Erik Putrycz erik.putrycz-at-nrc-cnrc.gc.ca
 */

class SimpleUrlDragDrop implements DropTargetListener {
    private static final Log LOGGER = LogFactory.getLog(SimpleUrlDragDrop.class);

    private final FieldEditor editor;

    private final EntryEditor.StoreFieldAction storeFieldAction;


    public SimpleUrlDragDrop(FieldEditor editor, EntryEditor.StoreFieldAction storeFieldAction) {
        this.editor = editor;
        this.storeFieldAction = storeFieldAction;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.awt.dnd.DropTargetListener#dragEnter(java.awt.dnd.DropTargetDragEvent)
     */
    @Override
    public void dragEnter(DropTargetDragEvent dtde) {
        // Ignored
    }

    /*
     * (non-Javadoc)
     *
     * @see java.awt.dnd.DropTargetListener#dragOver(java.awt.dnd.DropTargetDragEvent)
     */
    @Override
    public void dragOver(DropTargetDragEvent dtde) {
        // Ignored
    }

    /*
     * (non-Javadoc)
     *
     * @see java.awt.dnd.DropTargetListener#dropActionChanged(java.awt.dnd.DropTargetDragEvent)
     */
    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {
        // Ignored
    }

    /*
     * (non-Javadoc)
     *
     * @see java.awt.dnd.DropTargetListener#dragExit(java.awt.dnd.DropTargetEvent)
     */
    @Override
    public void dragExit(DropTargetEvent dte) {
        // Ignored
    }

    /*
     * (non-Javadoc)
     *
     * @see java.awt.dnd.DropTargetListener#drop(java.awt.dnd.DropTargetDropEvent)
     */
    @Override
    public void drop(DropTargetDropEvent event) {
        Transferable tsf = event.getTransferable();
        event.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
        //try with an URL
        DataFlavor dataFlavor = null;
        try {
            dataFlavor = new DataFlavor("application/x-java-url; class=java.net.URL");
        } catch (ClassNotFoundException e) {
            LOGGER.warn("Could not find DropTargetDropEvent class", e);
        }
        try {
            URL url = (URL) tsf.getTransferData(dataFlavor);
            //insert URL
            editor.setText(url.toString());
            storeFieldAction.actionPerformed(new ActionEvent(editor, 0, ""));
        } catch (UnsupportedFlavorException nfe) {
            // if not an URL
            JOptionPane.showMessageDialog((Component) editor,
                    Localization.lang("Operation not supported"),
                    Localization.lang("Drag and Drop Error"), JOptionPane.ERROR_MESSAGE);
            LOGGER.warn("Could not perform drage and drop", nfe);
        } catch (IOException ioex) {
            LOGGER.warn("Could not perform drage and drop", ioex);
        }
    }

}
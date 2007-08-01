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
package net.sf.jabref;

import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import net.sf.jabref.EntryEditor.StoreFieldAction;

/**
 * @author Erik Putrycz erik.putrycz-at-nrc-cnrc.gc.ca
 */

public class SimpleUrlDragDrop implements DropTargetListener {

    private static Logger logger = Logger.getLogger(SimpleUrlDragDrop.class
            .getName());

    private FieldEditor editor;

    private StoreFieldAction storeFieldAction;

    public SimpleUrlDragDrop(FieldEditor _editor,
            StoreFieldAction _storeFieldAction) {
        editor = _editor;
        storeFieldAction = _storeFieldAction;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.dnd.DropTargetListener#dragEnter(java.awt.dnd.DropTargetDragEvent)
     */
    public void dragEnter(DropTargetDragEvent dtde) {
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.dnd.DropTargetListener#dragOver(java.awt.dnd.DropTargetDragEvent)
     */
    public void dragOver(DropTargetDragEvent dtde) {
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.dnd.DropTargetListener#dropActionChanged(java.awt.dnd.DropTargetDragEvent)
     */
    public void dropActionChanged(DropTargetDragEvent dtde) {
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.dnd.DropTargetListener#dragExit(java.awt.dnd.DropTargetEvent)
     */
    public void dragExit(DropTargetEvent dte) {
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.dnd.DropTargetListener#drop(java.awt.dnd.DropTargetDropEvent)
     */
    public void drop(DropTargetDropEvent dtde) {
        Transferable tsf = dtde.getTransferable();
        dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
        //try with an URL
        DataFlavor dtURL = null;
        try{
            dtURL = new DataFlavor("application/x-java-url; class=java.net.URL");
        }catch (ClassNotFoundException e){
            logger.log(Level.WARNING,
                    "Class not found for DnD... should not happen", e);
        }
        try{
            URL url = (URL) tsf.getTransferData(dtURL);
            //insert URL
            editor.setText(url.toString());
            storeFieldAction.actionPerformed(new ActionEvent(editor, 0, ""));
            return;
        }catch (UnsupportedFlavorException nfe){
            // if not an URL
            JOptionPane.showMessageDialog((Component) editor, Globals
                    .lang("Operation not supported"), Globals
                    .lang("Drag and Drop Error"), JOptionPane.ERROR_MESSAGE);
            logger.log(Level.WARNING, "Transfer exception", nfe);
        }catch (IOException ioex){
            logger.log(Level.WARNING, "!should not happen!", ioex);
        }
    }

}
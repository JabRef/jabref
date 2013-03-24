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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import net.sf.jabref.net.URLDownload;

/**
 * @author Erik Putrycz erik.putrycz-at-nrc-cnrc.gc.ca
 */

public class UrlDragDrop implements DropTargetListener {

    private static Logger logger = Logger
            .getLogger(UrlDragDrop.class.getName());

    private FieldEditor feditor;

    private EntryEditor editor;

    private JabRefFrame frame;

    public UrlDragDrop(EntryEditor _editor, JabRefFrame _frame,
            FieldEditor _feditor) {
        editor = _editor;
        feditor = _feditor;
        frame = _frame;
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

    private static class JOptionChoice {

        private String label;

        private int id;

        public JOptionChoice(String _label, int _id) {
            label = _label;
            id = _id;
        }

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
            JOptionChoice res = (JOptionChoice) JOptionPane
                    .showInputDialog(editor, "", Globals
                            .lang("Select action"),
                            JOptionPane.QUESTION_MESSAGE, null,
                            new JOptionChoice[] {
                                    new JOptionChoice(Globals
                                            .lang("Insert URL"), 0),
                                    new JOptionChoice(Globals
                                            .lang("Download file"), 1) },
                            new JOptionChoice(Globals.lang("Insert URL"), 0));
            switch (res.getId()) {
            //insert URL
            case 0:
                feditor.setText(url.toString());
                editor.updateField(feditor);
                break;
            //download file
            case 1:
                try{
                    //auto file name:
                    File file = new File(new File(Globals.prefs
                            .get("pdfDirectory")), editor.getEntry()
                            .getField(BibtexFields.KEY_FIELD)
                            + ".pdf");
                    URLDownload udl = new URLDownload(editor, url,
                            file);
                    frame.output(Globals.lang("Downloading..."));
                    udl.download();
                    frame.output(Globals.lang("Download completed"));
                    feditor.setText(file.toURI().toURL().toString());
                    editor.updateField(feditor);

                }catch (IOException ioex){
                    logger.log(Level.SEVERE, "Error while downloading file",
                            ioex);
                    JOptionPane.showMessageDialog(editor, Globals
                            .lang("File download"), Globals
                            .lang("Error while downloading file:"
                                    + ioex.getMessage()),
                            JOptionPane.ERROR_MESSAGE);
                }
                break;
            }
            return;
        }catch (UnsupportedFlavorException nfe){
            // not an URL then...
        }catch (IOException ioex){
            logger.log(Level.WARNING, "!should not happen!", ioex);
        }
        
        try{
            //try with a File List
        	@SuppressWarnings("unchecked")
        	List<File> filelist = (List<File>) tsf
                    .getTransferData(DataFlavor.javaFileListFlavor);
            if (filelist.size() > 1){
                JOptionPane
                        .showMessageDialog(editor, Globals
                                .lang("Only one item is supported"), Globals
                                .lang("Drag and Drop Error"),
                                JOptionPane.ERROR_MESSAGE);
                return;
            }
            File fl = filelist.get(0);
            feditor.setText(fl.toURI().toURL().toString());
            editor.updateField(feditor);

        }catch (UnsupportedFlavorException nfe){
            JOptionPane.showMessageDialog(editor, Globals
                    .lang("Operation not supported"), Globals
                    .lang("Drag and Drop Error"), JOptionPane.ERROR_MESSAGE);
            logger.log(Level.WARNING, "Transfer exception", nfe);
        }catch (IOException ioex){
            logger.log(Level.WARNING, "Transfer exception", ioex);
        }

    }

}

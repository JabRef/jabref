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
package net.sf.jabref.external;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;
import java.util.ArrayList;

import javax.swing.*;

import net.sf.jabref.*;
import net.sf.jabref.gui.FileListTableModel;
import net.sf.jabref.gui.FileListEntry;
import net.sf.jabref.util.XMPUtil;

import com.jgoodies.forms.builder.ButtonBarBuilder;

/**
 * 
 * This action goes through all selected entries in the BasePanel, and attempts
 * to write the XMP data to the external pdf.
 * 
 * @author $Author$
 * @version $Revision$ ($Date$)
 * 
 */
public class WriteXMPAction extends AbstractWorker {

	BasePanel panel;

	BibtexEntry[] entries;
	
	BibtexDatabase database;

	OptionsDialog optDiag;

	boolean goOn = true;

	int skipped, entriesChanged, errors;

	public WriteXMPAction(BasePanel panel) {
		this.panel = panel;
	}

	public void init() {

		// Get entries and check if it makes sense to perform this operation
		entries = panel.getSelectedEntries();

		if (entries.length == 0) {

			database = panel.getDatabase();
			entries = database.getEntries().toArray(new BibtexEntry[]{});

			if (entries.length == 0) {

				JOptionPane.showMessageDialog(panel, Globals
					.lang("This operation requires at least one entry."), Globals
					.lang("Write XMP-metadata"), JOptionPane.ERROR_MESSAGE);
				goOn = false;
				return;

			} else {

				int response = JOptionPane.showConfirmDialog(panel, Globals
					.lang("Write XMP-metadata for all PDFs in current database?"), Globals
					.lang("Write XMP-metadata"), JOptionPane.YES_NO_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE);

				if (response != JOptionPane.YES_OPTION) {
					goOn = false;
					return;
				}
			}
		}

		errors = entriesChanged = skipped = 0;

		if (optDiag == null) {
			optDiag = new OptionsDialog(panel.frame());
		}
		optDiag.open();

		panel.output(Globals.lang("Writing XMP metadata..."));
	}

	public void run() {

		if (!goOn)
			return;

		for (int i = 0; i < entries.length; i++) {

			BibtexEntry entry = entries[i];

            // Make a list of all PDFs linked from this entry:
            List<File> files = new ArrayList<File>();

            // First check the (legacy) "pdf" field:
            String pdf = entry.getField("pdf");
            String[] dirs = panel.metaData().getFileDirectory("pdf");
            File f = Util.expandFilename(pdf, dirs);
            if (f != null)
                files.add(f);

            // Then check the "file" field:
            dirs = panel.metaData().getFileDirectory(GUIGlobals.FILE_FIELD);
            String field = entry.getField(GUIGlobals.FILE_FIELD);
            if (field != null) {
                FileListTableModel tm = new FileListTableModel();
                tm.setContent(field);
                for (int j=0; j<tm.getRowCount(); j++) {
                    FileListEntry flEntry = tm.getEntry(j);
                    if ((flEntry.getType()!=null) && (flEntry.getType().getName().toLowerCase().equals("pdf"))) {
                        f = Util.expandFilename(flEntry.getLink(), dirs);
                        if (f != null)
                            files.add(f);
                    }
                }
            }

            optDiag.progressArea.append(entry.getCiteKey() + "\n");

			if (files.size() == 0) {
				skipped++;
				optDiag.progressArea.append("  " + Globals.lang("Skipped - No PDF linked") + ".\n");
			}
            else for (File file : files) {
                if (!file.exists()) {
                    skipped++;
				    optDiag.progressArea.append("  " + Globals.lang("Skipped - PDF does not exist")
					    + ":\n");
				    optDiag.progressArea.append("    " + file.getPath() + "\n");


                } else {
                    try {
                        XMPUtil.writeXMP(file, entry, database);
                        optDiag.progressArea.append("  " + Globals.lang("Ok") + ".\n");
                        entriesChanged++;
                    } catch (Exception e) {
                        optDiag.progressArea.append("  " + Globals.lang("Error while writing") + " '"
                            + file.getPath() + "':\n");
                        optDiag.progressArea.append("    " + e.getLocalizedMessage() + "\n");
                        errors++;
                    }
                }
            }

            if (optDiag.canceled){
                optDiag.progressArea.append("\n"
                    + Globals.lang("Operation canceled.\n"));
                break;
            }
		}
		optDiag.progressArea.append("\n"
			+ Globals.lang("Finished writing XMP for %0 file (%1 skipped, %2 errors).", String
				.valueOf(entriesChanged), String.valueOf(skipped), String.valueOf(errors)));
		optDiag.done();
	}

	public void update() {
		if (!goOn)
			return;

		panel.output(Globals.lang("Finished writing XMP for %0 file (%1 skipped, %2 errors).",
			String.valueOf(entriesChanged), String.valueOf(skipped), String.valueOf(errors)));
	}

	class OptionsDialog extends JDialog {

		private static final long serialVersionUID = 7459164400811785958L;

		JButton okButton = new JButton(Globals.lang("Ok")), cancelButton = new JButton(Globals
			.lang("Cancel"));

		boolean canceled;

		JTextArea progressArea;

		public OptionsDialog(JFrame parent) {
			super(parent, Globals.lang("Writing XMP metadata for selected entries..."), false);
			okButton.setEnabled(false);

			okButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					dispose();
				}
			});

			AbstractAction cancel = new AbstractAction() {
				private static final long serialVersionUID = -338601477652815366L;

				public void actionPerformed(ActionEvent e) {
					canceled = true;
				}
			};
			cancelButton.addActionListener(cancel);

			InputMap im = cancelButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
			ActionMap am = cancelButton.getActionMap();
			im.put(Globals.prefs.getKey("Close dialog"), "close");
			am.put("close", cancel);

			progressArea = new JTextArea(15, 60);

			JScrollPane scrollPane = new JScrollPane(progressArea,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			Dimension d = progressArea.getPreferredSize();
			d.height += scrollPane.getHorizontalScrollBar().getHeight() + 15;
			d.width += scrollPane.getVerticalScrollBar().getWidth() + 15;
			
			panel.setSize(d);

			progressArea.setBackground(Color.WHITE);
			progressArea.setEditable(false);
			progressArea.setBorder(BorderFactory.createEmptyBorder(3, 3, 3,
				3));
			progressArea.setText("");

			JPanel panel = new JPanel();
			panel.setBorder(BorderFactory.createEmptyBorder(3, 2, 3, 2));
			panel.add(scrollPane);

			

			// progressArea.setPreferredSize(new Dimension(300, 300));

			ButtonBarBuilder bb = new ButtonBarBuilder();
			bb.addGlue();
			bb.addButton(okButton);
			bb.addRelatedGap();
			bb.addButton(cancelButton);
			bb.addGlue();
			JPanel bbPanel = bb.getPanel();
			bbPanel.setBorder(BorderFactory.createEmptyBorder(0, 3, 3, 3));
			getContentPane().add(panel, BorderLayout.CENTER);
			getContentPane().add(bbPanel, BorderLayout.SOUTH);

			pack();
			this.setResizable(false);

		}

		public void done() {
			okButton.setEnabled(true);
			cancelButton.setEnabled(false);
		}

		public void open() {
			progressArea.setText("");
			canceled = false;
			Util.placeDialog(optDiag, panel.frame());

			okButton.setEnabled(false);
			cancelButton.setEnabled(true);

			new FocusRequester(okButton);

			optDiag.setVisible(true);
		}
	}
}

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
package net.sf.jabref.external;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

import javax.swing.*;

import net.sf.jabref.*;
import net.sf.jabref.gui.*;
import net.sf.jabref.gui.keyboard.KeyBinds;
import net.sf.jabref.gui.util.FocusRequester;
import net.sf.jabref.gui.util.PositionWindow;
import net.sf.jabref.gui.worker.AbstractWorker;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibtexDatabase;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.logic.xmp.XMPUtil;

import com.jgoodies.forms.builder.ButtonBarBuilder;

/**
 *
 * This action goes through all selected entries in the BasePanel, and attempts
 * to write the XMP data to the external pdf.
 */
public class WriteXMPAction extends AbstractWorker {

    private final BasePanel panel;

    private BibtexEntry[] entries;

    private BibtexDatabase database;

    private OptionsDialog optDiag;

    private boolean goOn = true;

    private int skipped;
    private int entriesChanged;
    private int errors;


    public WriteXMPAction(BasePanel panel) {
        this.panel = panel;
    }

    @Override
    public void init() {

        database = panel.getDatabase();
        // Get entries and check if it makes sense to perform this operation
        entries = panel.getSelectedEntries();

        if (entries.length == 0) {

            Collection<BibtexEntry> var = database.getEntries();
            entries = var.toArray(new BibtexEntry[var.size()]);

            if (entries.length == 0) {

                JOptionPane.showMessageDialog(panel, Localization.lang("This operation requires at least one entry."),
                        Localization.lang("Write XMP-metadata"), JOptionPane.ERROR_MESSAGE);
                goOn = false;
                return;

            } else {

                int response = JOptionPane.showConfirmDialog(panel, Localization.lang("Write XMP-metadata for all PDFs in current database?"),
                        Localization.lang("Write XMP-metadata"), JOptionPane.YES_NO_CANCEL_OPTION,
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

        panel.output(Localization.lang("Writing XMP metadata..."));
    }

    @Override
    public void run() {

        if (!goOn) {
            return;
        }

        for (BibtexEntry entry : entries) {

            // Make a list of all PDFs linked from this entry:
            List<File> files = new ArrayList<>();

            // First check the (legacy) "pdf" field:
            String pdf = entry.getField("pdf");
            String[] dirs = panel.metaData().getFileDirectory("pdf");
            File f = FileUtil.expandFilename(pdf, dirs);
            if (f != null) {
                files.add(f);
            }

            // Then check the "file" field:
            dirs = panel.metaData().getFileDirectory(Globals.FILE_FIELD);
            String field = entry.getField(Globals.FILE_FIELD);
            if (field != null) {
                FileListTableModel tm = new FileListTableModel();
                tm.setContent(field);
                for (int j = 0; j < tm.getRowCount(); j++) {
                    FileListEntry flEntry = tm.getEntry(j);
                    if ((flEntry.getType() != null) && flEntry.getType().getName().toLowerCase().equals("pdf")) {
                        f = FileUtil.expandFilename(flEntry.getLink(), dirs);
                        if (f != null) {
                            files.add(f);
                        }
                    }
                }
            }

            optDiag.progressArea.append(entry.getCiteKey() + "\n");

            if (files.isEmpty()) {
                skipped++;
                optDiag.progressArea.append("  " + Localization.lang("Skipped - No PDF linked") + ".\n");
            } else {
                for (File file : files) {
                    if (!file.exists()) {
                        skipped++;
                        optDiag.progressArea.append("  " + Localization.lang("Skipped - PDF does not exist")
                                + ":\n");
                        optDiag.progressArea.append("    " + file.getPath() + "\n");

                    } else {
                        try {
                            XMPUtil.writeXMP(file, entry, database);
                            optDiag.progressArea.append("  " + Localization.lang("Ok") + ".\n");
                            entriesChanged++;
                        } catch (Exception e) {
                            optDiag.progressArea.append("  " + Localization.lang("Error while writing") + " '"
                                    + file.getPath() + "':\n");
                            optDiag.progressArea.append("    " + e.getLocalizedMessage() + "\n");
                            errors++;
                        }
                    }
                }
            }

            if (optDiag.canceled) {
                optDiag.progressArea.append("\n"
                        + Localization.lang("Operation canceled.\n"));
                break;
            }
        }
        optDiag.progressArea.append("\n"
                + Localization.lang("Finished writing XMP for %0 file (%1 skipped, %2 errors).", String
                .valueOf(entriesChanged), String.valueOf(skipped), String.valueOf(errors)));
        optDiag.done();
    }

    @Override
    public void update() {
        if (!goOn) {
            return;
        }

        panel.output(Localization.lang("Finished writing XMP for %0 file (%1 skipped, %2 errors).",
                String.valueOf(entriesChanged), String.valueOf(skipped), String.valueOf(errors)));
    }


    class OptionsDialog extends JDialog {
        final JButton okButton = new JButton(Localization.lang("Ok"));
        final JButton cancelButton = new JButton(
                Localization.lang("Cancel"));

        boolean canceled;

        final JTextArea progressArea;


        public OptionsDialog(JFrame parent) {
            super(parent, Localization.lang("Writing XMP metadata for selected entries..."), false);
            okButton.setEnabled(false);

            okButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    dispose();
                }
            });

            AbstractAction cancel = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    canceled = true;
                }
            };
            cancelButton.addActionListener(cancel);

            InputMap im = cancelButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            ActionMap am = cancelButton.getActionMap();
            im.put(Globals.prefs.getKey(KeyBinds.CLOSE_DIALOG), "close");
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

            JPanel tmpPanel = new JPanel();
            tmpPanel.setBorder(BorderFactory.createEmptyBorder(3, 2, 3, 2));
            tmpPanel.add(scrollPane);

            // progressArea.setPreferredSize(new Dimension(300, 300));

            ButtonBarBuilder bb = new ButtonBarBuilder();
            bb.addGlue();
            bb.addButton(okButton);
            bb.addRelatedGap();
            bb.addButton(cancelButton);
            bb.addGlue();
            JPanel bbPanel = bb.getPanel();
            bbPanel.setBorder(BorderFactory.createEmptyBorder(0, 3, 3, 3));
            getContentPane().add(tmpPanel, BorderLayout.CENTER);
            getContentPane().add(bbPanel, BorderLayout.SOUTH);

            pack();
            this.setResizable(false);

        }

        public void done() {
            okButton.setEnabled(true);
            cancelButton.setEnabled(false);
        }

        @SuppressWarnings("unused")
        public void open() {
            progressArea.setText("");
            canceled = false;
            PositionWindow.placeDialog(optDiag, panel.frame());

            okButton.setEnabled(false);
            cancelButton.setEnabled(true);

            new FocusRequester(okButton);

            optDiag.setVisible(true);
        }
    }
}

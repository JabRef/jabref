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
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.FileListEntry;
import net.sf.jabref.gui.FileListTableModel;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.gui.util.FocusRequester;
import net.sf.jabref.gui.worker.AbstractWorker;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.logic.xmp.XMPUtil;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;

import com.jgoodies.forms.builder.ButtonBarBuilder;

/**
 *
 * This action goes through all selected entries in the BasePanel, and attempts
 * to write the XMP data to the external pdf.
 */
public class WriteXMPAction extends AbstractWorker {

    private final BasePanel panel;

    private Collection<BibEntry> entries;

    private BibDatabase database;

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

        if (entries.isEmpty()) {

            entries = database.getEntries();

            if (entries.isEmpty()) {

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

        panel.output(Localization.lang("Writing XMP-metadata..."));
    }

    @Override
    public void run() {

        if (!goOn) {
            return;
        }

        for (BibEntry entry : entries) {

            // Make a list of all PDFs linked from this entry:
            List<File> files = new ArrayList<>();

            // First check the (legacy) "pdf" field:
            String pdf = entry.getField("pdf");
            List<String> dirs = panel.getBibDatabaseContext().getFileDirectory("pdf");
            FileUtil.expandFilename(pdf, dirs).ifPresent(files::add);

            // Then check the "file" field:
            dirs = panel.getBibDatabaseContext().getFileDirectory();
            if (entry.hasField(Globals.FILE_FIELD)) {
                FileListTableModel tm = new FileListTableModel();
                tm.setContent(entry.getField(Globals.FILE_FIELD));
                for (int j = 0; j < tm.getRowCount(); j++) {
                    FileListEntry flEntry = tm.getEntry(j);
                    if ((flEntry.type.isPresent()) && "pdf".equalsIgnoreCase(flEntry.type.get().getName())) {
                        FileUtil.expandFilename(flEntry.link, dirs).ifPresent(files::add);
                    }
                }
            }

            optDiag.getProgressArea().append(entry.getCiteKey() + "\n");

            if (files.isEmpty()) {
                skipped++;
                optDiag.getProgressArea().append("  " + Localization.lang("Skipped - No PDF linked") + ".\n");
            } else {
                for (File file : files) {
                    if (file.exists()) {
                        try {
                            XMPUtil.writeXMP(file, entry, database);
                            optDiag.getProgressArea().append("  " + Localization.lang("OK") + ".\n");
                            entriesChanged++;
                        } catch (Exception e) {
                            optDiag.getProgressArea().append(
                                    "  " + Localization.lang("Error while writing") + " '" + file.getPath() + "':\n");
                            optDiag.getProgressArea().append("    " + e.getLocalizedMessage() + "\n");
                            errors++;
                        }
                    } else {
                        skipped++;
                        optDiag.getProgressArea()
                                .append("  " + Localization.lang("Skipped - PDF does not exist") + ":\n");
                        optDiag.getProgressArea().append("    " + file.getPath() + "\n");
                    }
                }
            }

            if (optDiag.isCanceled()) {
                optDiag.getProgressArea().append("\n"
                        + Localization.lang("Operation canceled.") +"\n");
                break;
            }
        }
        optDiag.getProgressArea()
                .append("\n"
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

        private final JButton okButton = new JButton(Localization.lang("OK"));
        private final JButton cancelButton = new JButton(Localization.lang("Cancel"));

        private boolean canceled;

        private final JTextArea progressArea;


        public OptionsDialog(JFrame parent) {
            super(parent, Localization.lang("Writing XMP-metadata for selected entries..."), false);
            okButton.setEnabled(false);

            okButton.addActionListener(e -> dispose());

            AbstractAction cancel = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    canceled = true;
                }
            };
            cancelButton.addActionListener(cancel);

            InputMap im = cancelButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            ActionMap am = cancelButton.getActionMap();
            im.put(Globals.getKeyPrefs().getKey(KeyBinding.CLOSE_DIALOG), "close");
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

        public void open() {
            progressArea.setText("");
            canceled = false;
            optDiag.setLocationRelativeTo(panel.frame());

            okButton.setEnabled(false);
            cancelButton.setEnabled(true);

            new FocusRequester(okButton);

            optDiag.setVisible(true);
        }

        public boolean isCanceled() {
            return canceled;
        }

        public JTextArea getProgressArea() {
            return progressArea;
        }
    }
}

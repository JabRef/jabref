package org.jabref.gui.externalfiles;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.JabRefDialog;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.worker.AbstractWorker;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.xmp.XmpUtilWriter;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

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

                JOptionPane.showMessageDialog(panel,
                        Localization.lang("This operation requires one or more entries to be selected."),
                        Localization.lang("Write XMP-metadata"), JOptionPane.ERROR_MESSAGE);
                goOn = false;
                return;

            } else {

                int response = JOptionPane.showConfirmDialog(panel, Localization.lang("Write XMP-metadata for all PDFs in current library?"),
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
            List<Path> files = entry.getFiles().stream()
                    .filter(file -> file.getFileType().equalsIgnoreCase("pdf"))
                    .map(file -> file.findIn(panel.getBibDatabaseContext(), Globals.prefs.getFileDirectoryPreferences()))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());

            SwingUtilities.invokeLater(() -> optDiag.getProgressArea()
                    .append(entry.getCiteKeyOptional().orElse(Localization.lang("undefined")) + "\n"));

            if (files.isEmpty()) {
                skipped++;
                SwingUtilities.invokeLater(() -> optDiag.getProgressArea()
                        .append("  " + Localization.lang("Skipped - No PDF linked") + ".\n"));
            } else {
                for (Path file : files) {
                    if (Files.exists(file)) {
                        try {
                            XmpUtilWriter.writeXmp(file, entry, database, Globals.prefs.getXMPPreferences());
                            SwingUtilities.invokeLater(
                                    () -> optDiag.getProgressArea().append("  " + Localization.lang("OK") + ".\n"));
                            entriesChanged++;
                        } catch (Exception e) {
                            SwingUtilities.invokeLater(() -> {
                                optDiag.getProgressArea().append("  " + Localization.lang("Error while writing") + " '"
                                        + file.toString() + "':\n");
                                optDiag.getProgressArea().append("    " + e.getLocalizedMessage() + "\n");
                            });
                            errors++;
                        }
                    } else {
                        skipped++;
                        SwingUtilities.invokeLater(() -> {
                            optDiag.getProgressArea()
                                    .append("  " + Localization.lang("Skipped - PDF does not exist") + ":\n");
                            optDiag.getProgressArea().append("    " + file.toString() + "\n");
                        });
                    }
                }
            }

            if (optDiag.isCanceled()) {
                SwingUtilities.invokeLater(
                        () -> optDiag.getProgressArea().append("\n" + Localization.lang("Operation canceled.") + "\n"));
                break;
            }
        }
        SwingUtilities.invokeLater(() -> {
            optDiag.getProgressArea()
                .append("\n"
                + Localization.lang("Finished writing XMP for %0 file (%1 skipped, %2 errors).", String
                .valueOf(entriesChanged), String.valueOf(skipped), String.valueOf(errors)));
            optDiag.done();
        });
    }

    @Override
    public void update() {
        if (!goOn) {
            return;
        }

        panel.output(Localization.lang("Finished writing XMP for %0 file (%1 skipped, %2 errors).",
                String.valueOf(entriesChanged), String.valueOf(skipped), String.valueOf(errors)));
    }

    class OptionsDialog extends JabRefDialog {

        private final JButton okButton = new JButton(Localization.lang("OK"));
        private final JButton cancelButton = new JButton(Localization.lang("Cancel"));

        private boolean canceled;

        private final JTextArea progressArea;


        public OptionsDialog(JFrame parent) {
            super(parent, Localization.lang("Writing XMP-metadata for selected entries..."), false, OptionsDialog.class);
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
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
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

            okButton.requestFocus();

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

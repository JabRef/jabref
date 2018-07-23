package org.jabref.gui.actions;

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
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefDialog;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.xmp.XmpUtilWriter;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

import com.jgoodies.forms.builder.ButtonBarBuilder;

public class WriteXMPAction extends SimpleCommand {

    private final BasePanel basePanel;
    private OptionsDialog optionsDialog;

    private Collection<BibEntry> entries;

    private BibDatabase database;

    private boolean shouldContinue = true;

    private int skipped;
    private int entriesChanged;
    private int errors;
    private final DialogService dialogService;

    public WriteXMPAction(BasePanel basePanel) {
        this.basePanel = basePanel;
        dialogService = basePanel.frame().getDialogService();
    }

    @Override
    public void execute() {
        init();
        BackgroundTask.wrap(this::writeXMP)
                      .executeWith(Globals.TASK_EXECUTOR);
    }

    public void init() {
        database = basePanel.getDatabase();
        // Get entries and check if it makes sense to perform this operation
        entries = basePanel.getSelectedEntries();

        if (entries.isEmpty()) {

            entries = database.getEntries();

            if (entries.isEmpty()) {
                dialogService.showErrorDialogAndWait(
                        Localization.lang("Write XMP-metadata"),
                        Localization.lang("This operation requires one or more entries to be selected."));
                shouldContinue = false;
                return;

            } else {
                boolean confirm = dialogService.showConfirmationDialogAndWait(
                        Localization.lang("Write XMP-metadata"),
                        Localization.lang("Write XMP-metadata for all PDFs in current library?"));
                if (confirm) {
                    shouldContinue = false;
                    return;
                }
            }
        }

        errors = entriesChanged = skipped = 0;

        if (optionsDialog == null) {
            optionsDialog = new OptionsDialog();
        }
        optionsDialog.open();

        basePanel.output(Localization.lang("Writing XMP-metadata..."));
    }

    private void writeXMP() {
        if (!shouldContinue) {
            return;
        }

        for (BibEntry entry : entries) {

            // Make a list of all PDFs linked from this entry:
            List<Path> files = entry.getFiles().stream()
                                    .filter(file -> file.getFileType().equalsIgnoreCase("pdf"))
                                    .map(file -> file.findIn(basePanel.getBibDatabaseContext(), Globals.prefs.getFileDirectoryPreferences()))
                                    .filter(Optional::isPresent)
                                    .map(Optional::get)
                                    .collect(Collectors.toList());

            SwingUtilities.invokeLater(() -> optionsDialog.getProgressArea()
                                                          .append(entry.getCiteKeyOptional().orElse(Localization.lang("undefined")) + "\n"));

            if (files.isEmpty()) {
                skipped++;
                SwingUtilities.invokeLater(() -> optionsDialog.getProgressArea()
                                                              .append("  " + Localization.lang("Skipped - No PDF linked") + ".\n"));
            } else {
                for (Path file : files) {
                    if (Files.exists(file)) {
                        try {
                            XmpUtilWriter.writeXmp(file, entry, database, Globals.prefs.getXMPPreferences());
                            SwingUtilities.invokeLater(
                                    () -> optionsDialog.getProgressArea().append("  " + Localization.lang("OK") + ".\n"));
                            entriesChanged++;
                        } catch (Exception e) {
                            SwingUtilities.invokeLater(() -> {
                                optionsDialog.getProgressArea().append("  " + Localization.lang("Error while writing") + " '"
                                        + file.toString() + "':\n");
                                optionsDialog.getProgressArea().append("    " + e.getLocalizedMessage() + "\n");
                            });
                            errors++;
                        }
                    } else {
                        skipped++;
                        SwingUtilities.invokeLater(() -> {
                            optionsDialog.getProgressArea()
                                         .append("  " + Localization.lang("Skipped - PDF does not exist") + ":\n");
                            optionsDialog.getProgressArea().append("    " + file.toString() + "\n");
                        });
                    }
                }
            }

            if (optionsDialog.isCanceled()) {
                SwingUtilities.invokeLater(
                        () -> optionsDialog.getProgressArea().append("\n" + Localization.lang("Operation canceled.") + "\n"));
                break;
            }
        }
        SwingUtilities.invokeLater(() -> {
            optionsDialog.getProgressArea()
                         .append("\n"
                           + Localization.lang("Finished writing XMP for %0 file (%1 skipped, %2 errors).", String
                           .valueOf(entriesChanged), String.valueOf(skipped), String.valueOf(errors)));
            optionsDialog.done();
        });

        if (!shouldContinue) {
            return;
        }

        basePanel.output(Localization.lang("Finished writing XMP for %0 file (%1 skipped, %2 errors).",
                String.valueOf(entriesChanged), String.valueOf(skipped), String.valueOf(errors)));
    }

    class OptionsDialog extends JabRefDialog {

        private final JButton okButton = new JButton(Localization.lang("OK"));
        private final JButton cancelButton = new JButton(Localization.lang("Cancel"));

        private boolean isCancelled;

        private final JTextArea progressArea;

        public OptionsDialog() {
            super(Localization.lang("Writing XMP-metadata for selected entries..."), false, WriteXMPAction.OptionsDialog.class);
            okButton.setEnabled(false);

            okButton.addActionListener(e -> dispose());

            AbstractAction cancel = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    isCancelled = true;
                }
            };
            cancelButton.addActionListener(cancel);

            InputMap im = cancelButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            ActionMap am = cancelButton.getActionMap();
            im.put(Globals.getKeyPrefs().getKey(KeyBinding.CLOSE), "close");
            am.put("close", cancel);

            progressArea = new JTextArea(15, 60);

            JScrollPane scrollPane = new JScrollPane(progressArea,
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            Dimension d = progressArea.getPreferredSize();
            d.height += scrollPane.getHorizontalScrollBar().getHeight() + 15;
            d.width += scrollPane.getVerticalScrollBar().getWidth() + 15;

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
            isCancelled = false;

            okButton.setEnabled(false);
            cancelButton.setEnabled(true);

            okButton.requestFocus();

            optionsDialog.setVisible(true);
        }

        public boolean isCanceled() {
            return isCancelled;
        }

        public JTextArea getProgressArea() {
            return progressArea;
        }
    }
}

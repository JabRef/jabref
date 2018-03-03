package org.jabref.gui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.jabref.Globals;
import org.jabref.gui.importer.ImportInspectionDialog;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.logic.bibtex.DuplicateCheck;
import org.jabref.logic.bibtexkeypattern.BibtexKeyGenerator;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.EntryTypes;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BiblatexEntryTypes;
import org.jabref.model.entry.BibtexEntryTypes;
import org.jabref.model.entry.EntryType;
import org.jabref.model.entry.IEEETranEntryTypes;
import org.jabref.preferences.JabRefPreferences;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dialog that prompts the user to choose a type for an entry.
 * Returns null if canceled.
 */
public class EntryTypeDialog extends JabRefDialog implements ActionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntryTypeDialog.class);
    private static final int COLUMN = 3;
    private final JabRefFrame frame;
    private final CancelAction cancelAction = new CancelAction();
    private EntryType type;
    private SwingWorker<Optional<BibEntry>, Void> fetcherWorker = new FetcherWorker();
    private JButton generateButton;
    private JTextField idTextField;
    private JComboBox<String> comboBox;

    public EntryTypeDialog(JabRefFrame frame) {
        // modal dialog
        super(frame, true, EntryTypeDialog.class);

        this.frame = frame;

        setTitle(Localization.lang("Select entry type"));

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cancelAction.actionPerformed(null);
            }
        });

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(createCancelButtonBarPanel(), BorderLayout.SOUTH);
        getContentPane().add(createEntryGroupsPanel(), BorderLayout.CENTER);

        pack();
        setResizable(false);
    }

    private JPanel createEntryGroupsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        if (frame.getCurrentBasePanel().getBibDatabaseContext().isBiblatexMode()) {
            panel.add(createEntryGroupPanel("biblatex", BiblatexEntryTypes.ALL));

            List<EntryType> customTypes = EntryTypes.getAllCustomTypes(BibDatabaseMode.BIBLATEX);
            if (!customTypes.isEmpty()) {
                panel.add(createEntryGroupPanel(Localization.lang("Custom"), customTypes));
            }
        } else {
            panel.add(createEntryGroupPanel("BibTeX", BibtexEntryTypes.ALL));
            panel.add(createEntryGroupPanel("IEEETran", IEEETranEntryTypes.ALL));

            List<EntryType> customTypes = EntryTypes.getAllCustomTypes(BibDatabaseMode.BIBTEX);
            if (!customTypes.isEmpty()) {
                panel.add(createEntryGroupPanel(Localization.lang("Custom"), customTypes));
            }
        }
        panel.add(createIdFetcherPanel());

        return panel;
    }

    private JPanel createCancelButtonBarPanel() {
        JButton cancel = new JButton(Localization.lang("Cancel"));
        cancel.addActionListener(this);

        // Make ESC close dialog, equivalent to clicking Cancel.
        cancel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(Globals.getKeyPrefs().getKey(KeyBinding.CLOSE_DIALOG), "close");
        cancel.getActionMap().put("close", cancelAction);

        JPanel buttons = new JPanel();
        ButtonBarBuilder bb = new ButtonBarBuilder(buttons);
        bb.addGlue();
        bb.addButton(cancel);
        bb.addGlue();
        return buttons;
    }

    private JPanel createEntryGroupPanel(String groupTitle, Collection<? extends EntryType> entries) {
        JPanel panel = new JPanel();
        GridBagLayout bagLayout = new GridBagLayout();
        panel.setLayout(bagLayout);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(4, 4, 4, 4);
        // column count
        int col = 0;

        for (EntryType entryType : entries) {
            TypeButton entryButton = new TypeButton(entryType.getName(), entryType);
            entryButton.addActionListener(this);
            // Check if we should finish the row.
            col++;
            if (col == EntryTypeDialog.COLUMN) {
                col = 0;
                constraints.gridwidth = GridBagConstraints.REMAINDER;
            } else {
                constraints.gridwidth = 1;
            }
            bagLayout.setConstraints(entryButton, constraints);
            panel.add(entryButton);
        }
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), groupTitle));

        return panel;
    }

    private JPanel createIdFetcherPanel() {
        JLabel fetcherLabel = new JLabel(Localization.lang("ID type"));
        JLabel idLabel = new JLabel(Localization.lang("ID"));
        generateButton = new JButton(Localization.lang("Generate"));
        idTextField = new JTextField("");
        comboBox = new JComboBox<>();

        WebFetchers.getIdBasedFetchers(Globals.prefs.getImportFormatPreferences()).forEach(fetcher -> comboBox.addItem(fetcher.getName()));

        comboBox.setSelectedItem(Globals.prefs.get(JabRefPreferences.ID_ENTRY_GENERATOR));

        generateButton.addActionListener(action -> {
            fetcherWorker.execute();
        });

        comboBox.addActionListener(e -> {
            idTextField.requestFocus();
            idTextField.selectAll();
        });

        idTextField.addActionListener(event -> fetcherWorker.execute());

        JPanel jPanel = new JPanel();

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);

        GridBagLayout layout = new GridBagLayout();
        jPanel.setLayout(layout);

        constraints.fill = GridBagConstraints.HORIZONTAL;

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        jPanel.add(fetcherLabel, constraints);

        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.weightx = 2;
        jPanel.add(comboBox, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.weightx = 1;
        jPanel.add(idLabel, constraints);

        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.weightx = 2;
        jPanel.add(idTextField, constraints);

        constraints.gridy = 2;
        constraints.gridx = 0;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.NONE;
        jPanel.add(generateButton, constraints);

        jPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), Localization.lang("ID-based entry generator")));

        SwingUtilities.invokeLater(() -> idTextField.requestFocus());

        return jPanel;
    }

    private void stopFetching() {
        if (fetcherWorker.getState() == SwingWorker.StateValue.STARTED) {
            fetcherWorker.cancel(true);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof TypeButton) {
            type = ((TypeButton) e.getSource()).getType();
        }
        stopFetching();
        dispose();
    }

    public EntryType getChoice() {
        return type;
    }

    static class TypeButton extends JButton implements Comparable<TypeButton> {

        private final EntryType type;


        TypeButton(String label, EntryType type) {
            super(label);
            this.type = type;
        }

        @Override
        public int compareTo(TypeButton o) {
            return type.getName().compareTo(o.type.getName());
        }

        public EntryType getType() {
            return type;
        }
    }

    class CancelAction extends AbstractAction {
        public CancelAction() {
            super("Cancel");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            stopFetching();
            dispose();
        }
    }

    private class FetcherWorker extends SwingWorker<Optional<BibEntry>, Void> {
        private boolean fetcherException = false;
        private String fetcherExceptionMessage = "";
        private IdBasedFetcher fetcher = null;
        private String searchID = "";

        @Override
        protected Optional<BibEntry> doInBackground() throws Exception {
            Optional<BibEntry> bibEntry = Optional.empty();
            SwingUtilities.invokeLater(() -> {
                generateButton.setEnabled(false);
                generateButton.setText(Localization.lang("Searching..."));
            });

            Globals.prefs.put(JabRefPreferences.ID_ENTRY_GENERATOR,String.valueOf(comboBox.getSelectedItem()));
            searchID = idTextField.getText().trim();
            searchID = searchID.replaceAll(" ", "");
            fetcher = WebFetchers.getIdBasedFetchers(Globals.prefs.getImportFormatPreferences()).get(comboBox.getSelectedIndex());
            if (!searchID.isEmpty()) {
                try {
                    bibEntry = fetcher.performSearchById(searchID);
                } catch (FetcherException e) {
                    LOGGER.error(e.getMessage(), e);
                    fetcherException = true;
                    fetcherExceptionMessage = e.getMessage();
                }
            }
            return bibEntry;
        }

        @Override
        protected void done() {
            try {
                Optional<BibEntry> result = get();
                if (result.isPresent()) {
                    final BibEntry bibEntry = result.get();
                    if ((DuplicateCheck.containsDuplicate(frame.getCurrentBasePanel().getDatabase(), bibEntry, frame.getCurrentBasePanel().getBibDatabaseContext().getMode()).isPresent())) {
                		//If there are duplicates starts ImportInspectionDialog
                        final BasePanel panel = (BasePanel) frame.getTabbedPane().getSelectedComponent();

                        ImportInspectionDialog diag = new ImportInspectionDialog(frame, panel, Localization.lang("Import"), false);
                        diag.addEntry(bibEntry);
                        diag.entryListComplete();
                        diag.setLocationRelativeTo(frame);
                        diag.setVisible(true);
                        diag.toFront();
                    } else {
                		// Regenerate CiteKey of imported BibEntry
                        new BibtexKeyGenerator(frame.getCurrentBasePanel().getBibDatabaseContext(), Globals.prefs.getBibtexKeyPatternPreferences()).generateAndSetKey(bibEntry);
                        // Update Timestamps
                        if (Globals.prefs.getTimestampPreferences().includeCreatedTimestamp()) {
                            bibEntry.setField(Globals.prefs.getTimestampPreferences().getTimestampField(), Globals.prefs.getTimestampPreferences().now());
                        }
                        frame.getCurrentBasePanel().insertEntry(bibEntry);
                    }

                    dispose();
                } else if (searchID.trim().isEmpty()) {
                    JOptionPane.showMessageDialog(frame, Localization.lang("The given search ID was empty."), Localization.lang("Empty search ID"), JOptionPane.WARNING_MESSAGE);
                } else if (!fetcherException) {
                    JOptionPane.showMessageDialog(frame, Localization.lang("Fetcher '%0' did not find an entry for id '%1'.", fetcher.getName(), searchID) + "\n" + fetcherExceptionMessage, Localization.lang("No files found."), JOptionPane.WARNING_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(frame,
                            Localization.lang("Error while fetching from %0", fetcher.getName()) + "." + "\n" + fetcherExceptionMessage,
                            Localization.lang("Error"), JOptionPane.ERROR_MESSAGE);
                }
                fetcherWorker = new FetcherWorker();
                SwingUtilities.invokeLater(() -> {
                    idTextField.requestFocus();
                    idTextField.selectAll();
                    generateButton.setText(Localization.lang("Generate"));
                    generateButton.setEnabled(true);
                });
            } catch (ExecutionException | InterruptedException e) {
                LOGGER.error(String.format("Exception during fetching when using fetcher '%s' with entry id '%s'.", searchID, fetcher.getName()), e);
            }
        }
    }
}

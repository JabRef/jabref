package net.sf.jabref.gui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.Globals;
import net.sf.jabref.gui.importer.fetcher.EntryFetchers;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.logic.CustomEntryTypesManager;
import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.logic.importer.IdBasedFetcher;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.EntryTypes;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexEntryTypes;
import net.sf.jabref.model.entry.EntryType;
import net.sf.jabref.model.entry.IEEETranEntryTypes;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdesktop.swingx.VerticalLayout;

import static net.sf.jabref.gui.importer.fetcher.EntryFetchers.getIdFetchers;

/**
 * Dialog that prompts the user to choose a type for an entry.
 * Returns null if canceled.
 */
public class EntryTypeDialog extends JDialog implements ActionListener {

    private static final Log LOGGER = LogFactory.getLog(EntryTypeDialog.class);

    private EntryType type;
    private JabRefFrame frame;
    private static final int COLUMN = 3;
    private final boolean biblatexMode;

    private final CancelAction cancelAction = new CancelAction();
    private final BibDatabaseContext bibDatabaseContext;

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

    public EntryTypeDialog(JabRefFrame frame) {
        // modal dialog
        super(frame, true);

        this.frame = frame;

        bibDatabaseContext = frame.getCurrentBasePanel().getBibDatabaseContext();
        biblatexMode = bibDatabaseContext.isBiblatexMode();


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
        panel.setLayout(new VerticalLayout());

        if (biblatexMode) {
            panel.add(createEntryGroupPanel("BibLateX", EntryTypes.getAllValues(bibDatabaseContext.getMode())));
        } else {
            panel.add(createEntryGroupPanel("BibTeX", BibtexEntryTypes.ALL));
            panel.add(createEntryGroupPanel("IEEETran", IEEETranEntryTypes.ALL));

            if (!CustomEntryTypesManager.ALL.isEmpty()) {
                panel.add(createEntryGroupPanel(Localization.lang("Custom"), CustomEntryTypesManager.ALL));
            }

            panel.add(createIdFetcher("ID-based entry generator"));
        }

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

    private JPanel createEntryGroupPanel(String groupTitle, Collection<EntryType> entries) {
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

    private JPanel createIdFetcher(String groupTitle) {
        JButton generateButton = new JButton(Localization.lang("Generate"));
        JButton cancelButton = new JButton(Localization.lang("Cancel"));
        JTextField idTextField = new JTextField("");
        JComboBox<String> comboBox = new JComboBox<>();
        getIdFetchers().forEach(n -> comboBox.addItem(n.getName()));
        JLabel fetcherLabel = new JLabel("ID type"), idLabel = new JLabel("ID");

        SwingWorker<Optional<BibEntry>, Void> fetcherWorker = new SwingWorker<Optional<BibEntry>, Void>() {
            Optional<BibEntry> bibEntry = Optional.empty();
            IdBasedFetcher fetcher;
            String searchID;

            @Override
            protected Optional<BibEntry> doInBackground() throws Exception {
                searchID = idTextField.getText();
                if (!searchID.isEmpty()) {
                    fetcher = EntryFetchers.getIdFetchers().get(comboBox.getSelectedIndex());
                    try {
                        bibEntry = fetcher.performSearchById(searchID);
                        dispose();
                    } catch (FetcherException e) {
                        LOGGER.error("Error fetching from " + fetcher.getName(), e);
                        JOptionPane.showMessageDialog(null, Localization.lang("Error_while_fetching_from_%0", fetcher.getName()), Localization.lang("Error_while_fetching"), JOptionPane.ERROR_MESSAGE);
                    }
                }
                return bibEntry;
            }

            @Override
            protected void done() {
                if (isCancelled()) {
                    JOptionPane.showMessageDialog(null, "Fetching was canceled", "Canceled fetching", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }

                try {
                    Optional<BibEntry> result = get();
                    if (result.isPresent()) {
                        frame.getCurrentBasePanel().insertEntry(result.get());
                    } else {
                        JOptionPane.showMessageDialog(null, Localization.lang("No_entry_with_id_'%0'_for_fetcher_'%1'_was_found.", searchID, fetcher.getName()), Localization.lang("No_files_found."), JOptionPane.WARNING_MESSAGE);
                    }
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        generateButton.addActionListener(n -> fetcherWorker.execute());
        cancelButton.addActionListener(n -> fetcherWorker.cancel(true));

        JPanel jPanel = new JPanel();

        GridBagConstraints constraints = new GridBagConstraints();

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
        JPanel buttons = new JPanel();
        ButtonBarBuilder bb = new ButtonBarBuilder(buttons);
        bb.addButton(generateButton);
        bb.addButton(cancelButton);

        jPanel.add(buttons, constraints);
        jPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), groupTitle));


        return jPanel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof TypeButton) {
            type = ((TypeButton) e.getSource()).getType();
        }
        dispose();
    }

    public EntryType getChoice() {
        return type;
    }


    class CancelAction extends AbstractAction {
        public CancelAction() {
            super("Cancel");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            dispose();
        }
    }

}

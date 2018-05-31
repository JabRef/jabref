package org.jabref.gui.importer.fetcher;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.jabref.Globals;
import org.jabref.JabRefExecutorService;
import org.jabref.gui.IconTheme;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.SidePaneComponent;
import org.jabref.gui.SidePaneManager;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.importer.FetcherPreviewDialog;
import org.jabref.gui.importer.ImportInspectionDialog;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.OS;
import org.jabref.preferences.JabRefPreferences;

public class GeneralFetcher extends SidePaneComponent implements ActionListener {

    private final JTextField tf = new JTextField();

    private final CardLayout optionsCards = new CardLayout();
    private final JPanel optionsPanel = new JPanel(optionsCards);
    private final JPanel optPanel = new JPanel(new BorderLayout());

    private final ToggleAction action;
    private final JabRefFrame frame;
    private EntryFetcher activeFetcher;


    public GeneralFetcher(JabRefFrame frame, SidePaneManager sidePaneManager) {
        super(sidePaneManager, IconTheme.JabRefIcon.WWW.getSmallIcon(), Localization.lang("Web search"));
        this.frame = frame;
        List<EntryFetcher> fetchers = new EntryFetchers(Globals.journalAbbreviationLoader).getEntryFetchers();
        EntryFetcher[] fetcherArray = fetchers.toArray(new EntryFetcher[fetchers.size()]);
        Arrays.sort(fetcherArray, new EntryFetcherComparator());
        //JLabel[] choices = new JLabel[fetchers.size()];
        String[] choices = new String[fetcherArray.length];
        for (int i = 0; i < fetcherArray.length; i++) {
            choices[i] = fetcherArray[i].getTitle();
        }
        JComboBox<String> fetcherChoice = new JComboBox<>(choices);
        int defaultFetcher = Globals.prefs.getInt(JabRefPreferences.SELECTED_FETCHER_INDEX);
        if (defaultFetcher >= fetcherArray.length) {
            defaultFetcher = 0;
        }
        this.activeFetcher = fetcherArray[defaultFetcher];
        fetcherChoice.setSelectedIndex(defaultFetcher);
        if (this.activeFetcher.getOptionsPanel() != null) {
            optPanel.add(this.activeFetcher.getOptionsPanel(), BorderLayout.CENTER);
        }
        HelpAction help = new HelpAction(activeFetcher.getHelpPage());
        JButton helpBut = help.getHelpButton();
        helpBut.setEnabled(activeFetcher.getHelpPage() != null);

        fetcherChoice.addActionListener(actionEvent -> {
            activeFetcher = fetcherArray[fetcherChoice.getSelectedIndex()];
            Globals.prefs.putInt(JabRefPreferences.SELECTED_FETCHER_INDEX, fetcherChoice.getSelectedIndex());
            if (activeFetcher.getHelpPage() == null) {
                helpBut.setEnabled(false);
            } else {
                help.setHelpFile(activeFetcher.getHelpPage());
                helpBut.setEnabled(true);
            }
            optionsCards.show(optionsPanel, String.valueOf(fetcherChoice.getSelectedIndex()));
            optPanel.removeAll();
            if (activeFetcher.getOptionsPanel() != null) {
                optPanel.add(activeFetcher.getOptionsPanel(), BorderLayout.CENTER);
            }
            revalidate();
        });

        action = new ToggleAction(Localization.lang("Web search"),
                Localization.lang("Toggle web search interface"),
                Globals.getKeyPrefs().getKey(KeyBinding.WEB_SEARCH),
                IconTheme.JabRefIcon.WWW);

        helpBut.setMargin(new Insets(0, 0, 0, 0));
        tf.setPreferredSize(new Dimension(1, tf.getPreferredSize().height));
        if (OS.OS_X) {
            tf.putClientProperty("JTextField.variant", "search");
        }

        tf.setName("tf");
        // add action to reset-button. resets tf and requests focus
        JButton reset = new JButton(Localization.lang("Reset"));
        reset.addActionListener(event -> {
            tf.setText("");
            tf.requestFocus();
        });

        JPanel main = new JPanel();
        GridBagLayout gbl = new GridBagLayout();
        main.setLayout(gbl);
        GridBagConstraints con = new GridBagConstraints();
        con.fill = GridBagConstraints.BOTH;
        con.insets = new Insets(0, 0, 2, 0);
        con.gridwidth = GridBagConstraints.REMAINDER;
        con.weightx = 1;
        con.weighty = 1;
        con.insets = new Insets(1, 0, 1, 0);
        con.fill = GridBagConstraints.BOTH;
        gbl.setConstraints(fetcherChoice, con);
        main.add(fetcherChoice);
        con.insets = new Insets(0, 0, 0, 0);
        gbl.setConstraints(tf, con);
        main.add(tf);

        // Go Button
        con.weighty = 0;
        con.gridwidth = 1;
        JButton go = new JButton(Localization.lang("Fetch"));
        gbl.setConstraints(go, con);
        main.add(go);

        // Reset Button
        reset.setName("reset");
        gbl.setConstraints(reset, con);
        main.add(reset);

        // Help Button
        con.gridwidth = GridBagConstraints.REMAINDER;
        gbl.setConstraints(helpBut, con);
        main.add(helpBut);

        gbl.setConstraints(optPanel, con);
        main.add(optPanel);

        main.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        add(main, BorderLayout.CENTER);
        go.addActionListener(this);
        tf.addActionListener(this);
    }

    private JTextField getTextField() {
        return tf;
    }

    @Override
    public ToggleAction getToggleAction() {
        return action;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (tf.getText().trim().isEmpty()) {
            frame.output(Localization.lang("Please enter a search string"));
            return;
        }

        if (frame.getCurrentBasePanel() == null) {
            frame.output(Localization.lang("Please open or start a new library before searching"));
            return;
        }

        // We have two categories of fetchers. One category can show previews first and ask the
        // user which ones to download:
        if (activeFetcher instanceof PreviewEntryFetcher) {
            frame.output(Localization.lang("Searching..."));
            frame.setProgressBarIndeterminate(true);
            frame.setProgressBarVisible(true);
            final PreviewEntryFetcher pFetcher = (PreviewEntryFetcher) activeFetcher;
            final FetcherPreviewDialog dialog = new FetcherPreviewDialog(frame,
                    pFetcher.getWarningLimit(), pFetcher.getPreferredPreviewHeight());
            JabRefExecutorService.INSTANCE.execute(() -> {
                final boolean result = pFetcher.processQueryGetPreview(tf.getText().trim(), dialog, dialog);
                SwingUtilities.invokeLater(() -> {
                    frame.setProgressBarVisible(false);
                    frame.output("");
                    if (result) {
                        dialog.setLocationRelativeTo(frame);
                        dialog.setVisible(true);
                        if (dialog.isOkPressed()) {
                            final ImportInspectionDialog d2 = new ImportInspectionDialog(frame,
                                    frame.getCurrentBasePanel(), activeFetcher.getTitle(), false);
                            d2.addCallBack(activeFetcher);
                            d2.setLocationRelativeTo(frame);
                            d2.setVisible(true);
                            JabRefExecutorService.INSTANCE.execute(() -> {
                                pFetcher.getEntries(dialog.getSelection(), d2);
                                d2.entryListComplete();
                            });
                        }
                    }
                });
            });
        }

        // The other category downloads the entries first, then asks the user which ones to keep:
        else {
            final ImportInspectionDialog dialog = new ImportInspectionDialog(frame, frame.getCurrentBasePanel(),
                    activeFetcher.getTitle(), false);
            dialog.addCallBack(activeFetcher);
            dialog.setLocationRelativeTo(frame);
            dialog.setVisible(true);

            JabRefExecutorService.INSTANCE.execute(() -> {
                if (activeFetcher.processQuery(tf.getText().trim(), dialog, dialog)) {
                    dialog.entryListComplete();
                } else {
                    dialog.dispose();
                }
            });
        }
    }

    @Override
    public void grabFocus() {
        getTextField().grabFocus();
    }

    @Override
    public void componentClosing() {
        super.componentClosing();
        getToggleAction().setSelected(false);
        Globals.prefs.putBoolean(JabRefPreferences.WEB_SEARCH_VISIBLE, Boolean.FALSE);
    }

    @Override
    public void componentOpening() {
        super.componentOpening();
        Globals.prefs.putBoolean(JabRefPreferences.WEB_SEARCH_VISIBLE, Boolean.TRUE);
    }

    @Override
    public int getRescalingWeight() {
        return 0;
    }

    private static class EntryFetcherComparator implements Comparator<EntryFetcher> {

        @Override
        public int compare(EntryFetcher entryFetcher, EntryFetcher entryFetcher1) {
            return entryFetcher.getTitle().compareTo(entryFetcher1.getTitle());
        }
    }
}

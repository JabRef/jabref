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
package net.sf.jabref.gui.importer.fetcher;

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
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefExecutorService;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.SidePaneComponent;
import net.sf.jabref.gui.SidePaneManager;
import net.sf.jabref.gui.help.HelpAction;
import net.sf.jabref.gui.importer.FetcherPreviewDialog;
import net.sf.jabref.gui.importer.ImportInspectionDialog;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.gui.util.FocusRequester;
import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.logic.importer.IdBasedFetcher;
import net.sf.jabref.logic.importer.SearchBasedFetcher;
import net.sf.jabref.logic.importer.WebFetcher;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.OS;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.preferences.JabRefPreferences;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class GeneralFetcher extends SidePaneComponent implements ActionListener {

    private static final Log LOGGER = LogFactory.getLog(GeneralFetcher.class);

    private final JTextField tf = new JTextField();

    private final CardLayout optionsCards = new CardLayout();
    private final JPanel optionsPanel = new JPanel(optionsCards);
    private final JPanel optPanel = new JPanel(new BorderLayout());

    private final SidePaneManager sidePaneManager;
    private final Action action;
    private final JabRefFrame frame;
    private EntryFetcher activeFetcher;
    private WebFetcher webFetcher;


    public GeneralFetcher(SidePaneManager p0, JabRefFrame frame) {
        super(p0, IconTheme.JabRefIcon.WWW.getSmallIcon(), Localization.lang("Web search"));
        this.sidePaneManager = p0;
        this.frame = frame;

        List<EntryFetcher> fetchers = new EntryFetchers(Globals.journalAbbreviationLoader).getEntryFetchers();
        List<WebFetcher> webFetchers = new EntryFetchers(Globals.journalAbbreviationLoader).getWebFetchers();

        EntryFetcher[] fetcherArray = fetchers.toArray(new EntryFetcher[fetchers.size()]);
        WebFetcher[] searchBasedFetcherArray = webFetchers.toArray(new WebFetcher[webFetchers.size()]);

        Arrays.sort(fetcherArray, new EntryFetcherComparator());

        String[] choices = new String[fetcherArray.length + searchBasedFetcherArray.length];
        for (int i = 0; i < fetcherArray.length + searchBasedFetcherArray.length; i++) {
            if (i < fetcherArray.length) {
                choices[i] = fetcherArray[i].getTitle();
            } else {
                choices[i] = searchBasedFetcherArray[i - fetcherArray.length].getName();
            }
        }

        JComboBox<String> fetcherChoice = new JComboBox<>(choices);
        int defaultFetcher = Globals.prefs.getInt(JabRefPreferences.SELECTED_FETCHER_INDEX);
        if (defaultFetcher >= fetcherArray.length + searchBasedFetcherArray.length) {
            defaultFetcher = 0;
        }


        if (fetcherArray.length > defaultFetcher) {
            this.activeFetcher = fetcherArray[defaultFetcher];

            if (this.activeFetcher.getOptionsPanel() != null) {
                optPanel.add(this.activeFetcher.getOptionsPanel(), BorderLayout.CENTER);
            }
        } else if (defaultFetcher >= fetcherArray.length && defaultFetcher < fetcherArray.length + searchBasedFetcherArray.length) {
            this.webFetcher = searchBasedFetcherArray[defaultFetcher - fetcherArray.length];
        }


        fetcherChoice.setSelectedIndex(defaultFetcher);


        HelpAction help;

        if (fetcherArray.length < defaultFetcher) {
            help = new HelpAction(activeFetcher.getHelpPage());
        } else if (defaultFetcher >= fetcherArray.length && defaultFetcher < fetcherArray.length + searchBasedFetcherArray.length) {
            help = new HelpAction(webFetcher.getHelpPage());
        } else {
            help = new HelpAction(null);
        }


        JButton helpBut = help.getHelpButton();


        if (fetcherArray.length < defaultFetcher) {
            helpBut.setEnabled(activeFetcher.getHelpPage() != null);
        } else if (defaultFetcher >= fetcherArray.length && defaultFetcher < fetcherArray.length + searchBasedFetcherArray.length) {
            helpBut.setEnabled(webFetcher.getHelpPage() != null);
        }

        fetcherChoice.addActionListener(actionEvent -> {

            if (fetcherChoice.getSelectedIndex() < fetcherArray.length) {
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
            } else {
                webFetcher = searchBasedFetcherArray[fetcherChoice.getSelectedIndex() - fetcherArray.length];
                Globals.prefs.putInt(JabRefPreferences.SELECTED_FETCHER_INDEX, fetcherChoice.getSelectedIndex());
                if (webFetcher.getHelpPage() == null) {
                    helpBut.setEnabled(false);
                } else {
                    help.setHelpFile(webFetcher.getHelpPage());
                    helpBut.setEnabled(true);
                }
                optionsCards.show(optionsPanel, String.valueOf(fetcherChoice.getSelectedIndex() - fetcherArray.length));
                optPanel.removeAll();
            }

            revalidate();
        });

        action = new FetcherAction();


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
            new FocusRequester(tf);
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

    public Action getAction() {
        return action;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (tf.getText().trim().isEmpty()) {
            frame.output(Localization.lang("Please enter a search string"));
            return;
        }

        if (frame.getCurrentBasePanel() == null) {
            frame.output(Localization.lang("Please open or start a new database before searching"));
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

            if (webFetcher != null && activeFetcher == null) {
                if (webFetcher instanceof SearchBasedFetcher) {
                    SearchBasedFetcher searchBasedFetcher = (SearchBasedFetcher) webFetcher;
                    final ImportInspectionDialog dialog = new ImportInspectionDialog(frame, frame.getCurrentBasePanel(),
                            searchBasedFetcher.getName(), false);
                    dialog.setLocationRelativeTo(frame);
                    dialog.setVisible(true);

                    JabRefExecutorService.INSTANCE.execute(() -> {
                        try {
                            List<BibEntry> bibEntryList = searchBasedFetcher.performSearch(tf.getText().trim());

                            if (!bibEntryList.isEmpty()) {
                                dialog.addEntries(bibEntryList);
                                dialog.entryListComplete();
                            } else {
                                dialog.dispose();
                            }
                        } catch (FetcherException fe) {
                            LOGGER.error("Fail while fetching Entries");
                        }
                    });
                } else if (webFetcher instanceof IdBasedFetcher) {
                    IdBasedFetcher idBasedFetcher = (IdBasedFetcher) webFetcher;
                    final ImportInspectionDialog dialog = new ImportInspectionDialog(frame, frame.getCurrentBasePanel(),
                            idBasedFetcher.getName(), false);

                    dialog.setLocationRelativeTo(frame);
                    dialog.setVisible(true);

                    JabRefExecutorService.INSTANCE.execute(() -> {
                        try {
                            Optional<BibEntry> bibEntry = idBasedFetcher.performSearchById(tf.getText().trim());

                            if (bibEntry.isPresent()) {
                                dialog.addEntry(bibEntry.get());
                                dialog.entryListComplete();
                            } else {
                                dialog.dispose();
                            }
                        } catch (FetcherException fe) {
                            LOGGER.error("Fail while fetching Entries");
                        }
                    });
                }

            } else {
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
    }


    class FetcherAction extends AbstractAction {

        public FetcherAction() {
            super(Localization.lang("Web search"), IconTheme.JabRefIcon.WWW.getSmallIcon());
            //if ((activeFetcher.getKeyName() != null) && (activeFetcher.getKeyName().length() > 0))
            putValue(Action.ACCELERATOR_KEY, Globals.getKeyPrefs().getKey(KeyBinding.WEB_SEARCH));
            putValue(Action.LARGE_ICON_KEY, IconTheme.JabRefIcon.WWW.getIcon());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!sidePaneManager.hasComponent(GeneralFetcher.this.getTitle())) {
                sidePaneManager.register(GeneralFetcher.this.getTitle(), GeneralFetcher.this);
            }

            if (frame.getTabbedPane().getTabCount() > 0) {
                sidePaneManager.toggle(GeneralFetcher.this.getTitle());
                if (sidePaneManager.isComponentVisible(GeneralFetcher.this.getTitle())) {
                    new FocusRequester(getTextField());
                }
            }
        }
    }


    @Override
    public void componentClosing() {
        super.componentClosing();
        frame.setFetcherToggle(false);
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
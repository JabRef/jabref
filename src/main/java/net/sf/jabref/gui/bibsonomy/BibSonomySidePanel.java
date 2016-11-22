package net.sf.jabref.gui.bibsonomy;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.SystemColor;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.logic.l10n.Localization;

import org.bibsonomy.common.enums.GroupingEntity;
import net.sf.jabref.bibsonomy.BibSonomyProperties;
import net.sf.jabref.gui.actions.bibsonomy.RefreshTagListAction;
import net.sf.jabref.gui.actions.bibsonomy.SearchAction;
import net.sf.jabref.gui.actions.bibsonomy.ShowSettingsDialogAction;
import net.sf.jabref.gui.actions.bibsonomy.UpdateVisibilityAction;
import net.sf.jabref.gui.bibsonomy.listener.BibSonomyHyperLinkListener;
import net.sf.jabref.gui.bibsonomy.listener.VisibilityItemListener;

public class BibSonomySidePanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private JPanel controlsPanel = null;
    private JTextField searchTextField = null;
    private JComboBox<?> searchTypeComboBox = null;
    private JButton searchButton = null;
    private JPanel tagsPanel = null;
    private JButton tagsUpdateButton = null;
    private JPanel visibilityPanel = null;
    private JComboBox<GroupingComboBoxItem> visibilityComboBox = null;
    private JabRefFrame jabRefFrame;
    private JScrollPane tagListScrollPane = null;
    private JButton settingsButton = null;
    private JEditorPane tagCloudPanel = null;

    public BibSonomySidePanel(JabRefFrame jabRefFrame) {
        super();
        this.jabRefFrame = jabRefFrame;
        initialize();
    }

    private void initialize() {
        GridBagConstraints gridBagConstraints13 = new GridBagConstraints();
        gridBagConstraints13.gridx = 0;
        gridBagConstraints13.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints13.insets = new Insets(0, 3, 3, 3);
        gridBagConstraints13.gridy = 3;
        GridBagConstraints gridBagConstraints31 = new GridBagConstraints();
        gridBagConstraints31.gridx = 0;
        gridBagConstraints31.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints31.weightx = 1.0;
        gridBagConstraints31.insets = new Insets(0, 0, 3, 0);
        gridBagConstraints31.gridy = 2;
        GridBagConstraints gridBagConstraints21 = new GridBagConstraints();
        gridBagConstraints21.gridx = -1;
        gridBagConstraints21.gridy = -1;
        GridBagConstraints gridBagConstraints11 = new GridBagConstraints();
        gridBagConstraints11.gridx = 0;
        gridBagConstraints11.fill = GridBagConstraints.BOTH;
        gridBagConstraints11.weightx = 1.0;
        gridBagConstraints11.weighty = 1.0;
        gridBagConstraints11.insets = new Insets(0, 0, 3, 0);
        gridBagConstraints11.gridy = 1;
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(0, 0, 3, 0);
        gridBagConstraints.gridy = 0;
        this.setSize(300, 689);
        this.setLayout(new GridBagLayout());
        this.add(getControlsPanel(), gridBagConstraints);
        this.add(getTagsPanel(), gridBagConstraints11);
        this.add(getVisibilityPanel(), gridBagConstraints31);
        this.add(getSettingsButton(), gridBagConstraints13);

        searchTypeComboBox.addActionListener(action -> {
            searchTextField.requestFocus();
            searchTextField.selectAll();
        });

        visibilityComboBox.addActionListener(action -> tagsUpdateButton.requestFocus());

        searchTextField.addActionListener(action -> searchButton.doClick());
    }

    /**
     * This method initializes controlsPanel
     *
     * @return javax.swing.JPanel
     */
    private JPanel getControlsPanel() {
        if (controlsPanel == null) {
            GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
            gridBagConstraints1.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints1.gridy = 1;
            gridBagConstraints1.weightx = 1.0;
            gridBagConstraints1.insets = new Insets(0, 0, 0, 3);
            gridBagConstraints1.ipadx = 2;
            gridBagConstraints1.ipady = 2;
            gridBagConstraints1.gridx = 1;

            GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
            gridBagConstraints2.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints2.gridy = 0;
            gridBagConstraints2.weightx = 1.0;
            gridBagConstraints2.gridwidth = 2;
            gridBagConstraints2.insets = new Insets(0, 0, 3, 0);
            gridBagConstraints2.ipadx = 2;
            gridBagConstraints2.ipady = 2;
            gridBagConstraints2.gridx = 1;

            GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
            gridBagConstraints3.gridx = 2;
            gridBagConstraints3.fill = GridBagConstraints.NONE;
            gridBagConstraints3.weightx = 0.0;
            gridBagConstraints3.gridy = 1;

            controlsPanel = new JPanel();
            controlsPanel.setLayout(new GridBagLayout());
            controlsPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(null, Localization.lang("Search"), TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new Font("Dialog", Font.BOLD, 12), new Color(51, 51, 51)), BorderFactory.createEmptyBorder(3, 3, 3, 3)));
            controlsPanel.add(getSearchTextField(), gridBagConstraints2);
            controlsPanel.add(getSearchTypeComboBox(), gridBagConstraints1);
            controlsPanel.add(getSearchButton(), gridBagConstraints3);
        }
        return controlsPanel;
    }

    /**
     * This method initializes searchTextField
     *
     * @return javax.swing.JTextField
     */
    private JTextField getSearchTextField() {
        if (searchTextField == null) {
            searchTextField = new JTextField();
        }
        return searchTextField;
    }

    /**
     * This method initializes searchTypeComboBox
     *
     * @return javax.swing.JComboBox
     */
    private JComboBox<?> getSearchTypeComboBox() {

        if (searchTypeComboBox == null) {
            SearchTypeComboBoxItem[] items = new SearchTypeComboBoxItem[]{
                    new SearchTypeComboBoxItem(SearchType.FULL_TEXT, Localization.lang("Full text")),
                    new SearchTypeComboBoxItem(SearchType.TAGS, Localization.lang("Tag"))
            };
            searchTypeComboBox = new JComboBox<Object>(items);
        }
        return searchTypeComboBox;
    }

    /**
     * This method initializes searchButton
     *
     * @return javax.swing.JButton
     */
    private JButton getSearchButton() {
        if (searchButton == null) {
            searchButton = new JButton(
                    new SearchAction(jabRefFrame, getSearchTextField(),
                            getSearchTypeComboBox(),
                            getVisibilityComboBox())
            );
        }
        return searchButton;
    }

    /**
     * This method initializes tagsPanel
     *
     * @return javax.swing.JPanel
     */
    private JPanel getTagsPanel() {
        if (tagsPanel == null) {
            GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
            gridBagConstraints4.gridx = 0;
            gridBagConstraints4.fill = GridBagConstraints.BOTH;
            gridBagConstraints4.weightx = 1.0;
            gridBagConstraints4.weighty = 1.0;
            gridBagConstraints4.gridwidth = 3;
            gridBagConstraints4.gridy = 4;

            GridBagConstraints gridBagConstraints6 = new GridBagConstraints();
            gridBagConstraints6.gridx = 0;
            gridBagConstraints6.gridwidth = 4;
            gridBagConstraints6.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints6.insets = new Insets(3, 0, 0, 0);
            gridBagConstraints6.gridy = 5;

            GridBagConstraints gridBagConstraints12 = new GridBagConstraints();
            gridBagConstraints12.fill = GridBagConstraints.BOTH;
            gridBagConstraints12.gridy = 2;
            gridBagConstraints12.weightx = 1.0;
            gridBagConstraints12.weighty = 1.0;
            gridBagConstraints12.gridwidth = 4;
            gridBagConstraints12.gridx = 0;

            tagsPanel = new JPanel();
            tagsPanel.setLayout(new GridBagLayout());
            tagsPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(null, Localization.lang("Tag"), TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new Font("Dialog", Font.BOLD, 12), new Color(51, 51, 51)), BorderFactory.createEmptyBorder(3, 3, 3, 3)));
            tagsPanel.add(getTagsUpdateButton(), gridBagConstraints6);
            tagsPanel.add(getTagListScrollPane(), gridBagConstraints12);
        }
        return tagsPanel;
    }

    /**
     * This method initializes tagsUpdateButton
     *
     * @return javax.swing.JButton
     */
    private JButton getTagsUpdateButton() {
        if (tagsUpdateButton == null) {
            tagsUpdateButton = new JButton(new RefreshTagListAction(jabRefFrame, getTagCloudPanel(), getVisibilityComboBox()));
        }
        return tagsUpdateButton;
    }

    /**
     * This method initializes visibilityPanel
     *
     * @return javax.swing.JPanel
     */
    private JPanel getVisibilityPanel() {
        if (visibilityPanel == null) {
            GridBagConstraints gridBagConstraints7 = new GridBagConstraints();
            gridBagConstraints7.fill = GridBagConstraints.BOTH;
            gridBagConstraints7.gridy = 0;
            gridBagConstraints7.weightx = 1.0;
            gridBagConstraints7.ipadx = 2;
            gridBagConstraints7.ipady = 2;
            gridBagConstraints7.gridx = 0;
            visibilityPanel = new JPanel();
            visibilityPanel.setLayout(new GridBagLayout());
            visibilityPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(null, Localization.lang("Import posts from..."), TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new Font("Dialog", Font.BOLD, 12), new Color(51, 51, 51)), BorderFactory.createEmptyBorder(3, 3, 3, 3)));
            visibilityPanel.add(getVisibilityComboBox(), gridBagConstraints7);
        }
        return visibilityPanel;
    }

    /**
     * This method initializes visibilityComboBox
     *
     * @return javax.swing.JComboBox
     */
    private JComboBox<GroupingComboBoxItem> getVisibilityComboBox() {
        if (visibilityComboBox == null) {
            visibilityComboBox = new JComboBox<>();
            //Set Default Values
            List<GroupingComboBoxItem> defaultGroupings = new ArrayList<>();
            defaultGroupings.add(new GroupingComboBoxItem(GroupingEntity.ALL, "all users"));
            defaultGroupings.add(new GroupingComboBoxItem(GroupingEntity.USER, BibSonomyProperties.getUsername()));

            (new UpdateVisibilityAction(jabRefFrame, visibilityComboBox, defaultGroupings)).actionPerformed(null);
            visibilityComboBox.addItemListener(new VisibilityItemListener());
        }
        return visibilityComboBox;
    }

    /**
     * This method initializes tagListScrollPane
     *
     * @return javax.swing.JScrollPane
     */
    private JScrollPane getTagListScrollPane() {
        if (tagListScrollPane == null) {
            tagListScrollPane = new JScrollPane();
            tagListScrollPane.setBackground(SystemColor.control);
            tagListScrollPane.setBorder(null);
            tagListScrollPane.setViewportView(getTagCloudPanel());
        }
        return tagListScrollPane;
    }

    /**
     * This method initializes settingsButton
     *
     * @return javax.swing.JButton
     */
    private JButton getSettingsButton() {
        if (settingsButton == null) {
            settingsButton = new JButton(new ShowSettingsDialogAction(jabRefFrame));
        }
        return settingsButton;
    }

    /**
     * This method initializes tagCloudPanel
     *
     * @return javax.swing.JEditorPane
     */
    private JEditorPane getTagCloudPanel() {
        if (tagCloudPanel == null) {
            tagCloudPanel = new JEditorPane();
            tagCloudPanel.setContentType("text/html");
            tagCloudPanel.setEditable(false);
            tagCloudPanel.setFont(new Font("SansSerif", Font.PLAIN, 12));
            tagCloudPanel.setBackground(SystemColor.control);
            tagCloudPanel.addHyperlinkListener(new BibSonomyHyperLinkListener(jabRefFrame, getVisibilityComboBox()));

            if (BibSonomyProperties.getUpdateTagsOnStartUp()) {

                (new RefreshTagListAction(jabRefFrame, tagCloudPanel, getVisibilityComboBox())).actionPerformed(null);
            }
        }
        return tagCloudPanel;
    }

}  //  @jve:decl-index=0:visual-constraint="10,10"

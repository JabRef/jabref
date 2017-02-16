package org.jabref.gui.bibsonomy;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.TitledBorder;

import org.jabref.bibsonomy.BibSonomyGlobals;
import org.jabref.bibsonomy.BibSonomyProperties;
import org.jabref.bibsonomy.BibSonomyProperties;
import org.jabref.gui.IconTheme;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.bibsonomy.CloseBibSonomySettingsDialogByCancelAction;
import org.jabref.gui.actions.bibsonomy.CloseBibSonomySettingsDialogBySaveAction;
import org.jabref.gui.actions.bibsonomy.UpdateVisibilityAction;
import org.jabref.gui.actions.bibsonomy.CloseBibSonomySettingsDialogByCancelAction;
import org.jabref.gui.actions.bibsonomy.CloseBibSonomySettingsDialogBySaveAction;
import org.jabref.logic.l10n.Localization;

import org.bibsonomy.common.enums.GroupingEntity;
import org.bibsonomy.model.enums.Order;

public class BibSonomySettingsDialog extends JDialog {

    private final JabRefFrame jabRefFrame;

    private JPanel jContentPane = null;
    private JPanel buttonsPanel = null;
    private JButton saveButton = null;
    private JPanel whitespacePanel = null;
    private JButton cancelButton = null;
    private JTabbedPane settingsPane = null;
    private JPanel generalSettingsPanel = null;
    private JPanel credentialsPanel = null;
    private JLabel apiUrlLabel = null;
    private JTextField apiUrlTextField = null;
    private JLabel userNameLabel = null;
    private JTextField usernameTextField = null;
    private JLabel apiKeyLabel = null;
    private JTextField apiKeyTextField = null;
    private JCheckBox storeAPIKeyCheckBox = null;
    private JLabel apiKeyHintLabel = null;
    private JCheckBox ignoreOneTagWarningCheckBox = null;
    private JCheckBox updateTagsCheckBox = null;
    private JCheckBox uploadDocumentsCheckBox = null;
    private JCheckBox downloadDocumentsCheckBox = null;
    private JComboBox<GroupingComboBoxItem> defaultVisibilityComboBox = null;
    private JTextField extraFieldsTextField = null;
    private JPanel generalSettingsWhitespacePanel = null;
    private JSpinner numberOfPostsSpinner = null;
    private JCheckBox noWarningOnMorePostsCheckBox = null;
    private JSpinner tagCloudSizeSpinner = null;
    private JComboBox<?> tagCloudOrderComboBox = null;
    private JButton openDatabasePropertiesButton = null;

    public BibSonomySettingsDialog(JabRefFrame jabRefFrame) {
        super(jabRefFrame);
        this.jabRefFrame = jabRefFrame;
        if (BibSonomyProperties.getUsername().equals(BibSonomyGlobals.API_USERNAME))
            JOptionPane.showMessageDialog(this, Localization.lang("PLEASE NOTE: the current API access data is for testing purposes only.") + "\n"
                    + Localization.lang("You can up- and download entries.") + "\n"
                    + Localization.lang("After logging in you can see and_edit your entries on www.bibsonomy.org.") + "\n"
                    + Localization.lang("Do not use this account for_personal data, as it is accessible by everyone.") + "\n\n"
                    + Localization.lang("To obtain your own personal API key, visit") + "\n"
                    + "http://www.bibsonomy.org/help/doc/gettingaccess.html.", Localization.lang("Demo mode"), JOptionPane.INFORMATION_MESSAGE);
        initialize();
    }

    private void initialize() {
        this.setSize(677, 580);
        this.setResizable(true);
        this.setPreferredSize(new Dimension(700, 460));
        this.setModal(true);
        this.setMaximumSize(new Dimension(700, 460));
        this.setMinimumSize(new Dimension(700, 460));
        this.setContentPane(getJContentPane());
        this.setLocationRelativeTo(jabRefFrame);
    }

    private JPanel getJContentPane() {
        if (jContentPane == null) {
            BorderLayout borderLayout = new BorderLayout();
            borderLayout.setHgap(3);
            borderLayout.setVgap(3);
            jContentPane = new JPanel();
            jContentPane.setLayout(borderLayout);
            jContentPane.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
            jContentPane.add(getButtonsPanel(), BorderLayout.SOUTH);
            jContentPane.add(getSettingsPane(), BorderLayout.CENTER);
        }
        return jContentPane;
    }

    private JPanel getButtonsPanel() {
        if (buttonsPanel == null) {
            GridBagConstraints gridBagConstraints11 = new GridBagConstraints();
            gridBagConstraints11.gridx = 2;
            gridBagConstraints11.gridy = 0;
            GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
            gridBagConstraints1.gridx = 1;
            gridBagConstraints1.gridy = 0;
            gridBagConstraints1.insets = new Insets(0, 0, 0, 3);
            GridBagConstraints gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.gridy = 0;
            buttonsPanel = new JPanel();
            buttonsPanel.setLayout(new GridBagLayout());
            buttonsPanel.add(getWhitespacePanel(), gridBagConstraints);
            buttonsPanel.add(getSaveButton(), gridBagConstraints1);
            buttonsPanel.add(getCancelButton(), gridBagConstraints11);
        }
        return buttonsPanel;
    }

    private JButton getSaveButton() {
        if (saveButton == null) {
            saveButton = new JButton(new CloseBibSonomySettingsDialogBySaveAction(this, getApiUrlTextField(), getUsernameTextField(), getApiKeyTextField(), getStoreAPIKeyCheckBox(), getNumberOfPostsSpinner(), getTagCloudSizeSpinner(), getIgnoreOneTagWarningCheckBox(), getUpdateTagsCheckBox(), getUploadDocumentsCheckBox(), getDownloadDocumentsCheckBox(), getDefaultVisibilityComboBox(), getNoWarningOnMorePostsCheckBox(), getExtraFieldsTextField(), getTagCloudOrderComboBox()));
        }
        return saveButton;
    }

    private JPanel getWhitespacePanel() {
        if (whitespacePanel == null) {
            whitespacePanel = new JPanel();
            whitespacePanel.setLayout(new GridBagLayout());
        }
        return whitespacePanel;
    }

    private JButton getCancelButton() {
        if (cancelButton == null) {
            cancelButton = new JButton(new CloseBibSonomySettingsDialogByCancelAction(this));
        }
        return cancelButton;
    }

    private JTabbedPane getSettingsPane() {
        if (settingsPane == null) {
            settingsPane = new JTabbedPane();
            settingsPane.addTab(Localization.lang("General"), IconTheme.JabRefIcon.PREFERENCES.getIcon(), getGeneralSettingsPanel(), null);
        }
        return settingsPane;
    }

    private JPanel getGeneralSettingsPanel() {
        JLabel tagSizeLabel;
        JLabel tagCloudOrderLabel;
        JLabel tagCloudOrderHintLabel;
        JLabel openDatabasePropertiesLabel;
        JLabel numberOfPostsLabel;
        JLabel extraFieldsHintLabel;
        JLabel extraFieldsLabel;
        JLabel defaultVisibilityLabel;

        if (generalSettingsPanel == null) {

            tagCloudOrderHintLabel = new JLabel();
            tagCloudOrderHintLabel.setText(Localization.lang("Tag cloud ordering is not available when importing posts from all users"));
            tagCloudOrderHintLabel.setFont(new Font("Dialog", Font.ITALIC, 10));

            tagCloudOrderLabel = new JLabel();
            tagCloudOrderLabel.setText(Localization.lang("Tag cloud order"));

            tagSizeLabel = new JLabel();
            tagSizeLabel.setText(Localization.lang("Tag cloud size"));

            numberOfPostsLabel = new JLabel();
            numberOfPostsLabel.setText(Localization.lang("Number of Posts to fetch per Request"));


            extraFieldsHintLabel = new JLabel();
            extraFieldsHintLabel.setText(Localization.lang("You have to restart JabRef in order to see newly added or removed extra fields."));
            extraFieldsHintLabel.setFont(new Font("Dialog", Font.ITALIC, 10));

            extraFieldsLabel = new JLabel();
            extraFieldsLabel.setText(Localization.lang("Extra fields"));

            defaultVisibilityLabel = new JLabel();
            defaultVisibilityLabel.setText(Localization.lang("Default visibility"));

            apiKeyHintLabel = new JLabel();
            apiKeyHintLabel.setText(Localization.lang("You can find your API key at the settings page at http://www.bibsonomy.org"));
            apiKeyHintLabel.setFont(new Font("Dialog", Font.ITALIC, 10));

            apiKeyLabel = new JLabel();
            apiKeyLabel.setText(Localization.lang("API key"));

            userNameLabel = new JLabel();
            userNameLabel.setText(Localization.lang("Username"));
            apiUrlLabel = new JLabel();
            apiUrlLabel.setText(Localization.lang("Application URL"));

            //y = 0
            GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
            gridBagConstraints2.gridx = 1;
            gridBagConstraints2.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints2.weightx = 1.0;
            gridBagConstraints2.insets = new Insets(0, 0, 3, 0);
            gridBagConstraints2.gridwidth = 6;
            gridBagConstraints2.gridy = 0;

            //y = 1
            GridBagConstraints gridBagConstraints24 = new GridBagConstraints();
            gridBagConstraints24.gridy = 1;
            gridBagConstraints24.gridx = 1;
            gridBagConstraints24.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints24.insets = new Insets(0, 0, 3, 3);
            gridBagConstraints24.anchor = GridBagConstraints.WEST;

            GridBagConstraints gridBagConstraints25 = new GridBagConstraints();
            gridBagConstraints25.gridy = 1;
            gridBagConstraints25.gridx = 2;
            gridBagConstraints25.fill = GridBagConstraints.BOTH;
            gridBagConstraints25.weightx = 1.0;
            gridBagConstraints25.insets = new Insets(0, 0, 3, 0);
            gridBagConstraints25.gridwidth = 2;

            //y = 2
            GridBagConstraints gridBagConstraints16 = new GridBagConstraints();
            gridBagConstraints16.gridy = 2;
            gridBagConstraints16.gridx = 1;
            gridBagConstraints16.anchor = GridBagConstraints.WEST;
            gridBagConstraints16.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints16.insets = new Insets(0, 0, 3, 3);

            GridBagConstraints gridBagConstraints26 = new GridBagConstraints();
            gridBagConstraints26.gridy = 2;
            gridBagConstraints26.gridx = 2;
            gridBagConstraints26.fill = GridBagConstraints.BOTH;
            gridBagConstraints26.weightx = 1.0;
            gridBagConstraints26.insets = new Insets(0, 0, 3, 0);
            gridBagConstraints26.gridwidth = 2;

            //y = 3
            GridBagConstraints gridBagConstraints27 = new GridBagConstraints();
            gridBagConstraints27.gridy = 3;
            gridBagConstraints27.gridx = 2;
            gridBagConstraints27.anchor = GridBagConstraints.WEST;
            gridBagConstraints27.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints27.insets = new Insets(0, 0, 3, 0);

            //y = 4
            GridBagConstraints gridBagConstraints21 = new GridBagConstraints();
            gridBagConstraints21.gridy = 4;
            gridBagConstraints21.gridx = 1;
            gridBagConstraints21.anchor = GridBagConstraints.WEST;
            gridBagConstraints21.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints21.insets = new Insets(0, 0, 0, 3);

            GridBagConstraints gridBagConstraints22 = new GridBagConstraints();
            gridBagConstraints22.gridy = 4;
            gridBagConstraints22.gridx = 2;
            gridBagConstraints22.fill = GridBagConstraints.BOTH;
            gridBagConstraints22.weightx = 1.0;
            gridBagConstraints22.gridwidth = 2;
            gridBagConstraints22.ipadx = 2;
            gridBagConstraints22.ipady = 2;
            gridBagConstraints22.insets = new Insets(0, 0, 3, 0);

            //y = 5
            GridBagConstraints gridBagConstraints23 = new GridBagConstraints();
            gridBagConstraints23.gridy = 5;
            gridBagConstraints23.gridx = 2;
            gridBagConstraints23.anchor = GridBagConstraints.WEST;
            gridBagConstraints23.insets = new Insets(0, 0, 3, 0);
            gridBagConstraints23.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints23.gridwidth = 4;

            //y = 6
            GridBagConstraints gridBagConstraints9 = new GridBagConstraints();
            gridBagConstraints9.gridy = 6;
            gridBagConstraints9.gridx = 2;
            gridBagConstraints9.anchor = GridBagConstraints.WEST;
            gridBagConstraints9.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints9.weightx = 1.0;
            gridBagConstraints9.insets = new Insets(0, 0, 3, 0);
            gridBagConstraints9.gridwidth = 7;

            //y = 8
            GridBagConstraints gridBagConstraints10 = new GridBagConstraints();
            gridBagConstraints10.gridy = 8;
            gridBagConstraints10.gridx = 2;
            gridBagConstraints10.anchor = GridBagConstraints.WEST;
            gridBagConstraints10.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints10.weightx = 1.0;
            gridBagConstraints10.insets = new Insets(0, 0, 3, 0);
            gridBagConstraints10.gridwidth = 5;

            //y = 9
            GridBagConstraints gridBagConstraints12 = new GridBagConstraints();
            gridBagConstraints12.gridy = 9;
            gridBagConstraints12.gridx = 2;
            gridBagConstraints12.anchor = GridBagConstraints.WEST;
            gridBagConstraints12.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints12.weightx = 1.0;
            gridBagConstraints12.insets = new Insets(0, 0, 3, 0);
            gridBagConstraints12.gridwidth = 5;


            //y = 10
            GridBagConstraints gridBagConstraints13 = new GridBagConstraints();
            gridBagConstraints13.gridy = 10;
            gridBagConstraints13.gridx = 2;
            gridBagConstraints13.anchor = GridBagConstraints.WEST;
            gridBagConstraints13.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints13.insets = new Insets(0, 0, 3, 0);
            gridBagConstraints13.gridwidth = 5;


            //y = 14
            GridBagConstraints gridBagConstraints31 = new GridBagConstraints();
            gridBagConstraints31.gridy = 14;
            gridBagConstraints31.gridx = 1;
            gridBagConstraints31.anchor = GridBagConstraints.WEST;
            gridBagConstraints31.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints31.weightx = 1.0;
            gridBagConstraints31.insets = new Insets(0, 0, 3, 0);
            gridBagConstraints31.gridwidth = 1;

            GridBagConstraints gridBagConstraints32 = new GridBagConstraints();
            gridBagConstraints32.gridy = 14;
            gridBagConstraints32.gridx = 2;
            gridBagConstraints32.fill = GridBagConstraints.BOTH;
            gridBagConstraints32.gridwidth = 1;
            gridBagConstraints32.weightx = 1;
            gridBagConstraints32.insets = new Insets(0, 0, 3, 0);
            gridBagConstraints32.gridwidth = 1;

            //y = 15
            GridBagConstraints gridBagConstraints14 = new GridBagConstraints();
            gridBagConstraints14.gridy = 15;
            gridBagConstraints14.gridx = 1;
            gridBagConstraints14.anchor = GridBagConstraints.WEST;
            gridBagConstraints14.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints14.weightx = 0.1;
            gridBagConstraints14.insets = new Insets(0, 0, 0, 3);

            GridBagConstraints gridBagConstraints15 = new GridBagConstraints();
            gridBagConstraints15.gridy = 15;
            gridBagConstraints15.gridx = 2;
            gridBagConstraints15.fill = GridBagConstraints.BOTH;
            gridBagConstraints15.weightx = 1.0;
            gridBagConstraints15.insets = new Insets(0, 0, 3, 0);
            gridBagConstraints15.gridwidth = 5;

            //y = 16
            GridBagConstraints gridBagConstraints17 = new GridBagConstraints();
            gridBagConstraints17.gridy = 16;
            gridBagConstraints17.gridx = 1;
            gridBagConstraints17.anchor = GridBagConstraints.WEST;
            gridBagConstraints17.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints17.weightx = 0.1;
            gridBagConstraints17.insets = new Insets(0, 0, 0, 3);

            GridBagConstraints gridBagConstraints18 = new GridBagConstraints();
            gridBagConstraints18.gridy = 16;
            gridBagConstraints18.gridx = 2;
            gridBagConstraints18.fill = GridBagConstraints.BOTH;
            gridBagConstraints18.weightx = 1.0;
            gridBagConstraints18.ipadx = 2;
            gridBagConstraints18.ipady = 2;
            gridBagConstraints18.insets = new Insets(0, 0, 3, 0);
            gridBagConstraints18.gridwidth = 5;

            //y = 17
            GridBagConstraints gridBagConstraints19 = new GridBagConstraints();
            gridBagConstraints19.gridy = 17;
            gridBagConstraints19.gridx = 2;
            gridBagConstraints19.anchor = GridBagConstraints.WEST;
            gridBagConstraints19.insets = new Insets(0, 0, 3, 0);
            gridBagConstraints19.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints19.gridwidth = 4;

            //y = 19
            GridBagConstraints gridBagConstraints20 = new GridBagConstraints();
            gridBagConstraints20.gridy = 19;
            gridBagConstraints20.gridx = 1;
            gridBagConstraints20.fill = GridBagConstraints.BOTH;
            gridBagConstraints20.weightx = 1.0;
            gridBagConstraints20.weighty = 1.0;
            gridBagConstraints20.gridwidth = 8;

            generalSettingsPanel = new JPanel();
            generalSettingsPanel.setLayout(new GridBagLayout());
            generalSettingsPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
            generalSettingsPanel.add(getCredentialsPanel(), gridBagConstraints2);
            generalSettingsPanel.add(getIgnoreOneTagWarningCheckBox(), gridBagConstraints9);
            generalSettingsPanel.add(getUpdateTagsCheckBox(), gridBagConstraints10);
            generalSettingsPanel.add(getUploadDocumentsCheckBox(), gridBagConstraints12);
            generalSettingsPanel.add(getDownloadDocumentsCheckBox(), gridBagConstraints13);
            generalSettingsPanel.add(defaultVisibilityLabel, gridBagConstraints14);
            generalSettingsPanel.add(getDefaultVisibilityComboBox(), gridBagConstraints15);
            generalSettingsPanel.add(extraFieldsLabel, gridBagConstraints17);
            generalSettingsPanel.add(getExtraFieldsTextField(), gridBagConstraints18);
            generalSettingsPanel.add(extraFieldsHintLabel, gridBagConstraints19);
            generalSettingsPanel.add(getGeneralSettingsWhitespacePanel(), gridBagConstraints20);
            generalSettingsPanel.add(numberOfPostsLabel, gridBagConstraints21);
            generalSettingsPanel.add(getNumberOfPostsSpinner(), gridBagConstraints22);
            generalSettingsPanel.add(getNoWarningOnMorePostsCheckBox(), gridBagConstraints23);
            generalSettingsPanel.add(tagSizeLabel, gridBagConstraints24);
            generalSettingsPanel.add(getTagCloudSizeSpinner(), gridBagConstraints25);
            generalSettingsPanel.add(tagCloudOrderLabel, gridBagConstraints16);
            generalSettingsPanel.add(getTagCloudOrderComboBox(), gridBagConstraints26);
            generalSettingsPanel.add(tagCloudOrderHintLabel, gridBagConstraints27);
        }
        return generalSettingsPanel;
    }

    private JPanel getCredentialsPanel() {
        JLabel changingCredentialsHintLabel;

        if (credentialsPanel != null) {
            return credentialsPanel;
        }

        credentialsPanel = new JPanel();
        credentialsPanel.setLayout(new GridBagLayout());
        credentialsPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(null, Localization.lang("Credentials"), TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new Font("Dialog", Font.BOLD, 12), new Color(51, 51, 51)), BorderFactory.createEmptyBorder(3, 3, 3, 3)));

        final GridBagConstraints leftConstr = new GridBagConstraints();
        leftConstr.anchor = GridBagConstraints.WEST;
        leftConstr.insets = new Insets(0, 0, 3, 3);
        leftConstr.gridx = 0;
        leftConstr.gridy = 0;
        leftConstr.weightx = 0.1;
        //leftConstr.fill = GridBagConstraints.HORIZONTAL;

        final GridBagConstraints rightConstr = new GridBagConstraints();
        rightConstr.anchor = GridBagConstraints.WEST;
        rightConstr.ipadx = 2;
        rightConstr.ipady = 2;
        rightConstr.gridx = 1;
        rightConstr.gridy = 0;
        rightConstr.weightx = 0.9;
        rightConstr.fill = GridBagConstraints.HORIZONTAL;

        credentialsPanel.add(apiUrlLabel, leftConstr);
        credentialsPanel.add(getApiUrlTextField(), rightConstr);
        leftConstr.gridy++;
        rightConstr.gridy++;

        credentialsPanel.add(userNameLabel, leftConstr);
        credentialsPanel.add(getUsernameTextField(), rightConstr);
        leftConstr.gridy++;
        rightConstr.gridy++;

        credentialsPanel.add(apiKeyLabel, leftConstr);
        credentialsPanel.add(getApiKeyTextField(), rightConstr);
        leftConstr.gridy++;
        rightConstr.gridy++;

        rightConstr.ipadx = 0;
        rightConstr.ipady = 0;
        credentialsPanel.add(getStoreAPIKeyCheckBox(), rightConstr);
        rightConstr.gridy++;

        credentialsPanel.add(apiKeyHintLabel, rightConstr);
        rightConstr.gridy++;

        changingCredentialsHintLabel = new JLabel();
        changingCredentialsHintLabel.setText(Localization.lang("Don't forget to hit the refresh button after changing credentials!"));
        changingCredentialsHintLabel.setFont(new Font("Dialog", Font.ITALIC, 10));
        credentialsPanel.add(changingCredentialsHintLabel, rightConstr);

        return credentialsPanel;
    }

    private JTextField getUsernameTextField() {
        if (usernameTextField == null) {
            usernameTextField = new JTextField(BibSonomyProperties.getUsername());
        }
        return usernameTextField;
    }

    private JTextField getApiKeyTextField() {
        if (apiKeyTextField == null) {
            apiKeyTextField = new JTextField(BibSonomyProperties.getApiKey());
        }
        return apiKeyTextField;
    }


    private JTextField getApiUrlTextField() {
        if (apiUrlTextField == null) {
            apiUrlTextField = new JTextField(BibSonomyProperties.getApiUrl());
        }
        return apiUrlTextField;
    }

    private JCheckBox getStoreAPIKeyCheckBox() {
        if (storeAPIKeyCheckBox == null) {
            storeAPIKeyCheckBox = new JCheckBox();
            storeAPIKeyCheckBox.setText(Localization.lang("Store API key"));
            storeAPIKeyCheckBox.setSelected(BibSonomyProperties.getStoreApiKey());
        }
        return storeAPIKeyCheckBox;
    }

    private JCheckBox getIgnoreOneTagWarningCheckBox() {
        if (ignoreOneTagWarningCheckBox == null) {
            ignoreOneTagWarningCheckBox = new JCheckBox();
            ignoreOneTagWarningCheckBox.setText(Localization.lang("Do not warn me, if a post has no tags assigned"));
            ignoreOneTagWarningCheckBox.setSelected(BibSonomyProperties.ignoreNoTagsAssigned());
        }
        return ignoreOneTagWarningCheckBox;
    }

    private JCheckBox getUpdateTagsCheckBox() {
        if (updateTagsCheckBox == null) {
            updateTagsCheckBox = new JCheckBox();
            updateTagsCheckBox.setText(Localization.lang("Update tags on startup"));
            updateTagsCheckBox.setSelected(BibSonomyProperties.getUpdateTagsOnStartUp());
        }
        return updateTagsCheckBox;
    }

    private JCheckBox getUploadDocumentsCheckBox() {
        if (uploadDocumentsCheckBox == null) {
            uploadDocumentsCheckBox = new JCheckBox();
            uploadDocumentsCheckBox.setText(Localization.lang("Upload documents on export"));
            uploadDocumentsCheckBox.setSelected(BibSonomyProperties.getUploadDocumentsOnExport());
        }
        return uploadDocumentsCheckBox;
    }

    private JCheckBox getDownloadDocumentsCheckBox() {
        if (downloadDocumentsCheckBox == null) {
            downloadDocumentsCheckBox = new JCheckBox();
            downloadDocumentsCheckBox.setText(Localization.lang("Download documents on import"));
            downloadDocumentsCheckBox.setSelected(BibSonomyProperties.getDownloadDocumentsOnImport());
        }
        return downloadDocumentsCheckBox;
    }

    private JComboBox<GroupingComboBoxItem> getDefaultVisibilityComboBox() {
        if (defaultVisibilityComboBox == null) {

            List<GroupingComboBoxItem> items = new LinkedList<>();
            items.add(new GroupingComboBoxItem(GroupingEntity.ALL, "Public"));
            items.add(new GroupingComboBoxItem(GroupingEntity.USER, "Private"));

            defaultVisibilityComboBox = new JComboBox<>();
            (new UpdateVisibilityAction(jabRefFrame, defaultVisibilityComboBox, items)).actionPerformed(null);

            //Set selected Value
            int itemCount = defaultVisibilityComboBox.getItemCount();
            for (int i = 0; i < itemCount; i++) {
                if ((defaultVisibilityComboBox.getItemAt(i)).getValue().equals(BibSonomyProperties.getDefaultVisibilty())) {
                    defaultVisibilityComboBox.setSelectedItem(defaultVisibilityComboBox.getItemAt(i));
                }
            }
        }
        return defaultVisibilityComboBox;
    }

    private JTextField getExtraFieldsTextField() {
        if (extraFieldsTextField == null) {
            extraFieldsTextField = new JTextField();
            extraFieldsTextField.setText(BibSonomyProperties.getExtraTabFields());
        }
        return extraFieldsTextField;
    }

    private JPanel getGeneralSettingsWhitespacePanel() {
        if (generalSettingsWhitespacePanel == null) {
            generalSettingsWhitespacePanel = new JPanel();
            generalSettingsWhitespacePanel.setLayout(new GridBagLayout());
        }
        return generalSettingsWhitespacePanel;
    }

    private JSpinner getNumberOfPostsSpinner() {
        if (numberOfPostsSpinner == null) {
            numberOfPostsSpinner = new JSpinner(new SpinnerNumberModel(BibSonomyProperties.getNumberOfPostsPerRequest(), 1, 500, 1));
        }
        return numberOfPostsSpinner;
    }

    private JCheckBox getNoWarningOnMorePostsCheckBox() {
        if (noWarningOnMorePostsCheckBox == null) {
            noWarningOnMorePostsCheckBox = new JCheckBox();
            noWarningOnMorePostsCheckBox.setText(Localization.lang("Do not warn me, if more posts are available"));
            noWarningOnMorePostsCheckBox.setSelected(BibSonomyProperties.getIgnoreMorePostsWarning());
        }
        return noWarningOnMorePostsCheckBox;
    }

    private JSpinner getTagCloudSizeSpinner() {

        if (tagCloudSizeSpinner == null) {

            tagCloudSizeSpinner = new JSpinner(new SpinnerNumberModel(BibSonomyProperties.getTagCloudSize(), 20, 1000, 1));
        }

        return tagCloudSizeSpinner;
    }

    private JComboBox<?> getTagCloudOrderComboBox() {
        if (tagCloudOrderComboBox == null) {

            OrderComboBoxItem[] items = new OrderComboBoxItem[]{
                    new OrderComboBoxItem(Order.FREQUENCY, Localization.lang("Frequency")),
                    new OrderComboBoxItem(Order.ALPH, Localization.lang("Alphabethical")),
                    new OrderComboBoxItem(Order.FOLKRANK, Localization.lang("FolkRank")),
                    new OrderComboBoxItem(Order.ADDED, Localization.lang("Date"))
            };

            tagCloudOrderComboBox = new JComboBox<Object>(items);

            int itemCount = tagCloudOrderComboBox.getItemCount();
            for (int i = 0; i < itemCount; i++) {
                if (((OrderComboBoxItem) tagCloudOrderComboBox.getItemAt(i)).getKey() == BibSonomyProperties.getTagCloudOrder())
                    tagCloudOrderComboBox.setSelectedItem(tagCloudOrderComboBox.getItemAt(i));
            }
        }
        return tagCloudOrderComboBox;
    }

}

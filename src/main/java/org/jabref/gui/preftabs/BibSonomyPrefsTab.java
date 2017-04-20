package org.jabref.gui.preftabs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.TitledBorder;

import org.bibsonomy.common.enums.GroupingEntity;
import org.bibsonomy.model.enums.Order;
import org.jabref.bibsonomy.BibSonomyGlobals;
import org.jabref.bibsonomy.BibSonomyProperties;
import org.jabref.gui.bibsonomy.GroupingComboBoxItem;
import org.jabref.gui.preftabs.support.OrderComboBoxItem;
import org.jabref.gui.worker.bibsonomy.UpdateVisibilityWorker;
import org.jabref.logic.l10n.Localization;

public class BibSonomyPrefsTab extends JPanel implements PrefsTab {

    private JPanel generalSettingsPanel;
    private JPanel credentialsPanel;
    private JLabel apiUrlLabel;
    private JTextField apiUrlTextField;
    private JLabel userNameLabel;
    private JTextField usernameTextField;
    private JLabel apiKeyLabel;
    private JTextField apiKeyTextField;
    private JCheckBox storeAPIKeyCheckBox;
    private JLabel apiKeyHintLabel;
    private JCheckBox ignoreOneTagWarningCheckBox;
    private JCheckBox updateTagsCheckBox;
    private JCheckBox uploadDocumentsCheckBox;
    private JCheckBox downloadDocumentsCheckBox;
    private JComboBox<GroupingComboBoxItem> defaultVisibilityComboBox;
    private JPanel generalSettingsWhitespacePanel;
    private JSpinner numberOfPostsSpinner;
    private JCheckBox noWarningOnMorePostsCheckBox;
    private JSpinner tagCloudSizeSpinner;
    private JComboBox<?> tagCloudOrderComboBox;


    public BibSonomyPrefsTab() {
        setLayout(new BorderLayout());
        JPanel pan = this.getGeneralSettingsPanel();
        pan.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(pan, BorderLayout.CENTER);
    }

    @Override
    public void setValues() {
        // automatically done by the getter methods

        // display warning in the case of default values
        if (BibSonomyProperties.getUsername().equals(BibSonomyGlobals.API_USERNAME))
            JOptionPane.showMessageDialog(this,
                    Localization.lang("PLEASE NOTE: the current API access data is for testing purposes only.\\nYou can upload and download entries.\\nAfter logging in you can see and edit your entries on www.bibsonomy.org.\\nDo not use this account for personal data, as it is accessible by everyone.\\nTo obtain your own personal API key, visit %0.", "http://www.bibsonomy.org/help/doc/gettingaccess.html").replaceAll("\\\\n", "<br>"),
                    Localization.lang("Demo mode"),
                    JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void storeSettings() {
        BibSonomyProperties.setApiUrl(getApiUrlTextField().getText());
        BibSonomyProperties.setUsername(getUsernameTextField().getText());
        BibSonomyProperties.setApiKey(getApiKeyTextField().getText());
        BibSonomyProperties.setStoreApiKey(getStoreAPIKeyCheckBox().isSelected());
        BibSonomyProperties.setNumberOfPostsPerRequest((Integer) getNumberOfPostsSpinner().getValue());
        BibSonomyProperties.setTagCloudSize((Integer) getTagCloudSizeSpinner().getValue());
        BibSonomyProperties.setIgnoreNoTagsAssigned(getIgnoreOneTagWarningCheckBox().isSelected());
        BibSonomyProperties.setUpdateTagsOnStartup(getUpdateTagsCheckBox().isSelected());
        BibSonomyProperties.setUploadDocumentsOnExport(getUploadDocumentsCheckBox().isSelected());
        BibSonomyProperties.setDownloadDocumentsOnImport(getDownloadDocumentsCheckBox().isSelected());
        BibSonomyProperties.setIgnoreMorePostsWarning(getNoWarningOnMorePostsCheckBox().isSelected());
        BibSonomyProperties.setTagCloudOrder(((OrderComboBoxItem) getTagCloudOrderComboBox().getSelectedItem()).getKey());

        switch (((GroupingComboBoxItem) getDefaultVisibilityComboBox().getSelectedItem()).getKey()) {
            case USER:
                BibSonomyProperties.setDefaultVisisbility("private");
                break;
            case GROUP:
                BibSonomyProperties.setDefaultVisisbility(((GroupingComboBoxItem) getDefaultVisibilityComboBox().getSelectedItem()).getValue());
                break;
            default:
                BibSonomyProperties.setDefaultVisisbility("public");
                break;
        }

        BibSonomyProperties.save();
    }

    @Override
    public boolean validateSettings() {
        return true;
    }

    @Override
    public String getTabName() {
        return Localization.lang("BibSonomy");
    }

    private JPanel getGeneralSettingsPanel() {
        JLabel tagSizeLabel;
        JLabel tagCloudOrderLabel;
        JLabel tagCloudOrderHintLabel;
        JLabel numberOfPostsLabel;
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

            GridBagConstraints gridBagConstraints0 = new GridBagConstraints();
            gridBagConstraints0.gridy = 0;
            gridBagConstraints0.gridx = 1;
            gridBagConstraints0.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints0.weightx = 1.0;
            gridBagConstraints0.insets = new Insets(0, 0, 3, 0);
            gridBagConstraints0.gridwidth = 6;

            GridBagConstraints gridBagConstraints1_1 = new GridBagConstraints();
            gridBagConstraints1_1.gridy = 1;
            gridBagConstraints1_1.gridx = 1;
            gridBagConstraints1_1.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints1_1.insets = new Insets(0, 0, 3, 3);
            gridBagConstraints1_1.anchor = GridBagConstraints.WEST;

            GridBagConstraints gridBagConstraints1_2 = new GridBagConstraints();
            gridBagConstraints1_2.gridy = 1;
            gridBagConstraints1_2.gridx = 2;
            gridBagConstraints1_2.fill = GridBagConstraints.BOTH;
            gridBagConstraints1_2.weightx = 1.0;
            gridBagConstraints1_2.insets = new Insets(0, 0, 3, 0);
            gridBagConstraints1_2.gridwidth = 2;

            GridBagConstraints gridBagConstraints2_1 = new GridBagConstraints();
            gridBagConstraints2_1.gridy = 2;
            gridBagConstraints2_1.gridx = 1;
            gridBagConstraints2_1.anchor = GridBagConstraints.WEST;
            gridBagConstraints2_1.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints2_1.insets = new Insets(0, 0, 3, 3);

            GridBagConstraints gridBagConstraints2_2 = new GridBagConstraints();
            gridBagConstraints2_2.gridy = 2;
            gridBagConstraints2_2.gridx = 2;
            gridBagConstraints2_2.fill = GridBagConstraints.BOTH;
            gridBagConstraints2_2.weightx = 1.0;
            gridBagConstraints2_2.insets = new Insets(0, 0, 3, 0);
            gridBagConstraints2_2.gridwidth = 2;

            GridBagConstraints gridBagConstraints3_2 = new GridBagConstraints();
            gridBagConstraints3_2.gridy = 3;
            gridBagConstraints3_2.gridx = 2;
            gridBagConstraints3_2.anchor = GridBagConstraints.WEST;
            gridBagConstraints3_2.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints3_2.insets = new Insets(0, 0, 3, 0);

            GridBagConstraints gridBagConstraints4_1 = new GridBagConstraints();
            gridBagConstraints4_1.gridy = 4;
            gridBagConstraints4_1.gridx = 1;
            gridBagConstraints4_1.anchor = GridBagConstraints.WEST;
            gridBagConstraints4_1.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints4_1.insets = new Insets(0, 0, 0, 3);

            GridBagConstraints gridBagConstraints4_2 = new GridBagConstraints();
            gridBagConstraints4_2.gridy = 4;
            gridBagConstraints4_2.gridx = 2;
            gridBagConstraints4_2.fill = GridBagConstraints.BOTH;
            gridBagConstraints4_2.weightx = 1.0;
            gridBagConstraints4_2.gridwidth = 2;
            gridBagConstraints4_2.ipadx = 2;
            gridBagConstraints4_2.ipady = 2;
            gridBagConstraints4_2.insets = new Insets(0, 0, 3, 0);

            GridBagConstraints gridBagConstraints5_2 = new GridBagConstraints();
            gridBagConstraints5_2.gridy = 5;
            gridBagConstraints5_2.gridx = 2;
            gridBagConstraints5_2.anchor = GridBagConstraints.WEST;
            gridBagConstraints5_2.insets = new Insets(0, 0, 3, 0);
            gridBagConstraints5_2.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints5_2.gridwidth = 4;

            GridBagConstraints gridBagConstraints6_2 = new GridBagConstraints();
            gridBagConstraints6_2.gridy = 6;
            gridBagConstraints6_2.gridx = 2;
            gridBagConstraints6_2.anchor = GridBagConstraints.WEST;
            gridBagConstraints6_2.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints6_2.weightx = 1.0;
            gridBagConstraints6_2.insets = new Insets(0, 0, 3, 0);
            gridBagConstraints6_2.gridwidth = 7;

            GridBagConstraints gridBagConstraints8_2 = new GridBagConstraints();
            gridBagConstraints8_2.gridy = 8;
            gridBagConstraints8_2.gridx = 2;
            gridBagConstraints8_2.anchor = GridBagConstraints.WEST;
            gridBagConstraints8_2.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints8_2.weightx = 1.0;
            gridBagConstraints8_2.insets = new Insets(0, 0, 3, 0);
            gridBagConstraints8_2.gridwidth = 5;

            GridBagConstraints gridBagConstraints9_2 = new GridBagConstraints();
            gridBagConstraints9_2.gridy = 9;
            gridBagConstraints9_2.gridx = 2;
            gridBagConstraints9_2.anchor = GridBagConstraints.WEST;
            gridBagConstraints9_2.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints9_2.weightx = 1.0;
            gridBagConstraints9_2.insets = new Insets(0, 0, 3, 0);
            gridBagConstraints9_2.gridwidth = 5;

            GridBagConstraints gridBagConstraints10_2 = new GridBagConstraints();
            gridBagConstraints10_2.gridy = 10;
            gridBagConstraints10_2.gridx = 2;
            gridBagConstraints10_2.anchor = GridBagConstraints.WEST;
            gridBagConstraints10_2.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints10_2.insets = new Insets(0, 0, 3, 0);
            gridBagConstraints10_2.gridwidth = 5;

            GridBagConstraints gridBagConstraints14_1 = new GridBagConstraints();
            gridBagConstraints14_1.gridy = 14;
            gridBagConstraints14_1.gridx = 1;
            gridBagConstraints14_1.anchor = GridBagConstraints.WEST;
            gridBagConstraints14_1.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints14_1.weightx = 1.0;
            gridBagConstraints14_1.insets = new Insets(0, 0, 3, 0);
            gridBagConstraints14_1.gridwidth = 1;

            GridBagConstraints gridBagConstraints14_2 = new GridBagConstraints();
            gridBagConstraints14_2.gridy = 14;
            gridBagConstraints14_2.gridx = 2;
            gridBagConstraints14_2.fill = GridBagConstraints.BOTH;
            gridBagConstraints14_2.gridwidth = 1;
            gridBagConstraints14_2.weightx = 1;
            gridBagConstraints14_2.insets = new Insets(0, 0, 3, 0);
            gridBagConstraints14_2.gridwidth = 1;

            GridBagConstraints gridBagConstraints15_1 = new GridBagConstraints();
            gridBagConstraints15_1.gridy = 15;
            gridBagConstraints15_1.gridx = 1;
            gridBagConstraints15_1.anchor = GridBagConstraints.WEST;
            gridBagConstraints15_1.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints15_1.weightx = 0.1;
            gridBagConstraints15_1.insets = new Insets(0, 0, 0, 3);

            GridBagConstraints gridBagConstraints15_2 = new GridBagConstraints();
            gridBagConstraints15_2.gridy = 15;
            gridBagConstraints15_2.gridx = 2;
            gridBagConstraints15_2.fill = GridBagConstraints.BOTH;
            gridBagConstraints15_2.weightx = 1.0;
            gridBagConstraints15_2.insets = new Insets(0, 0, 3, 0);
            gridBagConstraints15_2.gridwidth = 5;

            GridBagConstraints gridBagConstraints19 = new GridBagConstraints();
            gridBagConstraints19.gridy = 19;
            gridBagConstraints19.gridx = 1;
            gridBagConstraints19.fill = GridBagConstraints.BOTH;
            gridBagConstraints19.weightx = 1.0;
            gridBagConstraints19.weighty = 1.0;
            gridBagConstraints19.gridwidth = 8;

            generalSettingsPanel = new JPanel();
            generalSettingsPanel.setLayout(new GridBagLayout());
            generalSettingsPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
            generalSettingsPanel.add(getCredentialsPanel(), gridBagConstraints0);
            generalSettingsPanel.add(getIgnoreOneTagWarningCheckBox(), gridBagConstraints6_2);
            generalSettingsPanel.add(getUpdateTagsCheckBox(), gridBagConstraints8_2);
            generalSettingsPanel.add(getUploadDocumentsCheckBox(), gridBagConstraints9_2);
            generalSettingsPanel.add(getDownloadDocumentsCheckBox(), gridBagConstraints10_2);
            generalSettingsPanel.add(defaultVisibilityLabel, gridBagConstraints15_1);
            generalSettingsPanel.add(getDefaultVisibilityComboBox(), gridBagConstraints15_2);
            generalSettingsPanel.add(getGeneralSettingsWhitespacePanel(), gridBagConstraints19);
            generalSettingsPanel.add(numberOfPostsLabel, gridBagConstraints4_1);
            generalSettingsPanel.add(getNumberOfPostsSpinner(), gridBagConstraints4_2);
            generalSettingsPanel.add(getNoWarningOnMorePostsCheckBox(), gridBagConstraints5_2);
            generalSettingsPanel.add(tagSizeLabel, gridBagConstraints1_1);
            generalSettingsPanel.add(getTagCloudSizeSpinner(), gridBagConstraints1_2);
            generalSettingsPanel.add(tagCloudOrderLabel, gridBagConstraints2_1);
            generalSettingsPanel.add(getTagCloudOrderComboBox(), gridBagConstraints2_2);
            generalSettingsPanel.add(tagCloudOrderHintLabel, gridBagConstraints3_2);
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
            // TODO: Why is here written "Private", whereas org.jabref.gui.actions.bibsonomy.RefreshTagListAction.getDefaultGroupings() uses BibSonomyProperties.getUsername()
            items.add(new GroupingComboBoxItem(GroupingEntity.USER, "Private"));

            defaultVisibilityComboBox = new JComboBox<>();
            new UpdateVisibilityWorker(defaultVisibilityComboBox, items, BibSonomyProperties.getDefaultVisibilty()).run();
        }
        return defaultVisibilityComboBox;
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

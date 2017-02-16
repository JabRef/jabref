package net.sf.jabref.gui.actions.bibsonomy;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JSpinner;
import javax.swing.JTextField;

import net.sf.jabref.bibsonomy.BibSonomyProperties;
import net.sf.jabref.gui.bibsonomy.BibSonomySettingsDialog;
import net.sf.jabref.gui.bibsonomy.GroupingComboBoxItem;
import net.sf.jabref.gui.bibsonomy.OrderComboBoxItem;

import org.jabref.gui.IconTheme;
import org.jabref.logic.l10n.Localization;

/**
 * Saves the properties and closes the {@link BibSonomySettingsDialog}.
 */
public class CloseBibSonomySettingsDialogBySaveAction extends AbstractAction {

    private JTextField apiUrl;
    private JTextField username;
    private JTextField apiKey;
    private JCheckBox saveApiKey;
    private JSpinner numberOfPosts;
    private JSpinner tagCloudSize;
    private JCheckBox ignoreNoTagsAssigned;
    private JCheckBox updateTags;
    private JCheckBox uploadDocuments;
    private JCheckBox downloadDocuments;
    private JComboBox<?> visibility;
    private JCheckBox morePosts;
    private JTextField extraFields;
    private BibSonomySettingsDialog settingsDialog;
    private JComboBox<?> order;

    public void actionPerformed(ActionEvent e) {

        BibSonomyProperties.setApiUrl(apiUrl.getText());
        BibSonomyProperties.setUsername(username.getText());
        BibSonomyProperties.setApiKey(apiKey.getText());
        BibSonomyProperties.setStoreApiKey(saveApiKey.isSelected());
        BibSonomyProperties.setNumberOfPostsPerRequest((Integer) numberOfPosts.getValue());
        BibSonomyProperties.setTagCloudSize((Integer) tagCloudSize.getValue());
        BibSonomyProperties.setIgnoreNoTagsAssigned(ignoreNoTagsAssigned.isSelected());
        BibSonomyProperties.setUpdateTagsOnStartup(updateTags.isSelected());
        BibSonomyProperties.setUploadDocumentsOnExport(uploadDocuments.isSelected());
        BibSonomyProperties.setDownloadDocumentsOnImport(downloadDocuments.isSelected());
        BibSonomyProperties.setIgnoreMorePostsWarning(morePosts.isSelected());
        BibSonomyProperties.setExtraFields(extraFields.getText());
        BibSonomyProperties.setTagCloudOrder(((OrderComboBoxItem) order.getSelectedItem()).getKey());

        switch (((GroupingComboBoxItem) visibility.getSelectedItem()).getKey()) {
            case USER:
                BibSonomyProperties.setDefaultVisisbility("private");
                break;
            case GROUP:
                BibSonomyProperties.setDefaultVisisbility(((GroupingComboBoxItem) visibility.getSelectedItem()).getValue());
                break;
            default:
                BibSonomyProperties.setDefaultVisisbility("public");
        }

        BibSonomyProperties.save();
        settingsDialog.setVisible(false);
    }

    public CloseBibSonomySettingsDialogBySaveAction(BibSonomySettingsDialog settingsDialog, JTextField apiUrl, JTextField username, JTextField apiKey,
                                                    JCheckBox saveApiKey, JSpinner numberOfPosts,
                                                    JSpinner tagCloudSize, JCheckBox ignoreNoTagsAssigned,
                                                    JCheckBox updateTags, JCheckBox uploadDocuments,
                                                    JCheckBox downloadDocuments, JComboBox<?> visibility,
                                                    JCheckBox morePosts, JTextField extraFields, JComboBox<?> order) {

        super(Localization.lang("Save"), IconTheme.JabRefIcon.SAVE.getIcon());
        this.apiUrl = apiUrl;
        this.settingsDialog = settingsDialog;
        this.username = username;
        this.apiKey = apiKey;
        this.saveApiKey = saveApiKey;
        this.numberOfPosts = numberOfPosts;
        this.tagCloudSize = tagCloudSize;
        this.ignoreNoTagsAssigned = ignoreNoTagsAssigned;
        this.updateTags = updateTags;
        this.uploadDocuments = uploadDocuments;
        this.downloadDocuments = downloadDocuments;
        this.visibility = visibility;
        this.morePosts = morePosts;
        this.extraFields = extraFields;
        this.order = order;
    }
}

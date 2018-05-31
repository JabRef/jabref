package org.jabref.gui.dbproperties;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.charset.Charset;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.FXDialogService;
import org.jabref.gui.JabRefDialog;
import org.jabref.gui.SaveOrderConfigDisplay;
import org.jabref.gui.cleanup.FieldFormatterCleanupsPanel;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.logic.cleanup.Cleanups;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Encodings;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.shared.DatabaseLocation;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.metadata.SaveOrderConfig;
import org.jabref.preferences.JabRefPreferences;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class DatabasePropertiesDialog extends JabRefDialog {

    private MetaData metaData;
    private BasePanel panel;
    private final JComboBox<Charset> encoding;
    private final JButton ok;
    private final JButton cancel;
    private final JTextField fileDir = new JTextField(40);
    private final JTextField fileDirIndv = new JTextField(40);
    private String oldFileVal = "";
    private String oldFileIndvVal = "";
    private SaveOrderConfig oldSaveOrderConfig;

    /* The code for "Save sort order" is copied from ExportSortingPrefsTab and slightly updated to fit storing at metadata */
    private JRadioButton saveInOriginalOrder;

    private JRadioButton saveInSpecifiedOrder;

    private final JCheckBox protect = new JCheckBox(
            Localization.lang("Refuse to save the library before external changes have been reviewed."));
    private boolean oldProtectVal;
    private SaveOrderConfigDisplay saveOrderPanel;

    private FieldFormatterCleanupsPanel fieldFormatterCleanupsPanel;

    public DatabasePropertiesDialog(JFrame parent) {
        super(parent, Localization.lang("Library properties"), true, DatabasePropertiesDialog.class);
        encoding = new JComboBox<>();
        encoding.setModel(new DefaultComboBoxModel<>(Encodings.ENCODINGS));
        ok = new JButton(Localization.lang("OK"));
        cancel = new JButton(Localization.lang("Cancel"));
        init();
    }

    public void setPanel(BasePanel panel) {
        this.panel = panel;
        this.metaData = panel.getBibDatabaseContext().getMetaData();
    }

    public void updateEnableStatus() {
        DatabaseLocation location = panel.getBibDatabaseContext().getLocation();
        boolean isShared = (location == DatabaseLocation.SHARED);
        encoding.setEnabled(!isShared); // the encoding of shared database is always UTF-8
        saveInOriginalOrder.setEnabled(!isShared);
        saveInSpecifiedOrder.setEnabled(!isShared);
        saveOrderPanel.setEnabled(!isShared);
        protect.setEnabled(!isShared);
    }

    private void init() {

        DirectoryDialogConfiguration directoryDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                .withInitialDirectory(Globals.prefs.get(JabRefPreferences.WORKING_DIRECTORY)).build();
        DialogService ds = new FXDialogService();

        JButton browseFile = new JButton(Localization.lang("Browse"));
        JButton browseFileIndv = new JButton(Localization.lang("Browse"));

        browseFile.addActionListener(e -> DefaultTaskExecutor
                .runInJavaFXThread(() -> ds.showDirectorySelectionDialog(directoryDialogConfiguration))
                .ifPresent(f -> fileDir.setText(f.toAbsolutePath().toString())));
        browseFileIndv.addActionListener(e -> DefaultTaskExecutor
                .runInJavaFXThread(() -> ds.showDirectorySelectionDialog(directoryDialogConfiguration))
                .ifPresent(f -> fileDirIndv.setText(f.toAbsolutePath().toString())));

        setupSortOrderConfiguration();
        FormLayout form = new FormLayout("left:pref, 4dlu, pref:grow, 4dlu, pref:grow, 4dlu, pref",
                "pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, fill:pref:grow, 180dlu, fill:pref:grow,");
        FormBuilder builder = FormBuilder.create().layout(form);
        builder.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        builder.add(Localization.lang("Library encoding")).xy(1, 1);
        builder.add(encoding).xy(3, 1);

        builder.addSeparator(Localization.lang("Override default file directories")).xyw(1, 3, 5);
        builder.add(Localization.lang("General file directory")).xy(1, 5);
        builder.add(fileDir).xy(3, 5);
        builder.add(browseFile).xy(5, 5);
        builder.add(Localization.lang("User-specific file directory")).xy(1, 7);
        builder.add(fileDirIndv).xy(3, 7);
        builder.add(browseFileIndv).xy(5, 7);

        builder.addSeparator(Localization.lang("Save sort order")).xyw(1, 13, 5);
        builder.add(saveInOriginalOrder).xyw(1, 15, 5);
        builder.add(saveInSpecifiedOrder).xyw(1, 17, 5);

        saveOrderPanel = new SaveOrderConfigDisplay();
        builder.add(saveOrderPanel.getPanel()).xyw(1, 21, 5);

        builder.addSeparator(Localization.lang("Library protection")).xyw(1, 23, 5);
        builder.add(protect).xyw(1, 25, 5);

        fieldFormatterCleanupsPanel = new FieldFormatterCleanupsPanel(Localization.lang("Enable save actions"),
                Cleanups.DEFAULT_SAVE_ACTIONS);
        builder.addSeparator(Localization.lang("Save actions")).xyw(1, 27, 5);
        builder.add(fieldFormatterCleanupsPanel).xyw(1, 29, 5);

        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        bb.addButton(ok);
        bb.addButton(cancel);
        bb.addRelatedGap();
        bb.addButton(new HelpAction(HelpFile.DATABASE_PROPERTIES).getHelpButton());
        bb.addGlue();
        bb.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        getContentPane().add(builder.getPanel(), BorderLayout.CENTER);
        getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);
        pack();

        AbstractAction closeAction = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        };
        ActionMap am = builder.getPanel().getActionMap();
        InputMap im = builder.getPanel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(Globals.getKeyPrefs().getKey(KeyBinding.CLOSE_DIALOG), "close");
        am.put("close", closeAction);

        ok.addActionListener(e -> {
            if (propertiesChanged()) {
                storeSettings();
            }
            dispose();
        });

        cancel.addActionListener(e -> dispose());
    }

    private boolean propertiesChanged() {
        Charset oldEncoding = panel.getBibDatabaseContext().getMetaData().getEncoding()
                .orElse(Globals.prefs.getDefaultEncoding());
        Charset newEncoding = (Charset) encoding.getSelectedItem();
        boolean saveActionsChanged = fieldFormatterCleanupsPanel.hasChanged();
        boolean saveOrderConfigChanged = !getNewSaveOrderConfig().equals(oldSaveOrderConfig);
        boolean changed = saveOrderConfigChanged || !newEncoding.equals(oldEncoding)
                || !oldFileVal.equals(fileDir.getText()) || !oldFileIndvVal.equals(fileDirIndv.getText())
                || (oldProtectVal != protect.isSelected()) || saveActionsChanged;
        return changed;
    }

    private SaveOrderConfig getNewSaveOrderConfig() {
        SaveOrderConfig saveOrderConfig = null;
        if (saveInOriginalOrder.isSelected()) {
            saveOrderConfig = SaveOrderConfig.getDefaultSaveOrder();
        } else {
            saveOrderConfig = saveOrderPanel.getSaveOrderConfig();
            saveOrderConfig.setSaveInSpecifiedOrder();
        }
        return saveOrderConfig;
    }

    private void setupSortOrderConfiguration() {
        saveInOriginalOrder = new JRadioButton(Localization.lang("Save entries in their original order"));
        saveInSpecifiedOrder = new JRadioButton(Localization.lang("Save entries ordered as specified"));

        ButtonGroup bg = new ButtonGroup();
        bg.add(saveInOriginalOrder);
        bg.add(saveInSpecifiedOrder);
        ActionListener listener = e -> {
            boolean selected = e.getSource() == saveInSpecifiedOrder;
            saveOrderPanel.setEnabled(selected);
        };

        saveInOriginalOrder.addActionListener(listener);
        saveInSpecifiedOrder.addActionListener(listener);
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            setValues();
        }
        super.setVisible(visible);
    }

    private void setValues() {
        Optional<Charset> charset = panel.getBibDatabaseContext().getMetaData().getEncoding();
        encoding.setSelectedItem(charset.orElse(Globals.prefs.getDefaultEncoding()));

        Optional<SaveOrderConfig> storedSaveOrderConfig = metaData.getSaveOrderConfig();
        boolean selected;
        if (!storedSaveOrderConfig.isPresent()) {
            saveInOriginalOrder.setSelected(true);
            oldSaveOrderConfig = SaveOrderConfig.getDefaultSaveOrder();
            selected = false;
        } else {
            SaveOrderConfig saveOrderConfig = storedSaveOrderConfig.get();
            oldSaveOrderConfig = saveOrderConfig;
            if (saveOrderConfig.saveInOriginalOrder) {
                saveInOriginalOrder.setSelected(true);
                selected = false;
            } else {
                saveInSpecifiedOrder.setSelected(true);
                selected = true;
            }
            saveOrderPanel.setSaveOrderConfig(saveOrderConfig);
        }
        saveOrderPanel.setEnabled(selected);

        Optional<String> fileD = metaData.getDefaultFileDirectory();
        if (fileD.isPresent()) {
            fileDir.setText(fileD.get().trim());
        } else {
            fileDir.setText("");
        }

        String fileDI = metaData.getUserFileDirectory(Globals.prefs.getUser()).orElse(""); // File dir setting
        fileDirIndv.setText(fileDI);
        oldFileIndvVal = fileDirIndv.getText();

        protect.setSelected(metaData.isProtected());

        // Store original values to see if they get changed:
        oldFileVal = fileDir.getText();
        oldProtectVal = protect.isSelected();

        //set save actions
        fieldFormatterCleanupsPanel.setValues(metaData);
    }

    private void storeSettings() {
        Charset oldEncoding = panel.getBibDatabaseContext().getMetaData().getEncoding()
                .orElse(Globals.prefs.getDefaultEncoding());
        Charset newEncoding = (Charset) encoding.getSelectedItem();
        panel.getBibDatabaseContext().getMetaData().setEncoding(newEncoding);

        String text = fileDir.getText().trim();
        if (text.isEmpty()) {
            metaData.clearDefaultFileDirectory();
        } else {
            metaData.setDefaultFileDirectory(text);
        }
        // Repeat for individual file dir - reuse 'text' and 'dir' vars
        text = fileDirIndv.getText();
        if (text.isEmpty()) {
            metaData.clearUserFileDirectory(Globals.prefs.getUser());
        } else {
            metaData.setUserFileDirectory(Globals.prefs.getUser(), text);
        }

        if (protect.isSelected()) {
            metaData.markAsProtected();
        } else {
            metaData.markAsNotProtected();
        }

        SaveOrderConfig newSaveOrderConfig = getNewSaveOrderConfig();

        boolean saveOrderConfigChanged = !getNewSaveOrderConfig().equals(oldSaveOrderConfig);

        // See if any of the values have been modified:
        if (saveOrderConfigChanged) {
            if (newSaveOrderConfig.equals(SaveOrderConfig.getDefaultSaveOrder())) {
                metaData.clearSaveOrderConfig();
            } else {
                metaData.setSaveOrderConfig(newSaveOrderConfig);
            }
        }

        boolean saveActionsChanged = fieldFormatterCleanupsPanel.hasChanged();
        if (saveActionsChanged) {
            if (fieldFormatterCleanupsPanel.isDefaultSaveActions()) {
                metaData.clearSaveActions();
            } else {
                fieldFormatterCleanupsPanel.storeSettings(metaData);
            }
        }

        boolean changed = saveOrderConfigChanged || !newEncoding.equals(oldEncoding)
                || !oldFileVal.equals(fileDir.getText()) || !oldFileIndvVal.equals(fileDirIndv.getText())
                || (oldProtectVal != protect.isSelected()) || saveActionsChanged;
        // ... if so, mark base changed. Prevent the Undo button from removing
        // change marking:
        if (changed) {
            panel.markNonUndoableBaseChanged();
        }
    }
}

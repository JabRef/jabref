/*  Copyright (C) 2003-2016 JabRef contributors.
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
package net.sf.jabref.gui.dbproperties;

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
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import net.sf.jabref.Globals;
import net.sf.jabref.MetaData;
import net.sf.jabref.exporter.FieldFormatterCleanups;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.SaveOrderConfigDisplay;
import net.sf.jabref.gui.actions.BrowseAction;
import net.sf.jabref.gui.cleanup.FieldFormatterCleanupsPanel;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.logic.config.SaveOrderConfig;
import net.sf.jabref.logic.l10n.Encodings;
import net.sf.jabref.logic.l10n.Localization;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Oct 31, 2005
 * Time: 10:46:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class DatabasePropertiesDialog extends JDialog {

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
    private SaveOrderConfig defaultSaveOrderConfig;

    /* The code for "Save sort order" is copied from FileSortTab and slightly updated to fit storing at metadata */
    private JRadioButton saveInOriginalOrder;
    private JRadioButton saveInSpecifiedOrder;

    private final JCheckBox protect = new JCheckBox(
            Localization.lang("Refuse to save the database before external changes have been reviewed."));
    private boolean oldProtectVal;
    private SaveOrderConfigDisplay saveOrderPanel;

    private FieldFormatterCleanupsPanel fieldFormatterCleanupsPanel;


    public DatabasePropertiesDialog(JFrame parent) {
        super(parent, Localization.lang("Database properties"), true);
        encoding = new JComboBox<>();
        encoding.setModel(new DefaultComboBoxModel<>(Encodings.ENCODINGS));
        ok = new JButton(Localization.lang("OK"));
        cancel = new JButton(Localization.lang("Cancel"));
        init(parent);
    }

    public void setPanel(BasePanel panel) {
        this.panel = panel;
        this.metaData = panel.getBibDatabaseContext().getMetaData();
    }

    private void init(JFrame parent) {

        JButton browseFile = new JButton(Localization.lang("Browse"));
        JButton browseFileIndv = new JButton(Localization.lang("Browse"));
        browseFile.addActionListener(BrowseAction.buildForDir(parent, fileDir));
        browseFileIndv.addActionListener(BrowseAction.buildForDir(parent, fileDirIndv));

        setupSortOrderConfiguration();
        FormLayout form = new FormLayout("left:pref, 4dlu, pref:grow, 4dlu, pref:grow, 4dlu, pref",
                "pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, fill:pref:grow, 180dlu, fill:pref:grow,");
        FormBuilder builder = FormBuilder.create().layout(form);
        builder.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        builder.add(Localization.lang("Database encoding")).xy(1, 1);
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

        builder.addSeparator(Localization.lang("Database protection")).xyw(1, 23, 5);
        builder.add(protect).xyw(1, 25, 5);

        fieldFormatterCleanupsPanel = new FieldFormatterCleanupsPanel(Localization.lang("Enable save actions"),
                FieldFormatterCleanups.DEFAULT_SAVE_ACTIONS);
        builder.addSeparator(Localization.lang("Save actions")).xyw(1, 27, 5);
        builder.add(fieldFormatterCleanupsPanel).xyw(1, 29, 5);

        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        bb.addButton(ok);
        bb.addButton(cancel);
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
            storeSettings();
            dispose();
        });

        cancel.addActionListener(e -> dispose());
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
        encoding.setSelectedItem(panel.getEncoding());

        defaultSaveOrderConfig = new SaveOrderConfig();
        defaultSaveOrderConfig.setSaveInOriginalOrder();

        Optional<SaveOrderConfig> storedSaveOrderConfig = metaData.getSaveOrderConfig();
        boolean selected;
        if (!storedSaveOrderConfig.isPresent()) {
            saveInOriginalOrder.setSelected(true);
            oldSaveOrderConfig = null;
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

        Charset oldEncoding = panel.getEncoding();
        Charset newEncoding = (Charset) encoding.getSelectedItem();
        panel.setEncoding(newEncoding);

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

        SaveOrderConfig newSaveOrderConfig = saveOrderPanel.getSaveOrderConfig();
        if (saveInOriginalOrder.isSelected()) {
            newSaveOrderConfig.setSaveInOriginalOrder();
        } else {
            newSaveOrderConfig.setSaveInSpecifiedOrder();
        }

        // See if any of the values have been modified:
        boolean saveOrderConfigChanged;
        if (newSaveOrderConfig.equals(oldSaveOrderConfig)) {
            saveOrderConfigChanged = false;
        } else {
            saveOrderConfigChanged = true;
        }

        if (saveOrderConfigChanged) {
            if (newSaveOrderConfig.equals(defaultSaveOrderConfig)) {
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

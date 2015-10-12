/*  Copyright (C) 2003-2011 JabRef contributors.
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
package net.sf.jabref.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import net.sf.jabref.gui.actions.BrowseAction;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.MetaData;
import net.sf.jabref.logic.config.SaveOrderConfig;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import net.sf.jabref.logic.l10n.Encodings;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibtexEntry;

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
    private final JComboBox<String> encoding;
    private final JButton ok;
    private final JButton cancel;
    private final JTextField fileDir = new JTextField(40);
    private final JTextField fileDirIndv = new JTextField(40);
    private final JTextField pdfDir = new JTextField(40);
    private final JTextField psDir = new JTextField(40);
    private String oldFileVal = "";
    private String oldFileIndvVal = "";
    private String oldPdfVal = "";
    private String oldPsVal = ""; // Remember old values to see if they are changed.
    private SaveOrderConfig oldSaveOrderConfig;

    /* The code for "Save sort order" is copied from FileSortTab and slightly updated to fit storing at metadata */

    private JRadioButton saveAsConfiguredGlobally;
    private JRadioButton saveInOriginalOrder;
    private JRadioButton saveInSpecifiedOrder;
    private JComboBox<String> savePriSort;
    private JComboBox<String> saveSecSort;
    private JComboBox<String> saveTerSort;
    private JTextField savePriField;
    private JTextField saveSecField;
    private JTextField saveTerField;
    private JCheckBox savePriDesc;
    private JCheckBox saveSecDesc;
    private JCheckBox saveTerDesc;

    public static final String SAVE_ORDER_CONFIG = "saveOrderConfig";

    private final JCheckBox protect = new JCheckBox(Localization.lang("Refuse to save the database before external changes have been reviewed."));
    private boolean oldProtectVal;


    public DatabasePropertiesDialog(JFrame parent) {
        super(parent, Localization.lang("Database properties"), true);
        encoding = new JComboBox<>(Encodings.ENCODINGS);
        ok = new JButton(Localization.lang("Ok"));
        cancel = new JButton(Localization.lang("Cancel"));
        init(parent);
    }

    public void setPanel(BasePanel panel) {
        this.panel = panel;
        this.metaData = panel.metaData();
    }

    private void init(JFrame parent) {

        JButton browseFile = new JButton(Localization.lang("Browse"));
        JButton browseFileIndv = new JButton(Localization.lang("Browse"));
        JButton browsePdf = new JButton(Localization.lang("Browse"));
        JButton browsePs = new JButton(Localization.lang("Browse"));
        browseFile.addActionListener(BrowseAction.buildForDir(parent, fileDir));
        browseFileIndv.addActionListener(BrowseAction.buildForDir(parent, fileDirIndv));
        browsePdf.addActionListener(BrowseAction.buildForDir(parent, pdfDir));
        browsePs.addActionListener(BrowseAction.buildForDir(parent, psDir));

        setupSortOrderConfiguration();

        DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout("left:pref, 4dlu, left:pref, 4dlu, fill:pref", ""));
        builder.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        builder.append(Localization.lang("Database encoding"));
        builder.append(encoding);
        builder.nextLine();

        builder.appendSeparator(Localization.lang("Override default file directories"));
        builder.nextLine();
        builder.append(Localization.lang("General file directory"));
        builder.append(fileDir);
        builder.append(browseFile);
        builder.nextLine();
        builder.append(Localization.lang("User-specific file directory"));
        builder.append(fileDirIndv);
        builder.append(browseFileIndv);
        builder.nextLine();
        builder.append(Localization.lang("PDF directory"));
        builder.append(pdfDir);
        builder.append(browsePdf);
        builder.nextLine();
        builder.append(Localization.lang("PS directory"));
        builder.append(psDir);
        builder.append(browsePs);
        builder.nextLine();

        builder.appendSeparator(Localization.lang("Save sort order"));
        builder.append(saveAsConfiguredGlobally, 1);
        builder.nextLine();
        builder.append(saveInOriginalOrder, 1);
        builder.nextLine();
        builder.append(saveInSpecifiedOrder, 1);
        builder.nextLine();

        // Create a new panel with its own FormLayout for these items:
        FormLayout layout2 = new FormLayout("right:pref, 8dlu, fill:pref, 4dlu, fill:60dlu, 4dlu, left:pref", "");
        DefaultFormBuilder builder2 = new DefaultFormBuilder(layout2);
        JLabel lab = new JLabel(Localization.lang("Primary sort criterion"));
        builder2.append(lab);
        builder2.append(savePriSort);
        builder2.append(savePriField);
        builder2.append(savePriDesc);
        builder2.nextLine();
        lab = new JLabel(Localization.lang("Secondary sort criterion"));
        builder2.append(lab);
        builder2.append(saveSecSort);
        builder2.append(saveSecField);
        builder2.append(saveSecDesc);
        builder2.nextLine();
        lab = new JLabel(Localization.lang("Tertiary sort criterion"));
        builder2.append(lab);
        builder2.append(saveTerSort);
        builder2.append(saveTerField);
        builder2.append(saveTerDesc);

        JPanel saveSpecPanel = builder2.getPanel();
        builder.append(saveSpecPanel);
        builder.nextLine();

        builder.appendSeparator(Localization.lang("Database protection"));
        builder.nextLine();
        builder.append(protect, 3);
        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        bb.addButton(ok);
        bb.addButton(cancel);
        bb.addGlue();

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
        im.put(Globals.prefs.getKey("Close dialog"), "close");
        am.put("close", closeAction);

        ok.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                storeSettings();
                dispose();
            }
        });

        cancel.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

    }

    private void setupSortOrderConfiguration() {
        saveAsConfiguredGlobally = new JRadioButton(Localization.lang("Save entries as configured globally"));
        saveInOriginalOrder = new JRadioButton(Localization.lang("Save entries in their original order"));
        saveInSpecifiedOrder = new JRadioButton(Localization.lang("Save entries ordered as specified"));

        ButtonGroup bg = new ButtonGroup();
        bg.add(saveAsConfiguredGlobally);
        bg.add(saveInOriginalOrder);
        bg.add(saveInSpecifiedOrder);
        ActionListener listener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                boolean selected = e.getSource() == saveInSpecifiedOrder;
                savePriSort.setEnabled(selected);
                savePriField.setEnabled(selected);
                savePriDesc.setEnabled(selected);
                saveSecSort.setEnabled(selected);
                saveSecField.setEnabled(selected);
                saveSecDesc.setEnabled(selected);
                saveTerSort.setEnabled(selected);
                saveTerField.setEnabled(selected);
                saveTerDesc.setEnabled(selected);
            }
        };

        saveAsConfiguredGlobally.addActionListener(listener);
        saveInOriginalOrder.addActionListener(listener);
        saveInSpecifiedOrder.addActionListener(listener);

        ArrayList<String> v = new ArrayList<String>(Arrays.asList(BibtexFields.getAllFieldNames()));
        v.add(BibtexEntry.KEY_FIELD);
        Collections.sort(v);
        String[] allPlusKey = v.toArray(new String[v.size()]);
        savePriSort = new JComboBox<>(allPlusKey);
        saveSecSort = new JComboBox<>(allPlusKey);
        saveTerSort = new JComboBox<>(allPlusKey);

        savePriSort.insertItemAt(Localization.lang("<select>"), 0);
        saveSecSort.insertItemAt(Localization.lang("<select>"), 0);
        saveTerSort.insertItemAt(Localization.lang("<select>"), 0);

        savePriField = new JTextField(10);
        saveSecField = new JTextField(10);
        saveTerField = new JTextField(10);

        savePriSort.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (savePriSort.getSelectedIndex() > 0) {
                    savePriField.setText(savePriSort.getSelectedItem().toString());
                    savePriSort.setSelectedIndex(0);
                }
            }
        });
        saveSecSort.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (saveSecSort.getSelectedIndex() > 0) {
                    saveSecField.setText(saveSecSort.getSelectedItem().toString());
                    saveSecSort.setSelectedIndex(0);
                }
            }
        });
        saveTerSort.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (saveTerSort.getSelectedIndex() > 0) {
                    saveTerField.setText(saveTerSort.getSelectedItem().toString());
                    saveTerSort.setSelectedIndex(0);
                }
            }
        });

        savePriDesc = new JCheckBox(Localization.lang("Descending"));
        saveSecDesc = new JCheckBox(Localization.lang("Descending"));
        saveTerDesc = new JCheckBox(Localization.lang("Descending"));

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

        Vector<String> storedSaveOrderConfig = metaData.getData(DatabasePropertiesDialog.SAVE_ORDER_CONFIG);
        boolean selected;
        if (storedSaveOrderConfig == null) {
            saveAsConfiguredGlobally.setSelected(true);
            oldSaveOrderConfig = null;
            selected = false;
        } else {
            SaveOrderConfig saveOrderConfig;
            saveOrderConfig = new SaveOrderConfig(storedSaveOrderConfig);
            oldSaveOrderConfig = saveOrderConfig;
            if (saveOrderConfig.saveInOriginalOrder) {
                saveInOriginalOrder.setSelected(true);
                selected = false;
            } else {
                assert (saveOrderConfig.saveInSpecifiedOrder);
                saveInSpecifiedOrder.setSelected(true);
                selected = true;
            }
            savePriField.setText(saveOrderConfig.sortCriteria[0].field);
            savePriDesc.setSelected(saveOrderConfig.sortCriteria[0].descending);
            saveSecField.setText(saveOrderConfig.sortCriteria[1].field);
            saveSecDesc.setSelected(saveOrderConfig.sortCriteria[1].descending);
            saveTerField.setText(saveOrderConfig.sortCriteria[2].field);
            saveTerDesc.setSelected(saveOrderConfig.sortCriteria[2].descending);
        }
        savePriSort.setEnabled(selected);
        savePriField.setEnabled(selected);
        savePriDesc.setEnabled(selected);
        saveSecSort.setEnabled(selected);
        saveSecField.setEnabled(selected);
        saveSecDesc.setEnabled(selected);
        saveTerSort.setEnabled(selected);
        saveTerField.setEnabled(selected);
        saveTerDesc.setEnabled(selected);

        Vector<String> fileD = metaData.getData(Globals.prefs.get(JabRefPreferences.USER_FILE_DIR));
        if (fileD == null) {
            fileDir.setText("");
        } else {
            // Better be a little careful about how many entries the Vector has:
            if (fileD.size() >= 1) {
                fileDir.setText((fileD.get(0)).trim());
            }
        }

        Vector<String> fileDI = metaData.getData(Globals.prefs.get(JabRefPreferences.USER_FILE_DIR_INDIVIDUAL)); // File dir setting
        Vector<String> fileDIL = metaData.getData(Globals.prefs.get(JabRefPreferences.USER_FILE_DIR_IND_LEGACY)); // Legacy file dir setting for backward comp.
        if (fileDI == null) {
            oldFileIndvVal = fileDirIndv.getText(); // Record individual file dir setting as originally empty if reading from legacy setting
            if (fileDIL == null) {
                fileDirIndv.setText("");
            } else {
                // Insert path from legacy setting if possible
                // Better be a little careful about how many entries the Vector has:
                if (fileDIL.size() >= 1) {
                    fileDirIndv.setText((fileDIL.get(0)).trim());
                }
            }
        } else {
            // Better be a little careful about how many entries the Vector has:
            if (fileDI.size() >= 1) {
                fileDirIndv.setText((fileDI.get(0)).trim());
            }
            oldFileIndvVal = fileDirIndv.getText(); // Record individual file dir setting normally if reading from ordinary setting
        }

        Vector<String> pdfD = metaData.getData("pdfDirectory");
        if (pdfD == null) {
            pdfDir.setText("");
        } else {
            // Better be a little careful about how many entries the Vector has:
            if (pdfD.size() >= 1) {
                pdfDir.setText((pdfD.get(0)).trim());
            }
        }

        Vector<String> psD = metaData.getData("psDirectory");
        if (psD == null) {
            psDir.setText("");
        } else {
            // Better be a little careful about how many entries the Vector has:
            if (psD.size() >= 1) {
                psDir.setText((psD.get(0)).trim());
            }
        }

        Vector<String> prot = metaData.getData(Globals.PROTECTED_FLAG_META);
        if (prot == null) {
            protect.setSelected(false);
        } else {
            if (prot.size() >= 1) {
                protect.setSelected(Boolean.parseBoolean(prot.get(0)));
            }
        }

        // Store original values to see if they get changed:
        oldFileVal = fileDir.getText();
        oldPdfVal = pdfDir.getText();
        oldPsVal = psDir.getText();
        oldProtectVal = protect.isSelected();
    }

    private void storeSettings() {
        SaveOrderConfig newSaveOrderConfig;
        if (saveAsConfiguredGlobally.isSelected()) {
            metaData.remove(DatabasePropertiesDialog.SAVE_ORDER_CONFIG);
            newSaveOrderConfig = null;
        } else {
            SaveOrderConfig saveOrderConfig = new SaveOrderConfig();
            newSaveOrderConfig = saveOrderConfig;
            if (saveInOriginalOrder.isSelected()) {
                saveOrderConfig.setSaveInOriginalOrder();
            } else {
                saveOrderConfig.setSaveInSpecifiedOrder();
            }
            saveOrderConfig.sortCriteria[0].field = savePriField.getText();
            saveOrderConfig.sortCriteria[0].descending = savePriDesc.isSelected();
            saveOrderConfig.sortCriteria[1].field = saveSecField.getText();
            saveOrderConfig.sortCriteria[1].descending = saveSecDesc.isSelected();
            saveOrderConfig.sortCriteria[2].field = saveTerField.getText();
            saveOrderConfig.sortCriteria[2].descending = saveTerDesc.isSelected();

            Vector<String> serialized = saveOrderConfig.getVector();
            metaData.putData(DatabasePropertiesDialog.SAVE_ORDER_CONFIG, serialized);
        }

        String oldEncoding = panel.getEncoding();
        String newEncoding = (String) encoding.getSelectedItem();
        panel.setEncoding(newEncoding);

        Vector<String> dir = new Vector<String>(1);
        String text = fileDir.getText().trim();
        if (!text.isEmpty()) {
            dir.add(text);
            metaData.putData(Globals.prefs.get(JabRefPreferences.USER_FILE_DIR), dir);
        } else {
            metaData.remove(Globals.prefs.get(JabRefPreferences.USER_FILE_DIR));
        }
        // Repeat for individual file dir - reuse 'text' and 'dir' vars
        dir = new Vector<String>(1);
        text = fileDirIndv.getText().trim();
        if (!text.isEmpty()) {
            dir.add(text);
            metaData.putData(Globals.prefs.get(JabRefPreferences.USER_FILE_DIR_INDIVIDUAL), dir);
        } else {
            metaData.remove(Globals.prefs.get(JabRefPreferences.USER_FILE_DIR_INDIVIDUAL));
        }

        dir = new Vector<String>(1);
        text = pdfDir.getText().trim();
        if (!text.isEmpty()) {
            dir.add(text);
            metaData.putData("pdfDirectory", dir);
        } else {
            metaData.remove("pdfDirectory");
        }

        dir = new Vector<String>(1);
        text = psDir.getText().trim();
        if (!text.isEmpty()) {
            dir.add(text);
            metaData.putData("psDirectory", dir);
        } else {
            metaData.remove("psDirectory");
        }

        if (protect.isSelected()) {
            dir = new Vector<String>(1);
            dir.add("true");
            metaData.putData(Globals.PROTECTED_FLAG_META, dir);
        } else {
            metaData.remove(Globals.PROTECTED_FLAG_META);
        }

        // See if any of the values have been modified:
        boolean saveOrderConfigChanged;
        if (oldSaveOrderConfig == newSaveOrderConfig) {
            saveOrderConfigChanged = false;
        } else if ((oldSaveOrderConfig == null) || (newSaveOrderConfig == null)) {
            saveOrderConfigChanged = true;
        } else {
            // check on vector basis. This is slower than directly implementing equals, but faster to implement
            saveOrderConfigChanged = !oldSaveOrderConfig.getVector().equals(newSaveOrderConfig.getVector());
        }

        boolean changed = saveOrderConfigChanged || !newEncoding.equals(oldEncoding)
                || !oldFileVal.equals(fileDir.getText())
                || !oldFileIndvVal.equals(fileDirIndv.getText())
                || !oldPdfVal.equals(pdfDir.getText())
                || !oldPsVal.equals(psDir.getText())
                || (oldProtectVal != protect.isSelected());
        // ... if so, mark base changed. Prevent the Undo button from removing
        // change marking:
        if (changed) {
            panel.markNonUndoableBaseChanged();
        }
    }
}

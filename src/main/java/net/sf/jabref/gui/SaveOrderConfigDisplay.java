/*  Copyright (C) 2012-2016 JabRef contributors.
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

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import net.sf.jabref.bibtex.InternalBibtexFields;
import net.sf.jabref.logic.config.SaveOrderConfig;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;

public class SaveOrderConfigDisplay {

    private JPanel panel;
    private JComboBox<String> savePriSort;
    private JComboBox<String> saveSecSort;
    private JComboBox<String> saveTerSort;
    private JCheckBox savePriDesc;
    private JCheckBox saveSecDesc;
    private JCheckBox saveTerDesc;


    public SaveOrderConfigDisplay() {
        init();
    }

    private void init() {
        List<String> fieldNames = new ArrayList<>(InternalBibtexFields.getAllFieldNames());
        fieldNames.add(BibEntry.KEY_FIELD);
        Collections.sort(fieldNames);
        String[] allPlusKey = fieldNames.toArray(new String[fieldNames.size()]);
        savePriSort = new JComboBox<>(allPlusKey);
        savePriSort.setEditable(true);
        saveSecSort = new JComboBox<>(allPlusKey);
        saveSecSort.setEditable(true);
        saveTerSort = new JComboBox<>(allPlusKey);
        saveTerSort.setEditable(true);

        savePriDesc = new JCheckBox(Localization.lang("Descending"));
        saveSecDesc = new JCheckBox(Localization.lang("Descending"));
        saveTerDesc = new JCheckBox(Localization.lang("Descending"));

        FormLayout layout = new FormLayout("right:pref, 8dlu, fill:pref, 4dlu, fill:60dlu, 4dlu, left:pref",
                "pref, 2dlu, pref, 2dlu, pref");
        FormBuilder builder = FormBuilder.create().layout(layout);
        builder.add(Localization.lang("Primary sort criterion")).xy(1, 1);
        builder.add(savePriSort).xy(3, 1);
        builder.add(savePriDesc).xy(5, 1);

        builder.add(Localization.lang("Secondary sort criterion")).xy(1, 3);
        builder.add(saveSecSort).xy(3, 3);
        builder.add(saveSecDesc).xy(5, 3);

        builder.add(Localization.lang("Tertiary sort criterion")).xy(1, 5);
        builder.add(saveTerSort).xy(3, 5);
        builder.add(saveTerDesc).xy(5, 5);

        panel = builder.build();
    }

    public Component getPanel() {
        return panel;
    }

    public void setEnabled(boolean enabled) {
        savePriSort.setEnabled(enabled);
        savePriDesc.setEnabled(enabled);
        saveSecSort.setEnabled(enabled);
        saveSecDesc.setEnabled(enabled);
        saveTerSort.setEnabled(enabled);
        saveTerDesc.setEnabled(enabled);
    }

    public void setSaveOrderConfig(SaveOrderConfig saveOrderConfig) {
        Objects.requireNonNull(saveOrderConfig);

        savePriSort.setSelectedItem(saveOrderConfig.sortCriteria[0].field);
        savePriDesc.setSelected(saveOrderConfig.sortCriteria[0].descending);
        saveSecSort.setSelectedItem(saveOrderConfig.sortCriteria[1].field);
        saveSecDesc.setSelected(saveOrderConfig.sortCriteria[1].descending);
        saveTerSort.setSelectedItem(saveOrderConfig.sortCriteria[2].field);
        saveTerDesc.setSelected(saveOrderConfig.sortCriteria[2].descending);

    }

    public SaveOrderConfig getSaveOrderConfig() {
        SaveOrderConfig saveOrderConfig = new SaveOrderConfig();
        saveOrderConfig.sortCriteria[0].field = getSelectedItemAsLowerCaseTrim(savePriSort);
        saveOrderConfig.sortCriteria[0].descending = savePriDesc.isSelected();
        saveOrderConfig.sortCriteria[1].field = getSelectedItemAsLowerCaseTrim(saveSecSort);
        saveOrderConfig.sortCriteria[1].descending = saveSecDesc.isSelected();
        saveOrderConfig.sortCriteria[2].field = getSelectedItemAsLowerCaseTrim(saveTerSort);
        saveOrderConfig.sortCriteria[2].descending = saveTerDesc.isSelected();
        return saveOrderConfig;
    }

    private String getSelectedItemAsLowerCaseTrim(JComboBox<String> sortBox) {
        return sortBox.getSelectedItem().toString().toLowerCase().trim();
    }
}

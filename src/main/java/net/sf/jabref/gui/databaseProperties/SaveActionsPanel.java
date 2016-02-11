package net.sf.jabref.gui.databaseProperties;

import net.sf.jabref.MetaData;
import net.sf.jabref.exporter.SaveActions;
import net.sf.jabref.logic.l10n.Localization;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.List;

public class SaveActionsPanel extends JPanel {

    private JCheckBox enabled;

    private SaveActions saveActions;

    public SaveActionsPanel() {

        enabled = new JCheckBox(Localization.lang("Enable save actions"));

    }

    public void setValues(MetaData metaData) {
        saveActions = new SaveActions(metaData);

        enabled.setSelected(saveActions.isEnabled());

        this.setLayout(new GridLayout(2, 1));
        this.add(enabled);
        this.add(getSelectorPanel());
    }

    private JPanel getSelectorPanel(){
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(1, 3));

        JTextField keyField = new JTextField(20);
        panel.add(keyField);

        List<String> formatterNames = saveActions.getAvailableFormatters().stream().map(formatter -> formatter.getKey()).collect(Collectors.toList());
        JComboBox formatters = new JComboBox(formatterNames.toArray());
        panel.add(formatters);

        JButton addButton = new JButton(Localization.lang("Add"));
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });
        panel.add(addButton);

        return panel;
    }

    public boolean storeSetting(MetaData metaData) {
        java.util.List<String> actions = new ArrayList<>();

        if(enabled.isSelected()) {
            actions.add("enabled;");
        } else {
            actions.add("disabled;");
        }

        metaData.putData(SaveActions.META_KEY, actions);

        boolean hasChanged = saveActions.equals(new SaveActions((metaData)));

        return hasChanged;
    }
}

package net.sf.jabref.gui.databaseProperties;

import net.sf.jabref.MetaData;
import net.sf.jabref.exporter.SaveActions;
import net.sf.jabref.logic.l10n.Localization;

import javax.swing.*;
import java.awt.*;

public class SaveActionsPanel extends JPanel {

    private JCheckBox enabled;

    private SaveActions saveActions;

    public SaveActionsPanel() {

        enabled = new JCheckBox(Localization.lang("Enable save actions"));

    }

    public void setValues(MetaData metaData) {
        saveActions = new SaveActions(metaData);

        enabled.setSelected(saveActions.isEnabled());

        this.setLayout(new GridLayout(1, 1));
        this.add(enabled);
    }
}

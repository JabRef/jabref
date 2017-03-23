package org.jabref.gui.preftabs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.jabref.gui.SaveOrderConfigDisplay;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Preference tab for file sorting options.
 */
class ExportSortingPrefsTab extends JPanel implements PrefsTab {

    private final JabRefPreferences prefs;

    private final JRadioButton exportInOriginalOrder;
    private final JRadioButton exportInTableOrder;
    private final JRadioButton exportInSpecifiedOrder;
    private final SaveOrderConfigDisplay exportOrderPanel;


    public ExportSortingPrefsTab(JabRefPreferences prefs) {
        this.prefs = prefs;
        FormLayout layout = new FormLayout("4dlu, left:pref, 4dlu, fill:pref", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.leadingColumnOffset(1);

        // EXPORT SORT ORDER
        // create Components
        exportInOriginalOrder = new JRadioButton(Localization.lang("Export entries in their original order"));
        exportInTableOrder = new JRadioButton(Localization.lang("Export in current table sort order"));
        exportInSpecifiedOrder = new JRadioButton(Localization.lang("Export entries ordered as specified"));

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(exportInOriginalOrder);
        buttonGroup.add(exportInTableOrder);
        buttonGroup.add(exportInSpecifiedOrder);

        ActionListener listener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                boolean selected = e.getSource() == exportInSpecifiedOrder;
                exportOrderPanel.setEnabled(selected);
            }
        };
        exportInOriginalOrder.addActionListener(listener);
        exportInTableOrder.addActionListener(listener);
        exportInSpecifiedOrder.addActionListener(listener);

        // create GUI
        builder.appendSeparator(Localization.lang("Export sort order"));
        builder.append(exportInOriginalOrder, 1);
        builder.nextLine();
        builder.append(exportInTableOrder, 1);
        builder.nextLine();
        builder.append(exportInSpecifiedOrder, 1);
        builder.nextLine();

        exportOrderPanel = new SaveOrderConfigDisplay();
        builder.append(exportOrderPanel.getPanel());
        builder.nextLine();

        // COMBINE EVERYTHING
        JPanel pan = builder.getPanel();
        pan.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        setLayout(new BorderLayout());
        add(pan, BorderLayout.CENTER);
    }

    @Override
    public void setValues() {
        if (prefs.getBoolean(JabRefPreferences.EXPORT_IN_ORIGINAL_ORDER)) {
            exportInOriginalOrder.setSelected(true);
        } else if (prefs.getBoolean(JabRefPreferences.EXPORT_IN_SPECIFIED_ORDER)) {
            exportInSpecifiedOrder.setSelected(true);
        } else {
            exportInTableOrder.setSelected(true);
        }

        boolean selected = prefs.getBoolean(JabRefPreferences.EXPORT_IN_SPECIFIED_ORDER);
        exportOrderPanel.setEnabled(selected);
        exportOrderPanel.setSaveOrderConfig(prefs.loadExportSaveOrder());
    }

    @Override
    public void storeSettings() {
        prefs.putBoolean(JabRefPreferences.EXPORT_IN_ORIGINAL_ORDER, exportInOriginalOrder.isSelected());
        prefs.putBoolean(JabRefPreferences.EXPORT_IN_SPECIFIED_ORDER, exportInSpecifiedOrder.isSelected());

        prefs.storeExportSaveOrder(exportOrderPanel.getSaveOrderConfig());
    }

    @Override
    public boolean validateSettings() {
        return true;
    }

    @Override
    public String getTabName() {
        return Localization.lang("Export sorting");
    }
}

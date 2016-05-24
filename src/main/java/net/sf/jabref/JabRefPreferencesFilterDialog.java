package net.sf.jabref;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;
import java.util.Objects;

import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import net.sf.jabref.gui.WrapLayout;
import net.sf.jabref.logic.l10n.Localization;

public class JabRefPreferencesFilterDialog extends JDialog {

    private final JabRefPreferencesFilter preferencesFilter;

    private final JTable table;
    private final JCheckBox showOnlyDeviatingPreferenceOptions;
    private final JLabel count;

    public JabRefPreferencesFilterDialog(JabRefPreferencesFilter preferencesFilter, JFrame frame) {
        super(frame, true); // is modal

        this.preferencesFilter = Objects.requireNonNull(preferencesFilter);

        this.setTitle(Localization.lang("Preferences"));
        this.setSize(new Dimension(800, 600));

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        JPanel northPanel = new JPanel();
        northPanel.setLayout(new WrapLayout(WrapLayout.LEFT));
        showOnlyDeviatingPreferenceOptions = new JCheckBox(Localization.lang("Show only preferences deviating from their default value"), false);
        showOnlyDeviatingPreferenceOptions.addChangeListener(x -> updateModel());
        northPanel.add(showOnlyDeviatingPreferenceOptions);
        count = new JLabel();
        northPanel.add(count);
        panel.add(northPanel, BorderLayout.NORTH);

        table = new JTable();
        table.setAutoCreateRowSorter(true);
        updateModel();
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        this.getContentPane().add(panel);
    }

    private void updateModel() {
        List<JabRefPreferencesFilter.PreferenceOption> preferenceOptions;

        if (showOnlyDeviatingPreferenceOptions.isSelected()) {
            preferenceOptions = preferencesFilter.getDeviatingPreferences();
        } else {
            preferenceOptions = preferencesFilter.getPreferenceOptions();
        }

        table.setModel(new PreferencesTableModel(preferenceOptions));
        count.setText(String.format("(%d)", preferenceOptions.size()));
    }

    private static class PreferencesTableModel extends AbstractTableModel {

        private final List<JabRefPreferencesFilter.PreferenceOption> preferences;

        public PreferencesTableModel(List<JabRefPreferencesFilter.PreferenceOption> preferences) {
            this.preferences = Objects.requireNonNull(preferences);
        }

        @Override
        public String getColumnName(int column) {
            if (column == 0) {
                return Localization.lang("type");
            } else if (column == 1) {
                return Localization.lang("key");
            } else if (column == 2) {
                return Localization.lang("value");
            } else if (column == 3) {
                return Localization.lang("default");
            } else {
                return "n/a";
            }
        }

        @Override
        public int getRowCount() {
            return preferences.size();
        }

        @Override
        public int getColumnCount() {
            return 4;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex < 0 || rowIndex - 1 > preferences.size()) {
                return "n/a";
            }

            JabRefPreferencesFilter.PreferenceOption preferenceOption = preferences.get(rowIndex);
            if (columnIndex == 0) {
                return preferenceOption.getType();
            } else if (columnIndex == 1) {
                return preferenceOption.getKey();
            } else if (columnIndex == 2) {
                return preferenceOption.getValue();
            } else if (columnIndex == 3) {
                return preferenceOption.getDefaultValue().orElse("NULL");
            } else {
                return "n/a";
            }
        }
    }

}

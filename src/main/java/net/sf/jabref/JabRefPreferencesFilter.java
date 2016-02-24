package net.sf.jabref;

import net.sf.jabref.gui.WrapLayout;
import net.sf.jabref.logic.l10n.Localization;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class JabRefPreferencesFilter {

    public static class PreferencesTableModel extends AbstractTableModel {

        private final List<PreferenceOption> preferences;

        public PreferencesTableModel(List<PreferenceOption> preferences) {
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

            PreferenceOption preferenceOption = preferences.get(rowIndex);
            if (columnIndex == 0) {
                return preferenceOption.type;
            } else if (columnIndex == 1) {
                return preferenceOption.key;
            } else if (columnIndex == 2) {
                return preferenceOption.value;
            } else if (columnIndex == 3) {
                return preferenceOption.defaultValue.orElse("NULL");
            } else {
                return "n/a";
            }
        }
    }

    public static class JabRefPreferencesDialog extends JDialog {

        private final JabRefPreferencesFilter preferencesFilter;

        private final JTable table;
        private final JCheckBox showOnlyDeviatingPreferenceOptions;
        private final JLabel count;

        public JabRefPreferencesDialog(JabRefPreferencesFilter preferencesFilter, JFrame frame) {
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
            List<PreferenceOption> preferenceOptions;

            if(showOnlyDeviatingPreferenceOptions.isSelected()) {
                preferenceOptions = preferencesFilter.getDeviatingPreferences();
            } else {
                preferenceOptions = preferencesFilter.getPreferenceOptions();
            }

            table.setModel(new PreferencesTableModel(preferenceOptions));
            count.setText(String.format("(%d)", preferenceOptions.size()));
        }

    }

    public enum PreferenceType {
        BOOLEAN, INTEGER, STRING
    }

    public static class PreferenceOption implements Comparable<PreferenceOption> {

        private final String key;
        private final Object value;
        private final Optional<Object> defaultValue;
        private final PreferenceType type;

        public PreferenceOption(String key, Object value, Object defaultValue) {
            this.key = Objects.requireNonNull(key);
            this.value = Objects.requireNonNull(value);
            this.defaultValue = Optional.ofNullable(defaultValue);
            this.type = Objects.requireNonNull(getType(value));

            if (defaultValue != null && !Objects.equals(this.type, getType(defaultValue))) {
                throw new IllegalStateException("types must match between default value and value");
            }
        }

        private PreferenceType getType(Object value) {
            if (value instanceof Boolean) {
                return PreferenceType.BOOLEAN;
            } else if (value instanceof Integer) {
                return PreferenceType.INTEGER;
            } else {
                return PreferenceType.STRING;
            }
        }

        public boolean isUnchanged() {
            return Objects.equals(value, defaultValue.orElse(null));
        }

        public boolean isChanged() {
            return !isUnchanged();
        }

        public String toString() {
            return String.format("%s: %s=%s (%s)", type, key, value, defaultValue.orElse("NULL"));
        }

        @Override
        public int compareTo(PreferenceOption o) {
            return Objects.compare(this.key, o.key, String::compareTo);
        }
    }

    private final JabRefPreferences preferences;

    public JabRefPreferencesFilter(JabRefPreferences preferences) {
        this.preferences = preferences;
    }

    public List<PreferenceOption> getPreferenceOptions() {
        Map<String, Object> defaults = new HashMap<>(preferences.defaults);
        Map<String, Object> prefs = preferences.getPreferences();

        return prefs.entrySet().stream()
                .map(entry -> new PreferenceOption(entry.getKey(), entry.getValue(), defaults.get(entry.getKey())))
                .collect(Collectors.toList());
    }

    public List<PreferenceOption> getDeviatingPreferences() {
        return getPreferenceOptions().stream()
                .filter(PreferenceOption::isChanged)
                .sorted()
                .collect(Collectors.toList());
    }

}

package net.sf.jabref;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class JabRefPreferencesFilter {

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

            if ((defaultValue != null) && !Objects.equals(this.type, getType(defaultValue))) {
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

        @Override
        public String toString() {
            return String.format("%s: %s=%s (%s)", type, key, value, defaultValue.orElse("NULL"));
        }

        public String getKey() {
            return key;
        }

        public Object getValue() {
            return value;
        }

        public Optional<Object> getDefaultValue() {
            return defaultValue;
        }

        public PreferenceType getType() {
            return type;
        }

        @Override
        public int compareTo(PreferenceOption o) {
            return Objects.compare(this.key, o.key, String::compareTo);
        }
    }

}

package org.jabref.logic.l10n;

import java.nio.file.Path;
import java.util.Objects;

/// Representation of a localization key required for testing
class LocalizationEntry implements Comparable<LocalizationEntry> {

    private final Path path;
    private final String key;

    LocalizationEntry(Path path, String key) {
        this.path = path;
        this.key = key;
    }

    public Path getPath() {
        return path;
    }

    public String getKey() {
        return key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        LocalizationEntry that = (LocalizationEntry) o;
        return Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }

    @Override
    public String toString() {
        return "%s (%s)".formatted(key, path);
    }

    @Override
    public int compareTo(LocalizationEntry o) {
        return key.compareTo(o.key);
    }
}

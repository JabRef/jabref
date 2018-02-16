package org.jabref.logic.l10n;

import java.nio.file.Path;
import java.util.Objects;

class LocalizationEntry implements Comparable<LocalizationEntry>{

    private final Path path;
    private final String key;
    private final LocalizationBundleForTest bundle;

    LocalizationEntry(Path path, String key, LocalizationBundleForTest bundle) {
        this.path = path;
        this.key = key;
        this.bundle = bundle;
    }

    public Path getPath() {
        return path;
    }

    public String getKey() {
        return key;
    }

    public String getId() {
        return String.format("%s___%s", bundle, key);
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

        if (key != null ? !key.equals(that.key) : that.key != null) {
            return false;
        }
        return bundle == that.bundle;

    }

    @Override
    public int hashCode() {
        return Objects.hash(key, bundle);
    }

    public LocalizationBundleForTest getBundle() {
        return bundle;
    }

    @Override
    public String toString() {
        return String.format("%s (%s %s)", key, path, bundle);
    }

    @Override
    public int compareTo(LocalizationEntry o) {
        return getId().compareTo(o.getId());
    }
}

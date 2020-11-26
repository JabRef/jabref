package org.jabref.model.study;

public class LibraryEntry {
    private String name;
    private boolean enabled;

    public LibraryEntry(String name, boolean enabled) {
        this.name = name;
        this.enabled = enabled;
    }

    /**
     * Used for Jackson deserialization
     */
    public LibraryEntry() {
        // Per default fetcher is activated
        this.enabled = true;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LibraryEntry that = (LibraryEntry) o;

        if (isEnabled() != that.isEnabled()) {
            return false;
        }
        return getName() != null ? getName().equals(that.getName()) : that.getName() == null;
    }

    @Override
    public int hashCode() {
        int result = getName() != null ? getName().hashCode() : 0;
        result = 31 * result + (isEnabled() ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "LibraryEntry{" +
                "name='" + name + '\'' +
                ", enabled=" + enabled +
                '}';
    }
}

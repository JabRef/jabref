package org.jabref.preferences;

public interface PreferencesReadWriteService<T> extends PreferencesReadService<T> {

    public void store(T preferences);
}

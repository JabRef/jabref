package org.jabref.logic.protectedterms;

import java.util.List;

public class ProtectedTermsPreferences {

    private final List<String> enabledInternalTermLists;
    private final List<String> enabledExternalTermLists;
    private final List<String> disabledInternalTermLists;
    private final List<String> disabledExternalTermLists;


    public ProtectedTermsPreferences(List<String> enabledInternalTermLists, List<String> enabledExternalTermLists,
            List<String> disabledInternalTermLists, List<String> disabledExternalTermLists) {
        this.enabledInternalTermLists = enabledInternalTermLists;
        this.disabledInternalTermLists = disabledInternalTermLists;
        this.enabledExternalTermLists = enabledExternalTermLists;
        this.disabledExternalTermLists = disabledExternalTermLists;
    }

    public List<String> getEnabledInternalTermLists() {
        return enabledInternalTermLists;
    }

    public List<String> getEnabledExternalTermLists() {
        return enabledExternalTermLists;
    }

    public List<String> getDisabledInternalTermLists() {
        return disabledInternalTermLists;
    }

    public List<String> getDisabledExternalTermLists() {
        return disabledExternalTermLists;
    }

}

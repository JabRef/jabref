package net.sf.jabref.logic.protectedterms;

import java.util.List;

import net.sf.jabref.preferences.JabRefPreferences;

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

    public static ProtectedTermsPreferences fromPreferences(JabRefPreferences jabRefPreferences) {
        return new ProtectedTermsPreferences(
                jabRefPreferences.getStringList(JabRefPreferences.PROTECTED_TERMS_ENABLED_INTERNAL),
                jabRefPreferences.getStringList(JabRefPreferences.PROTECTED_TERMS_ENABLED_EXTERNAL),
                jabRefPreferences.getStringList(JabRefPreferences.PROTECTED_TERMS_DISABLED_INTERNAL),
                jabRefPreferences.getStringList(JabRefPreferences.PROTECTED_TERMS_DISABLED_EXTERNAL));
    }

    public static void toPreferences(JabRefPreferences jabRefPreferences, List<String> enabledInternalTermLists,
            List<String> enabledExternalTermLists, List<String> disabledInternalTermLists,
            List<String> disabledExternalTermLists) {
        jabRefPreferences.putStringList(JabRefPreferences.PROTECTED_TERMS_ENABLED_EXTERNAL, enabledExternalTermLists);
        jabRefPreferences.putStringList(JabRefPreferences.PROTECTED_TERMS_DISABLED_EXTERNAL, disabledExternalTermLists);
        jabRefPreferences.putStringList(JabRefPreferences.PROTECTED_TERMS_ENABLED_INTERNAL, enabledInternalTermLists);
        jabRefPreferences.putStringList(JabRefPreferences.PROTECTED_TERMS_DISABLED_INTERNAL, disabledInternalTermLists);

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

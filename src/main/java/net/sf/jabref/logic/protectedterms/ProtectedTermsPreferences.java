package net.sf.jabref.logic.protectedterms;

import java.util.ArrayList;
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

    public static void toPreferences(JabRefPreferences jabRefPreferences, ProtectedTermsLoader loader) {
        List<String> enabledExternalList = new ArrayList<>();
        List<String> disabledExternalList = new ArrayList<>();
        List<String> enabledInternalList = new ArrayList<>();
        List<String> disabledInternalList = new ArrayList<>();

        for (ProtectedTermsList list : loader.getProtectedTermsLists()) {
            if (list.isInternalList()) {
                if (list.isEnabled()) {
                    enabledInternalList.add(list.getLocation());
                } else {
                    disabledInternalList.add(list.getLocation());
                }
            } else {
                if (list.isEnabled()) {
                    enabledExternalList.add(list.getLocation());
                } else {
                    disabledExternalList.add(list.getLocation());
                }
            }
        }

        jabRefPreferences.putStringList(JabRefPreferences.PROTECTED_TERMS_ENABLED_EXTERNAL, enabledExternalList);
        jabRefPreferences.putStringList(JabRefPreferences.PROTECTED_TERMS_DISABLED_EXTERNAL, disabledExternalList);
        jabRefPreferences.putStringList(JabRefPreferences.PROTECTED_TERMS_ENABLED_INTERNAL, enabledInternalList);
        jabRefPreferences.putStringList(JabRefPreferences.PROTECTED_TERMS_DISABLED_INTERNAL, disabledInternalList);

    }
}

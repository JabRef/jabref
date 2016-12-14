package net.sf.jabref.migrations;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.customentrytypes.CustomEntryTypesManager;
import net.sf.jabref.model.EntryTypes;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.CustomEntryType;
import net.sf.jabref.model.strings.StringUtil;
import net.sf.jabref.preferences.JabRefPreferences;

class CustomEntryTypePreferenceMigration {

    //non-default preferences
    private static final String CUSTOM_TYPE_NAME = "customTypeName_";
    private static final String CUSTOM_TYPE_REQ = "customTypeReq_";
    private static final String CUSTOM_TYPE_OPT = "customTypeOpt_";
    private static final String CUSTOM_TYPE_PRIOPT = "customTypePriOpt_";

    private static JabRefPreferences prefs = Globals.prefs;

    static void upgradeStoredCustomEntryTypes(BibDatabaseMode defaultBibDatabaseMode) {
        List<CustomEntryType> storedOldTypes = new ArrayList<>();

        int number = 0;
        Optional<CustomEntryType> type;
        while ((type = getCustomEntryType(number)).isPresent()) {
            EntryTypes.addOrModifyCustomEntryType(type.get(), defaultBibDatabaseMode);
            storedOldTypes.add(type.get());
            number++;
        }

        CustomEntryTypesManager.saveCustomEntryTypes(prefs);
    }

    /**
     * Retrieves all deprecated information about the entry type in preferences, with the tag given by number.
     *
     * (old implementation which has been copied)
     */
    private static Optional<CustomEntryType> getCustomEntryType(int number) {
        String nr = String.valueOf(number);
        String name = prefs.get(CUSTOM_TYPE_NAME + nr);
        if (name == null) {
            return Optional.empty();
        }
        List<String> req = prefs.getStringList(CUSTOM_TYPE_REQ + nr);
        List<String> opt = prefs.getStringList(CUSTOM_TYPE_OPT + nr);
        List<String> priOpt = prefs.getStringList(CUSTOM_TYPE_PRIOPT + nr);
        if (priOpt.isEmpty()) {
            return Optional.of(new CustomEntryType(StringUtil.capitalizeFirst(name), req, opt));
        }
        List<String> secondary = new ArrayList<>(opt);
        secondary.removeAll(priOpt);

        return Optional.of(new CustomEntryType(StringUtil.capitalizeFirst(name), req, priOpt, secondary));

    }
}

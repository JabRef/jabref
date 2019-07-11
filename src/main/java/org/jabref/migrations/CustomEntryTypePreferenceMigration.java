package org.jabref.migrations;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.Globals;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.strings.StringUtil;
import org.jabref.preferences.JabRefPreferences;

class BibEntryTypePreferenceMigration {

    //non-default preferences
    private static final String CUSTOM_TYPE_NAME = "customTypeName_";
    private static final String CUSTOM_TYPE_REQ = "customTypeReq_";
    private static final String CUSTOM_TYPE_OPT = "customTypeOpt_";
    private static final String CUSTOM_TYPE_PRIOPT = "customTypePriOpt_";

    private static JabRefPreferences prefs = Globals.prefs;

    private BibEntryTypePreferenceMigration() {
    }

    static void upgradeStoredBibEntryTypes(BibDatabaseMode defaultBibDatabaseMode) {
        List<BibEntryType> storedOldTypes = new ArrayList<>();

        int number = 0;
        Optional<BibEntryType> type;
        while ((type = getBibEntryType(number)).isPresent()) {
            BibEntryTypesManager.addOrModifyBibEntryType(type.get(), defaultBibDatabaseMode);
            storedOldTypes.add(type.get());
            number++;
        }

        prefs.saveBibEntryTypes();
    }

    /**
     * Retrieves all deprecated information about the entry type in preferences, with the tag given by number.
     *
     * (old implementation which has been copied)
     */
    private static Optional<BibEntryType> getBibEntryType(int number) {
        String nr = String.valueOf(number);
        String name = prefs.get(CUSTOM_TYPE_NAME + nr);
        if (name == null) {
            return Optional.empty();
        }
        List<String> req = prefs.getStringList(CUSTOM_TYPE_REQ + nr);
        List<String> opt = prefs.getStringList(CUSTOM_TYPE_OPT + nr);
        List<String> priOpt = prefs.getStringList(CUSTOM_TYPE_PRIOPT + nr);
        if (priOpt.isEmpty()) {
            return Optional.of(new BibEntryType(StringUtil.capitalizeFirst(name), req, opt));
        }
        List<String> secondary = new ArrayList<>(opt);
        secondary.removeAll(priOpt);

        return Optional.of(new BibEntryType(StringUtil.capitalizeFirst(name), req, priOpt, secondary));

    }
}

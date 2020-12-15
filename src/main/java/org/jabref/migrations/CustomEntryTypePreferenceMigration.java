package org.jabref.migrations;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.gui.Globals;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypeBuilder;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.types.EntryTypeFactory;
import org.jabref.preferences.JabRefPreferences;

class CustomEntryTypePreferenceMigration {

    // non-default preferences
    private static final String CUSTOM_TYPE_NAME = "customTypeName_";
    private static final String CUSTOM_TYPE_REQ = "customTypeReq_";
    private static final String CUSTOM_TYPE_OPT = "customTypeOpt_";
    private static final String CUSTOM_TYPE_PRIOPT = "customTypePriOpt_";

    private static JabRefPreferences prefs = Globals.prefs;

    private CustomEntryTypePreferenceMigration() {
    }

    static void upgradeStoredBibEntryTypes(BibDatabaseMode defaultBibDatabaseMode) {
        List<BibEntryType> storedOldTypes = new ArrayList<>();

        int number = 0;
        Optional<BibEntryType> type;
        while ((type = getBibEntryType(number)).isPresent()) {
            Globals.entryTypesManager.addCustomOrModifiedType(type.get(), defaultBibDatabaseMode);
            storedOldTypes.add(type.get());
            number++;
        }

        prefs.storeCustomEntryTypes(Globals.entryTypesManager);
    }

    /**
     * Retrieves all deprecated information about the entry type in preferences, with the tag given by number.
     * <p>
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

        BibEntryTypeBuilder entryTypeBuilder = new BibEntryTypeBuilder()
                .withType(EntryTypeFactory.parse(name))
                .withRequiredFields(req.stream().map(FieldFactory::parseOrFields).collect(Collectors.toList()));
        if (priOpt.isEmpty()) {
            entryTypeBuilder = entryTypeBuilder
                    .withImportantFields(opt.stream().map(FieldFactory::parseField).collect(Collectors.toSet()));
            return Optional.of(entryTypeBuilder.build());
        } else {
            List<String> secondary = new ArrayList<>(opt);
            secondary.removeAll(priOpt);

            entryTypeBuilder = entryTypeBuilder
                    .withImportantFields(priOpt.stream().map(FieldFactory::parseField).collect(Collectors.toSet()))
                    .withDetailFields(secondary.stream().map(FieldFactory::parseField).collect(Collectors.toSet()));
            return Optional.of(entryTypeBuilder.build());
        }
    }
}

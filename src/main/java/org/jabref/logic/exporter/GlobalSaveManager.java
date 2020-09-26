package org.jabref.logic.exporter;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import org.jabref.gui.BasePanel;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.DelayTaskThrottler;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.preferences.JabRefPreferences;

public class GlobalSaveManager {

    private static Set<GlobalSaveManager> runningInstances = new HashSet<>();

    private final DelayTaskThrottler<Set<Character>> throttler = new DelayTaskThrottler<>(1500);
    private final BibDatabaseContext bibDatabaseContext;
    private final List<BibEntry> selectedEntries;
    private final JabRefPreferences preferences;
    private final BibEntryTypesManager entryTypesManager;

    private GlobalSaveManager(BasePanel panel, JabRefPreferences preferences, BibEntryTypesManager entryTypesManager) {
        this.bibDatabaseContext = panel.getBibDatabaseContext();
        this.selectedEntries = panel.getSelectedEntries();
        this.preferences = preferences;
        this.entryTypesManager = entryTypesManager;

    }

    public static void shutdown(BibDatabaseContext context) {
        runningInstances.stream().filter(instance -> instance.bibDatabaseContext == context).forEach(GlobalSaveManager::shutdown);
        runningInstances.removeIf(instance -> instance.bibDatabaseContext == context);
    }

    public static GlobalSaveManager create(BasePanel panel, JabRefPreferences preferences, BibEntryTypesManager entryTypesManager) {
        GlobalSaveManager saveAction = new GlobalSaveManager(panel, preferences, entryTypesManager);
        runningInstances.add(saveAction);
        return saveAction;
    }

    private void shutdown() {
        this.throttler.shutdown();

    }

    public Future<Set<Character>> save(Path file, boolean selectedOnly, Charset encoding, SavePreferences.DatabaseSaveType saveType, BibDatabaseContext context, Consumer<List<FieldChange>> consumeFieldChanges) throws SaveException {
        return throttler.scheduleTask(() -> saveThrotteld(file, selectedOnly, encoding, saveType, context, consumeFieldChanges));
    }

    private Set<Character> saveThrotteld(Path file, boolean selectedOnly, Charset encoding, SavePreferences.DatabaseSaveType saveType, BibDatabaseContext context, Consumer<List<FieldChange>> consumeFieldChanges) throws SaveException {
        SavePreferences savePrefs = this.preferences.getSavePreferences()
                                                    .withEncoding(encoding)
                                                    .withSaveType(saveType);

        try (AtomicFileWriter fileWriter = new AtomicFileWriter(file, savePrefs.getEncoding(), savePrefs.shouldMakeBackup())) {
            BibtexDatabaseWriter databaseWriter = new BibtexDatabaseWriter(fileWriter, savePrefs, entryTypesManager);

            if (selectedOnly) {
                databaseWriter.savePartOfDatabase(context, selectedEntries);
            } else {
                databaseWriter.saveDatabase(context);
            }

            consumeFieldChanges.accept(databaseWriter.getSaveActionsFieldChanges());

            if (fileWriter.hasEncodingProblems()) {
                return fileWriter.getEncodingProblems();
            }
        } catch (UnsupportedCharsetException ex) {
            throw new SaveException(Localization.lang("Character encoding '%0' is not supported.", encoding.displayName()), ex);
        } catch (IOException ex) {
            throw new SaveException("Problems saving: " + ex, ex);
        }

        return Collections.emptySet();
    }
}

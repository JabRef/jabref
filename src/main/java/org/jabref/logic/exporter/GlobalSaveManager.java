package org.jabref.logic.exporter;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import org.jabref.gui.Globals;
import org.jabref.gui.StateManager;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.DelayTaskThrottler;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlobalSaveManager {

    private static Set<GlobalSaveManager> runningInstances = new HashSet<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalSaveManager.class);

    private final DelayTaskThrottler<Set<Character>> throttler = new DelayTaskThrottler<>(1500);
    private final BibDatabaseContext bibDatabaseContext;

    private final StateManager stateManager;

    private final PreferencesService preferencesService;

    private GlobalSaveManager(StateManager stateManager, PreferencesService preferencesService, BibEntryTypesManager entryTypesManager) {
        this.bibDatabaseContext = stateManager.getActiveDatabase().get();
        this.stateManager = stateManager;
        this.preferencesService = preferencesService;

    }

    public static void shutdown(BibDatabaseContext context) {
        runningInstances.stream().filter(instance -> instance.bibDatabaseContext == context).forEach(GlobalSaveManager::shutdown);
        runningInstances.removeIf(instance -> instance.bibDatabaseContext == context);
    }

    public static GlobalSaveManager start(StateManager stateManager, PreferencesService preferencesService, BibEntryTypesManager entryTypesManager) {
        GlobalSaveManager saveAction = new GlobalSaveManager(stateManager, preferencesService, entryTypesManager);

        if(runningInstances.contains(saveAction))
        {
           LOGGER.debug("I have an instance "+saveAction);
        }
        runningInstances.add(saveAction);
        return saveAction;
    }

    private void shutdown() {
        this.throttler.shutdown();

    }

    public Future<Set<Character>> save(Path file, boolean selectedOnly, Charset encoding, SavePreferences.DatabaseSaveType saveType, Consumer<List<FieldChange>> consumeFieldChanges) {
        return throttler.scheduleTask(() -> saveThrottled(file, selectedOnly, encoding, saveType, consumeFieldChanges));
    }

    private Set<Character> saveThrottled(Path file, boolean selectedOnly, Charset encoding, SavePreferences.DatabaseSaveType saveType, Consumer<List<FieldChange>> consumeFieldChanges) throws SaveException {

        SavePreferences savePrefs = this.preferencesService.getSavePreferences()
                                                           .withEncoding(encoding)
                                                           .withSaveType(saveType);

        try (AtomicFileWriter fileWriter = new AtomicFileWriter(file, savePrefs.getEncoding(), savePrefs.shouldMakeBackup())) {
            BibtexDatabaseWriter databaseWriter = new BibtexDatabaseWriter(fileWriter, savePrefs, Globals.entryTypesManager);

            if (selectedOnly) {
                databaseWriter.savePartOfDatabase(this.bibDatabaseContext, this.stateManager.getSelectedEntries());
            } else {
                databaseWriter.saveDatabase(this.bibDatabaseContext);
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

    @Override
    public String toString() {
        return "Global save manager for " + bibDatabaseContext;
    }

    @Override
    public int hashCode() {
       return Objects.hash(bibDatabaseContext);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }
        GlobalSaveManager other = (GlobalSaveManager) o;
        return Objects.equals(bibDatabaseContext, other.bibDatabaseContext);
    }


}

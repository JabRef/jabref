package org.jabref.logic.exporter;

import java.io.IOException;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Future;

import org.jabref.gui.Globals;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.DelayTaskThrottler;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlobalSaveManager {

    private static Set<GlobalSaveManager> runningInstances = new HashSet<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalSaveManager.class);

    private static DelayTaskThrottler<SaveResult> throttler = new DelayTaskThrottler<>(1500);
    private final BibDatabaseContext bibDatabaseContext;

    private GlobalSaveManager(BibDatabaseContext context) {
        this.bibDatabaseContext = context;
    }

    public static void shutdown(BibDatabaseContext context) {
        runningInstances.stream().filter(instance -> instance.bibDatabaseContext == context).forEach(GlobalSaveManager::shutdown);
        runningInstances.removeIf(instance -> instance.bibDatabaseContext == context);
    }

    public static GlobalSaveManager start(BibDatabaseContext context) {
        GlobalSaveManager saveAction = new GlobalSaveManager(context);

        if (runningInstances.contains(saveAction)) {
            LOGGER.debug("I have an instance " + saveAction);
        }
        runningInstances.add(saveAction);
        return saveAction;
    }

    private void shutdown() {
        this.throttler.shutdown();
    }

    public Future<SaveResult> save(Path file, boolean selectedOnly, List<BibEntry> entries, SavePreferences savePrefs) {
        return throttler.scheduleTask(() -> saveThrottled(file, selectedOnly, entries, savePrefs));
    }

    private SaveResult saveThrottled(Path file, boolean selectedOnly, List<BibEntry> entries, SavePreferences savePrefs) throws SaveException {

        try (AtomicFileWriter fileWriter = new AtomicFileWriter(file, savePrefs.getEncoding(), savePrefs.shouldMakeBackup())) {
            BibtexDatabaseWriter databaseWriter = new BibtexDatabaseWriter(fileWriter, savePrefs, Globals.entryTypesManager);

            if (selectedOnly) {
                databaseWriter.savePartOfDatabase(this.bibDatabaseContext, entries);
            } else {
                databaseWriter.saveDatabase(this.bibDatabaseContext);
            }

            var saveResult = new SaveResult(fileWriter.getEncodingProblems(), databaseWriter.getSaveActionsFieldChanges());
            return saveResult;

        } catch (UnsupportedCharsetException ex) {
            throw new SaveException(Localization.lang("Character encoding '%0' is not supported.", savePrefs.getEncoding().displayName()), ex);
        } catch (IOException ex) {
            throw new SaveException("Problems saving: " + ex, ex);
        }

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

    public class SaveResult {

        boolean success;
        Set<Character> encodingProblems = new HashSet<>();
        List<FieldChange> fieldChanges = new ArrayList<>();

        public SaveResult(Set<Character> encodingProblems, List<FieldChange> fieldChanges) {
            this.encodingProblems = encodingProblems;
            this.fieldChanges = fieldChanges;
        }

        public Set<Character> getEncodingProblems() {
            return this.encodingProblems;
        }

        public List<FieldChange> getFieldChanges() {
            return this.fieldChanges;
        }

    }
}

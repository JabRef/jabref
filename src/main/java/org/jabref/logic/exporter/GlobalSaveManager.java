package org.jabref.logic.exporter;

import java.io.IOException;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

public enum GlobalSaveManager {

    INSTANCE;

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalSaveManager.class);
    private static DelayTaskThrottler<SaveResult> throttler = new DelayTaskThrottler<>(1000);

    public static void shutdown() {
        throttler.shutdown();
    }

    public Future<SaveResult> save(BibDatabaseContext bibDatabaseContext, Path file, boolean selectedOnly, List<BibEntry> entries, SavePreferences savePrefs) {
        return throttler.scheduleTask(() -> saveThrottled(bibDatabaseContext, file, selectedOnly, entries, savePrefs));
    }

    private SaveResult saveThrottled(BibDatabaseContext bibDatabaseContext, Path file, boolean selectedOnly, List<BibEntry> entries, SavePreferences savePrefs) throws SaveException {

        try (JabRefFileWriter fileWriter = new JabRefFileWriter(file, savePrefs.getEncoding())) {
            BibtexDatabaseWriter databaseWriter = new BibtexDatabaseWriter(fileWriter, savePrefs, Globals.entryTypesManager);

            if (selectedOnly) {
                databaseWriter.savePartOfDatabase(bibDatabaseContext, entries);
            } else {
                databaseWriter.saveDatabase(bibDatabaseContext);
            }

            var saveResult = new SaveResult(fileWriter.getEncodingProblems(), databaseWriter.getSaveActionsFieldChanges());
            return saveResult;

        } catch (UnsupportedCharsetException ex) {
            throw new SaveException(Localization.lang("Character encoding '%0' is not supported.", savePrefs.getEncoding().displayName()), ex);
        } catch (IOException ex) {
            throw new SaveException("Problems saving: " + ex, ex);
        }

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

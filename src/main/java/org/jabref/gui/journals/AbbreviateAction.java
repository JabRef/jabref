package org.jabref.gui.journals;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jabref.Globals;
import org.jabref.gui.actions.BaseAction;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

import org.reactfx.util.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts journal full names to either iso or medline abbreviations for all selected entries.
 */
public class AbbreviateAction implements BaseAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbbreviateAction.class);
    private final Supplier<List<BibEntry>> bibEntryListSupplier;
    private final Supplier<UndoableAbbreviator> undoableAbbreviatorSupplier;
    private final Supplier<BibDatabase> databaseSupplier;
    private final Supplier<NamedCompound> compoundSupplier;
    private final AbbreviateActionListener listener;

    public AbbreviateAction(Supplier<List<BibEntry>> bibEntryListSupplier, Supplier<UndoableAbbreviator> undoableAbbreviatorSupplier, Supplier<BibDatabase> databaseSupplier, Supplier<NamedCompound> compoundSupplier, AbbreviateActionListener listener) {
        this.bibEntryListSupplier = bibEntryListSupplier;
        this.undoableAbbreviatorSupplier = undoableAbbreviatorSupplier;
        this.databaseSupplier = databaseSupplier;
        this.compoundSupplier = compoundSupplier;
        this.listener = listener;
    }

    @Override
    public void action() {
        listener.notifyAbbreviationStart();

        // Gather fresh data from suppliers
        List<BibEntry> bibEntryList = bibEntryListSupplier.get();
        BibDatabase database = databaseSupplier.get();
        UndoableAbbreviator undoableAbbreviator = undoableAbbreviatorSupplier.get();
        NamedCompound compound = compoundSupplier.get();

        // Start abbreviating
        CompletableFuture<List<CompletableFuture<AbbreviationResult>>> abbreviationResultListFuture = this.abbreviateEntriesAsync(bibEntryList, database, undoableAbbreviator, compound);
        listener.notifyAbbreviationStarted(abbreviationResultListFuture);
    }

    /**
     * Abbreviates a list of BibEntries.
     *
     * @param entries     The list of entries to abbreviate
     * @param database    The database where the entries are stored in
     * @param abbreviator The abbreviator to use for abbreviation
     * @param compound
     * @return A List of the abbreviation results containing the result of the abbreviation
     */
    private List<AbbreviationResult> abbreviateEntries(List<BibEntry> entries, BibDatabase database, UndoableAbbreviator abbreviator, NamedCompound compound) {
        return abbreviateEntriesAsync(entries, database, abbreviator, compound)
                .join()
                .stream()
                .map(CompletableFuture::join).collect(Collectors.toList());
    }

    private CompletableFuture<List<CompletableFuture<AbbreviationResult>>> abbreviateEntriesAsync(List<BibEntry> entries, BibDatabase database, UndoableAbbreviator abbreviator, NamedCompound compound) {
        LOGGER.debug(String.format("Starting to abbreviate %d entries in total", entries.size()));
        Set<Field> journalNameFields = FieldFactory.getJournalNameFields();
        CompletableFuture<List<CompletableFuture<AbbreviationResult>>> abbreviationResultsFuture;
        abbreviationResultsFuture = BackgroundTask.wrap((Supplier<List<CompletableFuture<AbbreviationResult>>>) () ->
                entries.stream()
                       .map(bibEntry -> abbreviateSingleEntry(bibEntry, abbreviator, database, journalNameFields, compound))
                       .reduce((a, b) -> Lists.concat(a, b)).orElseThrow())
                                                  .executeWith(Globals.TASK_EXECUTOR);
        abbreviationResultsFuture.thenAccept(resultList -> {
            List<AbbreviationResult> abbreviationResults = resultList.stream().map(CompletableFuture::join).collect(Collectors.toList());
            listener.notifyAbbreviationCompleted(abbreviationResults);
        });

        return abbreviationResultsFuture;
    }

    private List<CompletableFuture<AbbreviationResult>> abbreviateSingleEntry(BibEntry bibEntry, UndoableAbbreviator abbreviator, BibDatabase database, Set<Field> journalFields, NamedCompound compound) {
        LOGGER.debug(String.format("Starting to abbreviate BibEntry: '%s'", bibEntry.getId()));
        return journalFields.stream().map(field -> {
            // These tasks should be interruptible (or at least executed in another thread pool).
            // Otherwise there might be some chances for a dead-lock to happen
            CompletableFuture<AbbreviationResult> abbreviationResultCompletableFuture = BackgroundTask.wrap((Supplier<AbbreviationResult>) () -> {
                Boolean result = abbreviator.abbreviate(database, bibEntry, field, compound);
                AbbreviationResult abbreviationResult = new AbbreviationResult(field, bibEntry, result);
                listener.notifyAbbreviationCompleted(abbreviationResult);
                return abbreviationResult;
            }).executeWith(Globals.TASK_EXECUTOR);
            listener.notifySingleAbbreviationStarted(abbreviationResultCompletableFuture);
            return abbreviationResultCompletableFuture;
        }).collect(Collectors.toList());
    }

    public interface AbbreviateActionListener {
        // Add more methods if you need to
        void notifyAbbreviationStart();

        void notifyAbbreviationStarted(CompletableFuture<List<CompletableFuture<AbbreviationResult>>> futureResult);

        void notifySingleAbbreviationStarted(CompletableFuture<AbbreviationResult> futureResult);

        /**
         * Notifies the listener about a single abbreviation that has been completed.
         *
         * @param result The result of the abbreviation
         */
        void notifyAbbreviationCompleted(AbbreviationResult result);

        /**
         * Notifies the listener that the whole abbreviation process has been completed.
         *
         * @param abbreviationResults A List that contains the result of every abbreviation
         */
        void notifyAbbreviationCompleted(List<AbbreviationResult> abbreviationResults);
    }

    public static class AbbreviationResult {

        Field journalField;
        BibEntry entry;
        Boolean result;

        AbbreviationResult(Field journalField, BibEntry entry, Boolean result) {
            this.journalField = journalField;
            this.entry = entry;
            this.result = result;
        }

        public Field getJournalField() {
            return journalField;
        }

        public BibEntry getEntry() {
            return entry;
        }

        public Boolean getResult() {
            return result;
        }
    }

    /**
     * Adapter class which allows to override single methods if you don't want/need to implement all methods
     */
    public static class AbbreviateActionAdapter implements AbbreviateActionListener {

        @Override
        public void notifyAbbreviationStart() {
        }

        @Override
        public void notifyAbbreviationStarted(CompletableFuture<List<CompletableFuture<AbbreviationResult>>> futureResult) {
        }

        @Override
        public void notifySingleAbbreviationStarted(CompletableFuture<AbbreviationResult> futureResult) {
        }

        @Override
        public void notifyAbbreviationCompleted(AbbreviationResult result) {
        }

        @Override
        public void notifyAbbreviationCompleted(List<AbbreviationResult> abbreviationResults) {
        }
    }
}

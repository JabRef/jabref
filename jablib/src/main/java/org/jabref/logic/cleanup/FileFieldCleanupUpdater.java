package org.jabref.logic.cleanup;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jabref.logic.bibtex.FileFieldWriter;
import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;

final class FileFieldCleanupUpdater {

    private FileFieldCleanupUpdater() {
    }

    static List<FieldChange> updateFileField(BibEntry entry,
                                             List<LinkedFile> files,
                                             Consumer<Runnable> mutationScheduler) {
        String newValue = FileFieldWriter.getStringRepresentation(files);
        if (entry.getField(StandardField.FILE).filter(newValue::equals).isPresent()) {
            return List.of();
        }

        AtomicReference<Optional<FieldChange>> change = new AtomicReference<>(Optional.empty());
        mutationScheduler.accept(() -> change.set(entry.setField(StandardField.FILE, newValue)));
        return Optional.ofNullable(change.get())
                       .flatMap(Function.identity()) // Equivalent to `flatten`, unwraps optional in optional.
                       .map(List::of).orElseGet(List::of);
    }
}

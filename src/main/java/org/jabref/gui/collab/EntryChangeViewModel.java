package org.jabref.gui.collab;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.logic.bibtex.DuplicateCheck;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class EntryChangeViewModel extends DatabaseChangeViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntryChangeViewModel.class);

    private final List<FieldChangeViewModel> fieldChanges = new ArrayList<>();

    public EntryChangeViewModel(BibEntry memEntry, BibEntry tmpEntry, BibEntry diskEntry) {
        super();
        name = tmpEntry.getCiteKeyOptional()
                       .map(key -> Localization.lang("Modified entry") + ": '" + key + '\'')
                       .orElse(Localization.lang("Modified entry"));

        // We know that tmpEntry is not equal to diskEntry. Check if it has been modified
        // locally as well, since last tempfile was saved.
        boolean isModifiedLocally = (DuplicateCheck.compareEntriesStrictly(memEntry, tmpEntry) <= 1);

        // Another (unlikely?) possibility is that both disk and mem version has been modified
        // in the same way. Check for this, too.
        boolean modificationsAgree = (DuplicateCheck.compareEntriesStrictly(memEntry, diskEntry) > 1);

        LOGGER.debug("Modified entry: " + memEntry.getCiteKeyOptional().orElse("<no BibTeX key set>")
                + "\n Modified locally: " + isModifiedLocally + " Modifications agree: " + modificationsAgree);

        Set<String> allFields = new TreeSet<>();
        allFields.addAll(memEntry.getFieldNames());
        allFields.addAll(tmpEntry.getFieldNames());
        allFields.addAll(diskEntry.getFieldNames());

        for (String field : allFields) {
            Optional<String> mem = memEntry.getField(field);
            Optional<String> tmp = tmpEntry.getField(field);
            Optional<String> disk = diskEntry.getField(field);

            if ((tmp.isPresent()) && (disk.isPresent())) {
                if (!tmp.equals(disk)) {
                    // Modified externally.
                    fieldChanges.add(new FieldChangeViewModel(field, memEntry, tmpEntry, mem.orElse(null), tmp.get(), disk.get()));
                }
            } else if (((!tmp.isPresent()) && (disk.isPresent()) && !disk.get().isEmpty())
                    || ((!disk.isPresent()) && (tmp.isPresent()) && !tmp.get().isEmpty()
                            && (mem.isPresent()) && !mem.get().isEmpty())) {
                // Added externally.
                fieldChanges.add(new FieldChangeViewModel(field, memEntry, tmpEntry, mem.orElse(null), tmp.orElse(null), disk.orElse(null)));
            }
        }
    }

    @Override
    public void makeChange(BibDatabaseContext database, NamedCompound undoEdit) {
        for (DatabaseChangeViewModel c : fieldChanges) {
            if (c.isAccepted()) {
                c.makeChange(database, undoEdit);
            }
        }
    }

    @Override
    public Node description() {
        VBox container = new VBox();
        Label header = new Label(name);
        header.getStyleClass().add("sectionHeader");
        container.getChildren().add(header);

        for (FieldChangeViewModel change : fieldChanges) {
            container.getChildren().add(change.description());
        }

        return container;
    }

    static class FieldChangeViewModel extends DatabaseChangeViewModel {

        private final BibEntry entry;
        private final BibEntry tmpEntry;
        private final String field;
        private final String inMem;
        private final String onTmp;
        private final String onDisk;

        public FieldChangeViewModel(String field, BibEntry memEntry, BibEntry tmpEntry, String inMem, String onTmp, String onDisk) {
            super(field);
            entry = memEntry;
            this.tmpEntry = tmpEntry;
            this.field = field;
            this.inMem = inMem;
            this.onTmp = onTmp;
            this.onDisk = onDisk;
        }

        @Override
        public void makeChange(BibDatabaseContext database, NamedCompound undoEdit) {
            if (onDisk == null) {
                entry.clearField(field);
            } else {
                entry.setField(field, onDisk);
            }
            undoEdit.addEdit(new UndoableFieldChange(entry, field, inMem, onDisk));
        }

        @Override
        public Node description() {
            VBox container = new VBox();
            container.getChildren().add(new Label(Localization.lang("Modification of field") + " " + field));

            if ((onDisk != null) && !onDisk.isEmpty()) {
                container.getChildren().add(new Label(Localization.lang("Value set externally") + ": " + onDisk));
            } else {
                container.getChildren().add(new Label(Localization.lang("Value cleared externally")));
            }

            if ((inMem != null) && !inMem.isEmpty()) {
                container.getChildren().add(new Label(Localization.lang("Current value") + ": " + inMem));
            }
            if ((onTmp != null) && !onTmp.isEmpty()) {
                container.getChildren().add(new Label(Localization.lang("Current tmp value") + ": " + onTmp));
            }

            return container;
        }

    }
}

package org.jabref.gui.undo;

import java.util.Collections;
import java.util.List;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.strings.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents the removal of entries. The constructor needs
 * references to the database, entries, and a boolean marked true if the undo
 * is from a call to paste().
 */
public class UndoableInsertEntries extends AbstractUndoableJabRefEdit {

    private static final Logger LOGGER = LoggerFactory.getLogger(UndoableInsertEntries.class);
    private final BibDatabase database;
    private final List<BibEntry> entries;
    private final boolean paste;

    public UndoableInsertEntries(BibDatabase database, BibEntry entry) {
        this(database, Collections.singletonList(entry));
    }

    public UndoableInsertEntries(BibDatabase database, List<BibEntry> entries) {
        this(database, entries, false);
    }

    public UndoableInsertEntries(BibDatabase database, List<BibEntry> entries, boolean paste) {
        this.database = database;
        this.entries = entries;
        this.paste = paste;
    }

    @Override
    public String getPresentationName() {
        if (paste) {
            if (entries.size() > 1) {
                return Localization.lang("paste entries");
            } else if (entries.size() == 1) {
                return Localization.lang("paste entry %0",
                        StringUtil.boldHTML(entries.get(0).getCitationKey().orElse(Localization.lang("undefined"))));
            } else {
                return null;
            }
        } else {
            if (entries.size() > 1) {
                return Localization.lang("insert entries");
            } else if (entries.size() == 1) {
                return Localization.lang("insert entry %0",
                        StringUtil.boldHTML(entries.get(0).getCitationKey().orElse(Localization.lang("undefined"))));
            } else {
                return null;
            }
        }
    }

    @Override
    public void undo() {
        super.undo();

        try {
            database.removeEntries(entries);
        } catch (Throwable ex) {
            LOGGER.warn("Problem undoing `insert entries`", ex);
        }
    }

    @Override
    public void redo() {
        super.redo();
        database.insertEntries(entries);
    }
}

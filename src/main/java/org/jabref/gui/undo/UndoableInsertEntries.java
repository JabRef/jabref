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
 * This class represents the removal of an entry. The constructor needs
 * references to the database, the entry, and the map of open entry editors.
 * TODO is this map still being used?
 * The latter to be able to close the entry's editor if it is opened before
 * the insert is undone.
 */
public class UndoableInsertEntries extends AbstractUndoableJabRefEdit {

    private static final Logger LOGGER = LoggerFactory.getLogger(UndoableInsertEntries.class);
    private final BibDatabase base;
    private final List<BibEntry> entries;
    private final boolean paste;

    public UndoableInsertEntries(BibDatabase base, BibEntry entry) {
        this(base, Collections.singletonList(entry));
    }

    public UndoableInsertEntries(BibDatabase base, List<BibEntry> entries) {
        this(base, entries, false);
    }

    public UndoableInsertEntries(BibDatabase base, List<BibEntry> entries, boolean paste) {
        this.base = base;
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
                        StringUtil.boldHTML(entries.get(0).getCiteKeyOptional().orElse(Localization.lang("undefined"))));
            } else {
                return null;
            }
        } else {
            if (entries.size() > 1) {
                return Localization.lang("insert entries");
            } else if (entries.size() == 1) {
                return Localization.lang("insert entry %0",
                        StringUtil.boldHTML(entries.get(0).getCiteKeyOptional().orElse(Localization.lang("undefined"))));
            } else {
                return null;
            }
        }
    }

    @Override
    public void undo() {
        super.undo();

        try {
            base.removeEntries(entries);
        } catch (Throwable ex) {
            LOGGER.warn("Problem undoing `insert entries`", ex);
        }
    }

    @Override
    public void redo() {
        super.redo();
        base.insertEntries(entries);
    }

}

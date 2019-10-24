package org.jabref.gui.undo;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.EntryEventSource;
import org.jabref.model.strings.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * This class represents the removal of an entry. The constructor needs
 * references to the database, the entry, and the map of open entry editors.
 * The latter to be able to close the entry's editor if it is opened after
 * an undo, and the removal is then undone.
 */
public class UndoableRemoveEntries extends AbstractUndoableJabRefEdit {

    private static final Logger LOGGER = LoggerFactory.getLogger(UndoableRemoveEntries.class);
    private final BibDatabase base;
    private final List<BibEntry> entries;
    private final boolean cut;

    public UndoableRemoveEntries(BibDatabase base, BibEntry entry) {
        this(base, Collections.singletonList(entry));
    }

    public UndoableRemoveEntries(BibDatabase base, List<BibEntry> entries) {
        this(base, entries, false);
    }

    public UndoableRemoveEntries(BibDatabase base, List<BibEntry> entries, boolean cut) {
        this.base = base;
        this.entries = entries;
        this.cut = cut;
    }

    @Override
    public String getPresentationName() {
        if (cut) {
            if (entries.size() > 1) {
                return Localization.lang("cut entries");
            }
            else if (entries.size() == 1) {
                return Localization.lang("cut entry %0",
                        StringUtil.boldHTML(entries.get(0).getCiteKeyOptional().orElse(Localization.lang("undefined"))));
            } else  {
                return null;
            }
        } else {
            if (entries.size() > 1) {
                return Localization.lang("remove entries");
            }
            else if (entries.size() == 1) {
                return Localization.lang("remove entry %0",
                        StringUtil.boldHTML(entries.get(0).getCiteKeyOptional().orElse(Localization.lang("undefined"))));
            }
            else {
                return null;
            }
        }
    }

    @Override
    public void undo() {
        super.undo();
        base.insertEntries(entries, EntryEventSource.UNDO);
    }

    @Override
    public void redo() {
        super.redo();

        // Redo the change.
        try {
            base.removeEntries(entries);
        } catch (Throwable ex) {
            LOGGER.warn("Problem to redo `remove entry`", ex);
        }
    }

}

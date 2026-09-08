package org.jabref.logic.undo;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.FieldTextMapper;
import org.jabref.model.undo.BibChange;
import org.jabref.model.undo.ChangeSet;
import org.jabref.model.undo.UndoableChangeType;
import org.jabref.model.undo.UndoableFieldChange;
import org.jabref.model.undo.UndoableGroupChange;
import org.jabref.model.undo.UndoableGroupTreeChange;
import org.jabref.model.undo.UndoableInsertEntries;
import org.jabref.model.undo.UndoableInsertString;
import org.jabref.model.undo.UndoableKeywordSeparatorChange;
import org.jabref.model.undo.UndoableMetaDataChange;
import org.jabref.model.undo.UndoablePreambleChange;
import org.jabref.model.undo.UndoableRemoveEntries;
import org.jabref.model.undo.UndoableRemoveString;
import org.jabref.model.undo.UndoableReplaceStrings;
import org.jabref.model.undo.UndoableStringChange;

import org.jspecify.annotations.NullMarked;

/// Names a change as the user would recognise it, for the message the Undo and Redo actions show.
///
/// The text lives here rather than on the change records: a [BibChange] is a model value and
/// carries no user-facing text.
///
/// A [ChangeSet] describes itself, because its name is the label of the control the user
/// activated and no derived text can beat that. Everything else is a change recorded on its own,
/// which has no such name, so it is described by what it does.
///
/// The switch is exhaustive over the sealed interface: a new kind of change does not compile
/// until it has been given a description here.
@NullMarked
public class BibChangeDescriber {

    private BibChangeDescriber() {
    }

    public static String describe(BibChange change) {
        return switch (change) {
            case ChangeSet changeSet ->
                    changeSet.name();
            case UndoableFieldChange fieldChange ->
                    Localization.lang("Change field %0", FieldTextMapper.getDisplayName(fieldChange.field()));
            case UndoableChangeType _ ->
                    Localization.lang("Change entry type");
            case UndoableInsertEntries insert ->
                    insert.entries().size() == 1 ? Localization.lang("Insert entry") : Localization.lang("Insert entries");
            case UndoableRemoveEntries remove ->
                    remove.entries().size() == 1 ? Localization.lang("Remove entry") : Localization.lang("Remove entries");
            case UndoableGroupChange groupChange ->
                    Localization.lang("Edit group %0", groupChange.after().getName());
            case UndoableGroupTreeChange _ ->
                    Localization.lang("Edit groups");
            case UndoableKeywordSeparatorChange _ ->
                    Localization.lang("Change keyword separator");
            case UndoableMetaDataChange _ ->
                    Localization.lang("Change library settings");
            case UndoablePreambleChange _ ->
                    Localization.lang("Change preamble");
            case UndoableStringChange stringChange ->
                    Localization.lang("Change string %0", stringChange.string().getName());
            case UndoableReplaceStrings _ ->
                    Localization.lang("Change string constants");
            case UndoableInsertString insert ->
                    Localization.lang("Insert string %0", insert.string().getName());
            case UndoableRemoveString remove ->
                    Localization.lang("Remove string %0", remove.string().getName());
        };
    }
}

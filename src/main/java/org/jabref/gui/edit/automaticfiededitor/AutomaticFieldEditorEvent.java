package org.jabref.gui.edit.automaticfiededitor;

public record AutomaticFieldEditorEvent(
        int tabIndex,
        int numberOfAffectedEntries) {
}

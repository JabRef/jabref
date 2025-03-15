package org.jabref.gui.backup;

import javafx.scene.control.ButtonType;

public record BackupChoiceDialogRecord(
        BackupEntry entry,
        ButtonType action) {
}

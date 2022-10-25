package org.jabref.gui.backup;

import java.nio.file.Path;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.SimpleCommand;

public class ResolveBackupAction extends SimpleCommand {
    private final DialogService dialogService;
    private final Path originalBackupPath;

    public ResolveBackupAction(Path originalBackupPath, DialogService dialogService) {
        this.originalBackupPath = originalBackupPath;
        this.dialogService = dialogService;
    }

    @Override
    public void execute() {
        dialogService.showCustomDialogAndWait(new BackupResolverDialog(originalBackupPath));
    }
}

package org.jabref.gui.filelist;

import java.util.EnumSet;

public class FileListDialogOptionDependency {



    private EnumSet<FileListDialogOptions> dialogOptions = EnumSet.noneOf(FileListDialogOptions.class);

    public void setDialogOptions(EnumSet<FileListDialogOptions> dialogOptions) {
        this.dialogOptions = dialogOptions;
    }

    public FileListDialogOptionDependency() {
        //empty, workaround for injection
    }

    public EnumSet<FileListDialogOptions> getDialogOptions() {
        return dialogOptions;
    }

    @Override
    public String toString() {
        return "FileListDialogOptionDependency [dialogOptions=" + dialogOptions + "]";
    }
}

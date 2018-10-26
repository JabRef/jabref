package org.jabref.gui.entryeditor;

public class FileDragDropPreferences {

    private boolean copyFile;
    private boolean linkFile;
    private boolean renameCopyFile;

    public FileDragDropPreferences(boolean copyFile, boolean linkFile, boolean renameCopyFile) {
        this.copyFile = copyFile;
        this.linkFile = linkFile;
        this.renameCopyFile = renameCopyFile;
    }

    public boolean isCopyFile() {
        return copyFile;
    }

    public boolean isLinkFile() {
        return linkFile;
    }

    public boolean isRenameCopyFile() {
        return renameCopyFile;
    }

    public void setCopyFile(boolean copyFile) {
        this.copyFile = copyFile;
    }

    public void setLinkFile(boolean linkFile) {
        this.linkFile = linkFile;
    }

    public void setRenameCopyFile(boolean renameCopyFile) {
        this.renameCopyFile = renameCopyFile;
    }

}

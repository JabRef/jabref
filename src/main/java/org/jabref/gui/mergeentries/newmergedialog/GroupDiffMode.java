package org.jabref.gui.mergeentries.newmergedialog;

public class GroupDiffMode implements DiffMethod {

    private final String separator;

    public GroupDiffMode(String separator) {
       this.separator = separator;
    }

    @Override
    public String separator() {
        return this.separator;
    }
}

package org.jabref.gui.mergeentries.newmergedialog.cell;

public class GroupsFieldNameCell extends FieldNameCell {
    private Runnable onMergeGroups;
    private Runnable onUnmergeGroups;
    private boolean isMerged = false;

    public GroupsFieldNameCell(String text, int rowIndex, boolean groupsMerged) {
        super(text, rowIndex);
        isMerged = groupsMerged;
        setOnMouseClicked(e -> {
            if (isMerged) {
                if (onUnmergeGroups != null) {
                    onUnmergeGroups.run();
                    isMerged = false;
                }
            } else {
                if (onMergeGroups != null) {
                    onMergeGroups.run();
                    isMerged = true;
                }
            }
        });
    }

    public void setOnMergeGroups(Runnable mergeGroups) {
        this.onMergeGroups = mergeGroups;
    }

    public void setOnUnmergeGroups(Runnable unmergeGroups) {
        this.onUnmergeGroups = unmergeGroups;
    }
}

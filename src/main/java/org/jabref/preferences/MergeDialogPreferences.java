package org.jabref.preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.gui.duplicationFinder.DuplicateResolverDialog;
import org.jabref.gui.mergeentries.DiffMode;

public class MergeDialogPreferences {
    private final ObjectProperty<DiffMode> mergeDiffMode;
    private final BooleanProperty mergeShouldShowDiff;
    private final BooleanProperty mergeShouldShowUnifiedDiff;
    private final BooleanProperty mergeHighlightWords;
    private final BooleanProperty mergeShowChangedFieldsOnly;
    private final BooleanProperty mergeApplyToAllEntries;
    private final ObjectProperty<DuplicateResolverDialog.DuplicateResolverResult> allEntriesDuplicateResolverDecision;

    MergeDialogPreferences(DiffMode mergeDiffMode,
                           boolean mergeShouldShowDiff,
                           boolean mergeShouldShowUnifiedDiff,
                           boolean mergeHighlightWords,
                           boolean mergeShowChangedFieldsOnly,
                           boolean mergeApplyToAllEntries,
                           DuplicateResolverDialog.DuplicateResolverResult allEntriesDuplicateResolverDecision) {
        this.mergeDiffMode = new SimpleObjectProperty<>(mergeDiffMode);
        this.mergeShouldShowDiff = new SimpleBooleanProperty(mergeShouldShowDiff);
        this.mergeShouldShowUnifiedDiff = new SimpleBooleanProperty(mergeShouldShowUnifiedDiff);
        this.mergeHighlightWords = new SimpleBooleanProperty(mergeHighlightWords);

        this.mergeShowChangedFieldsOnly = new SimpleBooleanProperty(mergeShowChangedFieldsOnly);
        this.mergeApplyToAllEntries = new SimpleBooleanProperty(mergeApplyToAllEntries);
        this.allEntriesDuplicateResolverDecision = new SimpleObjectProperty<>(allEntriesDuplicateResolverDecision);
    }

    public DiffMode getMergeDiffMode() {
        return mergeDiffMode.get();
    }

    public ObjectProperty<DiffMode> mergeDiffModeProperty() {
        return mergeDiffMode;
    }

    public void setMergeDiffMode(DiffMode mergeDiffMode) {
        this.mergeDiffMode.set(mergeDiffMode);
    }

    public boolean getMergeShouldShowDiff() {
        return mergeShouldShowDiff.get();
    }

    public BooleanProperty mergeShouldShowDiffProperty() {
        return mergeShouldShowDiff;
    }

    public void setMergeShouldShowDiff(boolean mergeShouldShowDiff) {
        this.mergeShouldShowDiff.set(mergeShouldShowDiff);
    }

    public boolean getMergeShouldShowUnifiedDiff() {
        return mergeShouldShowUnifiedDiff.get();
    }

    public BooleanProperty mergeShouldShowUnifiedDiffProperty() {
        return mergeShouldShowUnifiedDiff;
    }

    public void setMergeShouldShowUnifiedDiff(boolean mergeShouldShowUnifiedDiff) {
        this.mergeShouldShowUnifiedDiff.set(mergeShouldShowUnifiedDiff);
    }

    public boolean getMergeHighlightWords() {
        return mergeHighlightWords.get();
    }

    public BooleanProperty mergeHighlightWordsProperty() {
        return mergeHighlightWords;
    }

    public void setMergeHighlightWords(boolean mergeHighlightsWords) {
        this.mergeHighlightWords.set(mergeHighlightsWords);
    }

    public BooleanProperty mergeShowChangedFieldOnlyProperty() {
        return mergeShowChangedFieldsOnly;
    }

    public boolean shouldMergeShowChangedFieldsOnly() {
        return mergeShowChangedFieldsOnly.getValue();
    }

    public void setMergeShowChangedFieldsOnly(boolean showChangedFieldsOnly) {
        mergeShowChangedFieldsOnly.setValue(showChangedFieldsOnly);
    }

    public BooleanProperty mergeApplyToAllEntriesProperty() {
        return mergeApplyToAllEntries;
    }

    public Boolean shouldMergeApplyToAllEntries() {
        return mergeApplyToAllEntries.get();
    }

    public void setIsMergeApplyToAllEntries(boolean applyToAllEntries) {
        this.mergeApplyToAllEntries.setValue(applyToAllEntries);
    }

    public void setAllEntriesDuplicateResolverDecision(DuplicateResolverDialog.DuplicateResolverResult allEntriesDuplicateResolverDecision) {
        this.allEntriesDuplicateResolverDecision.setValue(allEntriesDuplicateResolverDecision);
    }

    public DuplicateResolverDialog.DuplicateResolverResult getAllEntriesDuplicateResolverDecision() {
        return allEntriesDuplicateResolverDecision.get();
    }

    public ObjectProperty<DuplicateResolverDialog.DuplicateResolverResult> allEntriesDuplicateResolverDecisionProperty() {
        return allEntriesDuplicateResolverDecision;
    }
}

package org.jabref.gui.entryeditor.citationrelationtab;

import java.util.BitSet;
import java.util.List;

import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;

/// Keeps track of the review of the checked citation entries: which entry is shown, the edits made so far,
/// and which entries are chosen for adding.
@NullMarked
public class CitationEntryReview {

    private final List<CitationRelationItem> items;
    private final List<BibEntry> editedEntries;
    private final BitSet chosenForAdding;

    /// Index of the entry under review, counted from 0.
    private int position;
    private boolean cancelled;

    public CitationEntryReview(List<CitationRelationItem> itemsToReview) {
        // The check model hands out a live list, which the import changes while the review runs
        this.items = List.copyOf(itemsToReview);
        // The dialog edits copies, so a skipped entry stays in the citation list as it was fetched.
        // One copy per entry keeps the edits when the user goes back to an entry.
        this.editedEntries = items.stream()
                                  .map(item -> new BibEntry(item.entry()))
                                  .toList();
        this.chosenForAdding = new BitSet(items.size());
    }

    /// Returns the copy of the entry under review, which the dialog edits in place.
    ///
    /// Only valid while [#isFinished()] is `false`.
    public BibEntry getCurrentEntry() {
        return editedEntries.get(position);
    }

    /// Returns the number of the current entry as shown in the dialog title, counted from 1.
    public int getCurrentNumber() {
        return position + 1;
    }

    public int getTotal() {
        return items.size();
    }

    /// Checks whether the user moved on from the last entry or ended the review.
    public boolean isFinished() {
        return cancelled || position >= items.size();
    }

    /// Applies the user's choice for the current entry, as described at [CitationEntryReviewDialog.Decision].
    public void applyDecision(CitationEntryReviewDialog.Decision decision) {
        switch (decision) {
            case PREVIOUS ->
                    position = Math.max(0, position - 1);
            case SKIP -> {
                chosenForAdding.clear(position);
                position++;
            }
            case NEXT -> {
                chosenForAdding.set(position);
                position++;
            }
            case CANCEL ->
                    cancelled = true;
        }
    }

    /// Returns the chosen entries with the user's edits, in review order.
    public List<CitationRelationItem> getAcceptedItems() {
        if (cancelled) {
            return List.of();
        }
        return chosenForAdding.stream()
                              .mapToObj(index -> new CitationRelationItem(
                                      editedEntries.get(index),
                                      items.get(index).localEntry(),
                                      items.get(index).isLocal()))
                              .toList();
    }
}

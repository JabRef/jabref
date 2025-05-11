package org.jabref.gui.entryeditor;

/// This interface is used to adapt the visible tabs in the entry editor based on updated preferences
///
/// TODO: A better solution would be use an observable list of tabs in the state manager, but in April 2025, this was too much effort.
public interface AdaptVisibleTabs {
    /**
     * Adapt the visible tabs to the current entry type.
     */
    void adaptVisibleTabs();
}

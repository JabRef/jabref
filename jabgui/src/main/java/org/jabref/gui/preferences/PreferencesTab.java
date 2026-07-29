package org.jabref.gui.preferences;

import java.util.List;

import javafx.scene.Node;

/// A prefsTab is a component displayed in the PreferenceDialog.
///
/// It needs to extend from Component.
public interface PreferencesTab {

    /// Extra, invisible search terms for the tab (synonyms, abbreviations). The visible texts are
    /// covered by {@link #getSearchableElements()} already.
    default List<String> getSearchKeywords() {
        return List.of(getTabName(), getTitle());
    }

    /// The visible texts of the tab with the nodes they caption; the preferences search matches
    /// these and highlights the node.
    default List<SearchableElement> getSearchableElements() {
        return List.of();
    }

    /// @return the root node of the tab's content, shown in the preferences dialog
    Node getContent();

    /// Should return the localized identifier to use for the tab.
    ///
    /// @return Identifier for the tab (for instance "General", "Appearance" or "External Files").
    String getTabName();

    /// The heading the dialog shows above the tab's content. Defaults to the tab's name; override
    /// where the heading is more specific (e.g. tab "Entry types", heading "Custom entry types").
    default String getTitle() {
        return getTabName();
    }

    /// This method is called when the dialog is opened, or if it is made
    /// visible after being hidden. This calls the appropriate method in the
    /// ViewModel.
    void setValues();

    /// This method is called when the user presses OK in the Preferences
    /// dialog. This calls the appropriate method in the ViewModel.
    void storeSettings();

    /// This method is called before the {@link #storeSettings()} method,
    /// to check if there are illegal settings in the tab, or if is ready
    /// to be closed. This calls the appropriate method in the ViewModel.
    boolean validateSettings();

    /// This method should be called after storing the preferences, This
    /// calls the appropriate method in the ViewModel.
    ///
    /// @return The messages for the changed properties (e.g. "Changed language: English")
    List<String> getRestartWarnings();
}

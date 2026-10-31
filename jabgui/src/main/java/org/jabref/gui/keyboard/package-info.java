/// Keyboard shortcuts and their routing in the desktop UI.
///
/// [org.jabref.gui.keyboard.KeyBindingRepository] resolves configured shortcuts;
/// [org.jabref.gui.keyboard.KeyBindingDispatcher] runs a view's actions for its shortcuts and lets all other shortcuts bubble up;
/// [org.jabref.gui.keyboard.SearchShortcutTarget] lets a focused view handle Search locally.
///
/// @see <a href="https://devdocs.jabref.org/code-howtos/javafx.html">JavaFX code howto</a>
package org.jabref.gui.keyboard;

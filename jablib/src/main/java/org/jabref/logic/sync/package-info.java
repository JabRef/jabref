/// Keeping an in-memory library and its file the same in both directions without asking more than necessary.
///
/// [LibraryBaseline] is the common ancestor for the three-way comparison of the in-memory library with the file: for
/// each item it tells which side diverged, which decides whether an external change is taken over silently or needs
/// a review, and merges entries changed on both sides field by field with the rules of the Git merge
/// (`org.jabref.logic.git.merge.planning.util`).
///
/// Detecting the changes themselves and presenting a review is the GUI's part (`org.jabref.gui.collab`).
///
/// See <https://devdocs.jabref.org/requirements/ux.html> for the requirements this implements.
@NullMarked
package org.jabref.logic.sync;

import org.jspecify.annotations.NullMarked;

package org.jabref.gui.preferences.forms;

import javafx.scene.layout.VBox;

import org.jspecify.annotations.NullMarked;

/// A container whose elements are laid out as setting tiles: each tile a row of its own, the rows
/// divided by thin lines, the elements that are no tile collected in cards between them.
///
/// A section's contents and the block the builder opens for tiles outside a section are drawn as
/// one surface; a group inside either is transparent, so that it can be disabled or hidden as one
/// without changing how its contents look.
@NullMarked
final class TileGroup extends VBox {

    private final boolean implicit;

    private TileGroup(boolean surface, boolean implicit) {
        this.implicit = implicit;
        if (surface) {
            getStyleClass().add("setting-tile-group");
        }
    }

    /// The contents of a section.
    static TileGroup section() {
        return new TileGroup(true, false);
    }

    /// The block the builder opens for consecutive tiles outside a section.
    static TileGroup block() {
        return new TileGroup(true, true);
    }

    /// A group inside a tile group, drawn on the surface of the one it sits in.
    static TileGroup nested() {
        return new TileGroup(false, false);
    }

    /// Whether the builder opened this group by itself, so that following tiles may join it.
    boolean isImplicit() {
        return implicit;
    }
}

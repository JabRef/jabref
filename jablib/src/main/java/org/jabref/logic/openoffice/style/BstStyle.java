package org.jabref.logic.openoffice.style;

import java.nio.file.Path;

import org.jspecify.annotations.NullMarked;

/// An [OOStyle] backed by an external `.bst` file.
/// BST styles are always external (user-supplied); there are no internal BST styles.
@NullMarked
public class BstStyle implements OOStyle {

    private final Path path;

    public BstStyle(Path path) {
        this.path = path;
    }

    @Override
    public String getName() {
        return path.getFileName().toString();
    }

    @Override
    public boolean isInternalStyle() {
        return false;
    }

    @Override
    public String getPath() {
        return path.toString();
    }

    public Path getFilePath() {
        return path;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof BstStyle other) && path.equals(other.path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    @Override
    public String toString() {
        return "BstStyle{path=" + path + "}";
    }
}

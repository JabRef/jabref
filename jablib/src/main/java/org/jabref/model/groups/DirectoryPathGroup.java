package org.jabref.model.groups;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;

/// One directory of a directory library, materialized as a subgroup of
/// [DirectoryStructureGroup]: matches every entry whose source file lies in this directory or
/// anywhere below it. These nodes are recomputed from the entries and never persisted.
@NullMarked
public class DirectoryPathGroup extends AbstractGroup {

    private final Path relativeDirectory;
    private final Function<BibEntry, Optional<Path>> sourceFileLookup;

    /// @param relativeDirectory the directory this group represents, relative to the library root
    /// @param sourceFileLookup  resolves an entry to its source file, relative to the library root
    public DirectoryPathGroup(Path relativeDirectory, Function<BibEntry, Optional<Path>> sourceFileLookup) {
        super(relativeDirectory.getFileName().toString(), GroupHierarchyType.INDEPENDENT);
        this.relativeDirectory = relativeDirectory;
        this.sourceFileLookup = sourceFileLookup;
    }

    @Override
    public boolean contains(BibEntry entry) {
        return sourceFileLookup.apply(entry)
                               .map(Path::getParent)
                               .map(directory -> directory.startsWith(relativeDirectory))
                               .orElse(false);
    }

    @Override
    public boolean isDynamic() {
        return false;
    }

    @Override
    public AbstractGroup deepCopy() {
        return new DirectoryPathGroup(relativeDirectory, sourceFileLookup);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DirectoryPathGroup that = (DirectoryPathGroup) o;
        return Objects.equals(relativeDirectory, that.relativeDirectory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(relativeDirectory);
    }
}

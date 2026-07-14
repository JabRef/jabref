package org.jabref.model.groups;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.jabref.model.entry.BibEntry;

/// Mirrors the folder structure of a directory library in the groups panel
/// (<https://github.com/JabRef/jabref/issues/10930>): each subdirectory containing entries
/// becomes a [DirectoryPathGroup] subgroup. Like the other automatic groups, the subgroups are
/// materialized in the GUI from the current entries; this group does not watch the file system
/// itself — the directory synchronizer already updates the entries and invalidates the groups
/// view. When parsed back from a `.bib` file (after "Save as"), the lookup yields nothing and
/// the group simply stays empty.
public class DirectoryStructureGroup extends AutomaticGroup {

    private final Function<BibEntry, Optional<Path>> sourceFileLookup;

    /// @param sourceFileLookup resolves an entry to its source file, relative to the library root
    public DirectoryStructureGroup(String name, GroupHierarchyType context, Function<BibEntry, Optional<Path>> sourceFileLookup) {
        super(name, context);
        this.sourceFileLookup = sourceFileLookup;
    }

    @Override
    public Set<GroupTreeNode> createSubgroups(BibEntry entry) {
        Optional<Path> directory = sourceFileLookup.apply(entry).map(Path::getParent);
        if (directory.isEmpty()) {
            // Files directly in the library root need no subgroup
            return Set.of();
        }
        GroupTreeNode top = null;
        GroupTreeNode current = null;
        for (int depth = 1; depth <= directory.get().getNameCount(); depth++) {
            GroupTreeNode node = new GroupTreeNode(new DirectoryPathGroup(directory.get().subpath(0, depth), sourceFileLookup));
            if (current == null) {
                top = node;
            } else {
                current.addChild(node);
            }
            current = node;
        }
        return Set.of(top);
    }

    @Override
    public AbstractGroup deepCopy() {
        return new DirectoryStructureGroup(name.getValue(), context, sourceFileLookup);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DirectoryStructureGroup that = (DirectoryStructureGroup) o;
        return Objects.equals(getName(), that.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName());
    }
}

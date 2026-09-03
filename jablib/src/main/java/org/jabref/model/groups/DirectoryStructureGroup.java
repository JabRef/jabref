package org.jabref.model.groups;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;

/// Mirrors the folder structure of a directory library in the groups panel
/// (<https://github.com/JabRef/jabref/issues/10930>): each subdirectory containing entries
/// becomes a [DirectoryPathGroup] subgroup. Like the other automatic groups, the subgroups are
/// materialized in the GUI from the current entries; this group does not watch the file system
/// itself — the directory synchronizer already updates the entries and invalidates the groups
/// view. When parsed back from a `.bib` file (after "Save as"), the lookup yields nothing and
/// the group simply stays empty.
@NullMarked
public class DirectoryStructureGroup extends AutomaticGroup {

    private final Function<BibEntry, Optional<Path>> sourceFileLookup;

    /// @param sourceFileLookup resolves an entry to its source file, relative to the library root
    public DirectoryStructureGroup(String name, GroupHierarchyType context, Function<BibEntry, Optional<Path>> sourceFileLookup) {
        super(name, context);
        this.sourceFileLookup = sourceFileLookup;
    }

    /// Files directly in the library root need no subgroup.
    @Override
    public Set<GroupTreeNode> createSubgroups(BibEntry entry) {
        return sourceFileLookup.apply(entry)
                               .map(Path::getParent)
                               .map(this::directoryChain)
                               .orElse(Set.of());
    }

    /// `conference/2020` becomes the chain `conference` > `2020`.
    private Set<GroupTreeNode> directoryChain(Path directory) {
        GroupTreeNode top = new GroupTreeNode(new DirectoryPathGroup(directory.subpath(0, 1), sourceFileLookup));
        GroupTreeNode current = top;
        for (int depth = 2; depth <= directory.getNameCount(); depth++) {
            GroupTreeNode child = new GroupTreeNode(new DirectoryPathGroup(directory.subpath(0, depth), sourceFileLookup));
            current.addChild(child);
            current = child;
        }
        return Set.of(top);
    }

    @Override
    public AbstractGroup deepCopy() {
        return new DirectoryStructureGroup(name.getValue(), context, sourceFileLookup);
    }
}

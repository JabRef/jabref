package org.jabref.model.groups;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A group that mirrors a directory structure from the file system.
 * Entries belong to this group if they have a linked file within the specified directory.
 * Sub-groups are automatically created for subdirectories.
 */
public class DirectoryGroup extends AbstractGroup {

    private static final Logger LOGGER = LoggerFactory.getLogger(DirectoryGroup.class);

    private final Path directoryPath;
    private final Set<String> matchedEntries = new HashSet<>();

    /**
     * Creates a new DirectoryGroup.
     *
     * @param name          The name of the group (typically the directory name)
     * @param context       The hierarchical context
     * @param directoryPath The path to the directory this group mirrors
     */
    public DirectoryGroup(String name, GroupHierarchyType context, Path directoryPath) {
        super(name, context);
        this.directoryPath = Objects.requireNonNull(directoryPath, "directoryPath must not be null");
    }

    /**
     * Returns the directory path this group mirrors.
     */
    public Path getDirectoryPath() {
        return directoryPath;
    }

    /**
     * Checks if a BibEntry belongs to this group.
     * An entry belongs to this group if it has a linked file within the directory.
     */
    @Override
    public boolean contains(BibEntry entry) {
        return matchedEntries.contains(entry.getId());
    }

    /**
     * Updates the matched entries based on file links.
     * Called when the directory content changes or when entries are modified.
     *
     * @param entry   The entry to check
     * @param matched Whether the entry should be in this group
     */
    public void updateMatches(BibEntry entry, boolean matched) {
        if (matched) {
            matchedEntries.add(entry.getId());
        } else {
            matchedEntries.remove(entry.getId());
        }
    }

    /**
     * Checks if an entry has a file linked within this directory.
     *
     * @param entry The entry to check
     * @return true if the entry has a file in this directory
     */
    public boolean hasFileInDirectory(BibEntry entry) {
        return entry.getField(StandardField.FILE).map(fileField -> {
            // Parse the file field and check if any file is in this directory
            // File field format: description:path:type
            String[] parts = fileField.split(":");
            if (parts.length >= 2) {
                Path filePath = Path.of(parts[1]);
                return isFileInDirectory(filePath);
            }
            return false;
        }).orElse(false);
    }

    /**
     * Checks if a file path is within this group's directory.
     */
    private boolean isFileInDirectory(Path filePath) {
        try {
            Path normalizedDir = directoryPath.toAbsolutePath().normalize();
            Path normalizedFile = filePath.toAbsolutePath().normalize();
            return normalizedFile.startsWith(normalizedDir);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    /**
     * Scans the directory and returns a list of DirectoryGroups for each subdirectory.
     * This is used to automatically create the group tree structure mirroring the file system.
     *
     * @return List of DirectoryGroup objects for immediate subdirectories
     */
    public List<DirectoryGroup> scanSubdirectories() {
        List<DirectoryGroup> subgroups = new ArrayList<>();

        if (!Files.isDirectory(directoryPath)) {
            LOGGER.warn("Cannot scan subdirectories: {} is not a directory", directoryPath);
            return subgroups;
        }

        try (Stream<Path> paths = Files.list(directoryPath)) {
            paths.filter(Files::isDirectory).sorted().forEach(subDir -> {
                String subDirName = subDir.getFileName().toString();
                DirectoryGroup subgroup = new DirectoryGroup(subDirName, GroupHierarchyType.INCLUDING, subDir);
                subgroups.add(subgroup);
            });
        } catch (IOException e) {
            LOGGER.error("Error scanning subdirectories of {}", directoryPath, e);
        }

        return subgroups;
    }

    /**
     * Recursively builds the complete group tree structure from the directory.
     * Each subdirectory becomes a subgroup, and this process continues recursively.
     *
     * @param parentNode The parent GroupTreeNode to add subgroups to
     */
    public void buildGroupTree(GroupTreeNode parentNode) {
        List<DirectoryGroup> subgroups = scanSubdirectories();

        for (DirectoryGroup subgroup : subgroups) {
            GroupTreeNode childNode = parentNode.addSubgroup(subgroup);
            // Recursively build tree for subdirectories
            subgroup.buildGroupTree(childNode);
        }
    }

    @Override
    public AbstractGroup deepCopy() {
        DirectoryGroup copy = new DirectoryGroup(getName(), getHierarchicalContext(), directoryPath);
        copy.color = this.color;
        copy.isExpanded = this.isExpanded;
        copy.description = this.description;
        copy.iconName = this.iconName;
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DirectoryGroup other)) {
            return false;
        }
        return Objects.equals(getName(), other.getName()) && Objects.equals(getHierarchicalContext(), other.getHierarchicalContext()) && Objects.equals(directoryPath, other.directoryPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getHierarchicalContext(), directoryPath);
    }

    @Override
    public String toString() {
        return "DirectoryGroup{" + "name='" + getName() + '\'' + ", directoryPath=" + directoryPath + ", context=" + context + ", color=" + color + ", isExpanded=" + isExpanded + ", description=" + description + ", iconName=" + iconName + '}';
    }
}

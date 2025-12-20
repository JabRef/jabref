package org.jabref.model.groups;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

/**
 * A group that mirrors a directory structure from the file system.
 * Entries belong to this group if they have a linked file within the specified directory.
 * Sub-groups are automatically created for subdirectories.
 */
public class DirectoryGroup extends AbstractGroup {

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
        return entry.getField(StandardField.FILE)
                    .map(fileField -> {
                        // Parse the file field and check if any file is in this directory
                        // File field format: description:path:type
                        String[] parts = fileField.split(":");
                        if (parts.length >= 2) {
                            Path filePath = Path.of(parts[1]);
                            return isFileInDirectory(filePath);
                        }
                        return false;
                    })
                    .orElse(false);
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
        return Objects.equals(getName(), other.getName())
                && Objects.equals(getHierarchicalContext(), other.getHierarchicalContext())
                && Objects.equals(directoryPath, other.directoryPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getHierarchicalContext(), directoryPath);
    }

    @Override
    public String toString() {
        return "DirectoryGroup{" +
                "name='" + getName() + '\'' +
                ", directoryPath=" + directoryPath +
                ", context=" + context +
                ", color=" + color +
                ", isExpanded=" + isExpanded +
                ", description=" + description +
                ", iconName=" + iconName +
                '}';
    }
}

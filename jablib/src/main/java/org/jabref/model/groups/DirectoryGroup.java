package org.jabref.model.groups;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.architecture.AllowedToUseLogic;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.util.DirectoryUpdateListener;
import org.jabref.model.util.DirectoryUpdateMonitor;
import org.jabref.model.util.DummyDirectoryUpdateMonitor;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AllowedToUseLogic("Because it needs to recognize a PDF")
public class DirectoryGroup extends AbstractGroup implements DirectoryUpdateListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(DirectoryGroup.class);

    private final Path absoluteDirectoryPath;
    private final DirectoryUpdateMonitor directoryUpdateMonitor;
    private final MetaData metaData;
    private final String user;

    DirectoryGroup(String name,
                   GroupHierarchyType context,
                   @NonNull Path absoluteDirectoryPath,
                   DirectoryUpdateMonitor directoryUpdateMonitor,
                   MetaData metaData,
                   String user) {
        super(name, context);
        this.metaData = metaData;
        this.user = user;
        this.absoluteDirectoryPath = absoluteDirectoryPath;
        this.directoryUpdateMonitor = directoryUpdateMonitor;
    }

    public static DirectoryGroup create(String name,
                                        GroupHierarchyType context,
                                        Path absoluteDirectoryPath,
                                        DirectoryUpdateMonitor directoryUpdateMonitor,
                                        MetaData metaData,
                                        String userAndHost) throws IOException {
        DirectoryGroup group = new DirectoryGroup(name, context, absoluteDirectoryPath, directoryUpdateMonitor, metaData, userAndHost);
        directoryUpdateMonitor.addListenerForDirectory(absoluteDirectoryPath, group);
        return group;
    }

    // without DirectoryUpdateMonitor
    public static DirectoryGroup create(String name,
                                        GroupHierarchyType context,
                                        Path absoluteDirectoryPath,
                                        MetaData metaData,
                                        String userAndHost) throws IOException {
        return new DirectoryGroup(name, context, absoluteDirectoryPath, new DummyDirectoryUpdateMonitor(), metaData, userAndHost);
    }

    public void addDescendants() throws IOException {
        File parentFolder = absoluteDirectoryPath.toFile();
        Optional<GroupTreeNode> parentNode = getNode();
        File[] folders = parentFolder.listFiles(File::isDirectory);
        if (folders != null) {
            for (File folder : folders) {
                DirectoryGroup newSubgroup = this.createDescendantGroup(folder);
                parentNode.ifPresent(group -> group.addSubgroup(newSubgroup));
                newSubgroup.addDescendants();
            }
        }
    }

    public DirectoryGroup createDescendantGroup(File descendantDirectory) throws IOException {
        DirectoryGroup descendantGroup = DirectoryGroup.create(descendantDirectory.getName(),
                this.context,
                descendantDirectory.toPath(),
                this.directoryUpdateMonitor,
                this.metaData,
                this.user);
        this.getColor().ifPresent(descendantGroup::setColor);
        this.getIconName().ifPresent(descendantGroup::setIconName);
        return descendantGroup;
    }

    public List<Path> getAllPDFs() {
        File parentFolder = absoluteDirectoryPath.toFile();
        Optional<GroupTreeNode> parentNode = getNode();
        List<Path> allPDFs = new ArrayList<>();
        File[] files = parentFolder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (FileUtil.isPDFFile(file.toPath())) {
                    allPDFs.add(file.toPath());
                }
            }
        }
        if (parentNode.isPresent()) {
            for (GroupTreeNode childNode : parentNode.get().getChildren()) {
                if (childNode.getGroup() instanceof DirectoryGroup childGroup) {
                    allPDFs.addAll(childGroup.getAllPDFs());
                }
            }
        }
        return allPDFs;
    }

    public Optional<GroupTreeNode> getNode() {
        Optional<GroupTreeNode> groupNode = Optional.empty();
        Optional<GroupTreeNode> rootNode = metaData.getGroups();
        if (rootNode.isPresent()) {
            List<GroupTreeNode> groupNodeCandidates = rootNode.get().findChildrenSatisfying(groupTreeNode -> groupTreeNode.getGroup().equals(this));
            if (groupNodeCandidates.size() == 1) {
                groupNode = Optional.of(groupNodeCandidates.getFirst());
            }
        }
        return groupNode;
    }

    public Boolean isDeleted() {
        return getNode().isEmpty();
    }

    @Override
    public boolean contains(BibEntry entry) {
        List<LinkedFile> entryFiles = entry.getFiles();
        for (LinkedFile linkedFile : entryFiles) {
            Path filePath = Path.of(linkedFile.getLink());
            if (absoluteDirectoryPath.toString().endsWith(filePath.getParent().toString())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    @Override
    public AbstractGroup deepCopy() {
        return new DirectoryGroup(name.getValue(), context, absoluteDirectoryPath, directoryUpdateMonitor, metaData, user);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        DirectoryGroup group = (DirectoryGroup) o;
        return Objects.equals(absoluteDirectoryPath, group.absoluteDirectoryPath);
    }

    @Override
    public String toString() {
        return "DirectoryGroup{" +
                "directoryPath=" + absoluteDirectoryPath +
                ", directoryUpdateMonitor=" + directoryUpdateMonitor +
                "} " + super.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), absoluteDirectoryPath);
    }

    public Path getAbsoluteDirectoryPath() {
        return absoluteDirectoryPath;
    }

    @Override
    public void directoryCreated(Path newPath) throws IOException {
        Optional<GroupTreeNode> groupNode = getNode();
        if (groupNode.isPresent()) {
            DirectoryGroup newSubgroup = this.createDescendantGroup(newPath.toFile());
            groupNode.get().addSubgroup(newSubgroup);
            newSubgroup.addDescendants();
        } else {
            LOGGER.error("Directory {} could not be created because its parent is not linked with a GroupTreeNode", newPath);
        }
    }

    @Override
    public void directoryDeleted() {
        Optional<GroupTreeNode> groupNode = getNode();
        if (groupNode.isPresent()) {
            groupNode.get().removeFromParent();
            // TODO : finish the deletion by deleting the corresponding entries
        } else {
            LOGGER.error("Directory {} could not be deleted because it is not linked with a GroupTreeNode", absoluteDirectoryPath);
        }
    }

    @Override
    public void PDFDeleted(Path PDFPath) {
        // TODO : Find the corresponding entry and remove it from the database
    }
}

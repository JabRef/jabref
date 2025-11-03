package org.jabref.model.groups;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.pdf.PdfContentImporter;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.TreeNode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.util.DirectoryUpdateListener;
import org.jabref.model.util.DirectoryUpdateMonitor;
import org.jabref.model.util.DummyDirectoryUpdateMonitor;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DirectoryGroup extends AbstractGroup implements DirectoryUpdateListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(DirectoryGroup.class);

    private Path directoryPath;
    private final DirectoryUpdateMonitor directoryMonitor;
    private final MetaData metaData;
    private final String user;

    DirectoryGroup(String name,
                   GroupHierarchyType context,
                   @NonNull Path directoryPath,
                   DirectoryUpdateMonitor directoryMonitor,
                   MetaData metaData,
                   String user) {
        super(name, context);
        this.metaData = metaData;
        this.user = user;
        this.directoryPath = expandPath(directoryPath);
        this.directoryMonitor = directoryMonitor;
    }

    public static DirectoryGroup create(String name,
                                        GroupHierarchyType context,
                                        Path directoryPath,
                                        DirectoryUpdateMonitor directoryMonitor,
                                        MetaData metaData,
                                        String userAndHost) throws IOException {
        DirectoryGroup group = new DirectoryGroup(name, context, directoryPath, directoryMonitor, metaData, userAndHost);
        directoryMonitor.addListenerForDirectory(group.getDirectoryPathResolved(), group);
        return group;
    }

    // without DirectoryUpdateMonitor
    public static DirectoryGroup create(String name,
                                        GroupHierarchyType context,
                                        Path directoryPath,
                                        MetaData metaData,
                                        String userAndHost) throws IOException {
        return new DirectoryGroup(name, context, directoryPath, new DummyDirectoryUpdateMonitor(), metaData, userAndHost);
    }

    public void addDescendants() throws IOException {
        File parentFolder = directoryPath.toFile();
        Optional<GroupTreeNode> parentNode = getNode();
        File[] folders = parentFolder.listFiles(File::isDirectory);
        for (File folder : folders) {
            DirectoryGroup newSubgroup = this.createDescendantGroup(folder);
            parentNode.ifPresent(group -> group.addSubgroup(newSubgroup));
            newSubgroup.addDescendants();
        }
        File[] pdfs = parentFolder.listFiles(file -> FileUtil.isPDFFile(file.toPath()));
        for (File pdf : pdfs) {
            ParserResult pdfImporterResult = new PdfContentImporter().importDatabase(pdf.toPath());
            List<BibEntry> entriesToAdd = pdfImporterResult.getDatabase().getEntries();
        }
    }

    public DirectoryGroup createDescendantGroup(File descendantDirectory) throws IOException {
        DirectoryGroup descendantGroup = DirectoryGroup.create(descendantDirectory.getName(),
                this.context,
                descendantDirectory.toPath(),
                this.directoryMonitor,
                this.metaData,
                this.user);
        this.getColor().ifPresent(descendantGroup::setColor);
        this.getIconName().ifPresent(descendantGroup::setIconName);
        return descendantGroup;
    }

    public List<BibEntry> createPDFEntries(File[] pdfs) throws IOException {
        List<BibEntry> PDFEntries = new ArrayList<>();
        for (File pdf : pdfs) {
            // TODO : find a way to truly import the pdf with an ImportHandler
        }
        return PDFEntries;
    }

    public Path getDirectoryPathResolved() {
        return this.directoryPath;
    }

    public Optional<GroupTreeNode> getNode() {
        Optional<GroupTreeNode> groupNode = Optional.empty();
        List<GroupTreeNode> groupNodesToParse = new ArrayList<>();
        Optional<GroupTreeNode> rootNode = metaData.getGroups().filter(TreeNode::isRoot);
        rootNode.ifPresent(groupNodesToParse::add);
        while (groupNode.isEmpty() && !groupNodesToParse.isEmpty()) {
            GroupTreeNode groupNodeToParse = groupNodesToParse.getFirst();
            if (groupNodeToParse.getName().equals(name.getValue())) {
                groupNode = Optional.of(groupNodeToParse);
            } else {
                groupNodesToParse.remove(groupNodeToParse);
                for (GroupTreeNode group : groupNodeToParse.getChildren()) {
                    groupNodesToParse.add(group);
                }
            }
        }
        return groupNode;
    }

    @Override
    public boolean contains(BibEntry entry) {
        List<LinkedFile> entryFiles = entry.getFiles();
        for (LinkedFile linkedFile : entryFiles) {
            Path filePath = Path.of(linkedFile.getLink());
            if (directoryPath.toString().endsWith(filePath.getParent().toString())) {
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
        return new DirectoryGroup(name.getValue(), context, directoryPath, directoryMonitor, metaData, user);
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
        return Objects.equals(directoryPath, group.directoryPath);
    }

    @Override
    public String toString() {
        return "DirectoryGroup{" +
                "directoryPath=" + directoryPath +
                ", fileMonitor=" + directoryMonitor +
                "} " + super.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), directoryPath);
    }

    public Path getDirectoryPath() {
        return relativize(directoryPath);
    }

    @Override
    public void directoryRenamed(Path newPath) {
        System.out.println(metaData.getGroups());
        name.setValue(newPath.getFileName().toString());
        directoryPath = newPath;
        getNode().ifPresent(groupNode -> {
                    groupNode.setGroup(this);
                    System.out.println(groupNode.getGroup());
                }
        );
        System.out.println("Directory renamed: " + newPath);
        metaData.getGroups().ifPresent(group -> {
            if (group.getName().equals("All entries")) {
                metaData.setGroups(group);
                System.out.println(group.getChildren());
                System.out.println(getNode().get().getName());
            }
        });
    }

    @Override
    public void directoryCreated(Path newPath) throws IOException {
        DirectoryGroup newSubgroup = this.createDescendantGroup(newPath.toFile());
        getNode().ifPresent(group -> group.addSubgroup(newSubgroup));
        newSubgroup.addDescendants();
        System.out.println("Directory created: " + newPath.toString());
    }

    @Override
    public void fileUpdated() {
        System.out.println("File updated in " + directoryPath.toString());
    }

    /**
     * Relativizes the given path to the file directories.
     * The getLatexFileDirectory must be absolute to correctly relativize because we do not have a bibdatabasecontext
     *
     * @param path The path to relativize
     * @return A relative path or the original one if it could not be made relative
     */
    private Path relativize(Path path) {
        List<Path> fileDirectories = getFileDirectoriesAsPaths();
        return FileUtil.relativize(path, fileDirectories);
    }

    private Path expandPath(Path path) {
        List<Path> fileDirectories = getFileDirectoriesAsPaths();
        return FileUtil.find(path.toString(), fileDirectories).orElse(path);
    }

    private List<Path> getFileDirectoriesAsPaths() {
        return metaData.getLatexFileDirectory(user)
                       .map(List::of)
                       .orElse(List.of());
    }
}

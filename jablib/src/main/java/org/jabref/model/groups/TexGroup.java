package org.jabref.model.groups;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.jabref.architecture.AllowedToUseLogic;
import org.jabref.logic.auxparser.AuxParser;
import org.jabref.logic.util.LazyValue;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateListener;
import org.jabref.model.util.FileUpdateMonitor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@AllowedToUseLogic("because it needs access to aux parser")
@NullMarked
public class TexGroup extends AbstractGroup implements FileUpdateListener {

    private final Path filePath;
    private final LazyValue<Set<String>> keysUsedInAux;
    private final FileUpdateMonitor fileMonitor;
    private final AuxParser auxParser;
    private final MetaData metaData;
    private final String user;

    TexGroup(String name,
             GroupHierarchyType context,
             Path filePath,
             AuxParser auxParser,
             FileUpdateMonitor fileMonitor,
             MetaData metaData,
             String user) {
        super(name, context);

        this.metaData = metaData;
        this.user = user;
        this.filePath = expandPath(filePath);
        this.auxParser = auxParser;
        this.fileMonitor = fileMonitor;

        this.keysUsedInAux = new LazyValue<>(() -> auxParser.parse(filePath).getUniqueKeys());
    }

    public static TexGroup create(String name,
                                  GroupHierarchyType context,
                                  Path filePath,
                                  AuxParser auxParser,
                                  FileUpdateMonitor fileMonitor,
                                  MetaData metaData,
                                  String userAndHost) throws IOException {
        TexGroup group = new TexGroup(name, context, filePath, auxParser, fileMonitor, metaData, userAndHost);
        fileMonitor.addListenerForFile(group.getFilePathResolved(), group);
        return group;
    }

    // without FileUpdateMonitor
    public static TexGroup create(String name,
                                  GroupHierarchyType context,
                                  Path filePath,
                                  AuxParser auxParser,
                                  MetaData metaData,
                                  String userAndHost) throws IOException {
        return new TexGroup(name, context, filePath, auxParser, new DummyFileUpdateMonitor(), metaData, userAndHost);
    }

    public Path getFilePathResolved() {
        return this.filePath;
    }

    @Override
    public boolean contains(BibEntry entry) {
        return entry.getCitationKey()
                    .map(keysUsedInAux.get()::contains)
                    .orElse(false);
    }

    @Override
    public boolean isDynamic() {
        return false;
    }

    @Override
    public AbstractGroup deepCopy() {
        return new TexGroup(name.getValue(), context, filePath, auxParser, fileMonitor, metaData, user);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        TexGroup group = (TexGroup) o;
        return Objects.equals(filePath, group.filePath);
    }

    @Override
    public String toString() {
        return "TexGroup{" +
                "filePath=" + filePath +
                ", keysUsedInAux=" + keysUsedInAux +
                ", auxParser=" + auxParser +
                ", fileMonitor=" + fileMonitor +
                "} " + super.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), filePath);
    }

    public Path getFilePath() {
        return relativize(filePath);
    }

    @Override
    public void fileUpdated() {
        // Reset previous parse result
        keysUsedInAux.invalidate();
        metaData.groupsBinding().invalidate();
    }

    /// Relativizes the given path to the file directories.
    /// The getLatexFileDirectory must be absolute to correctly relativize because we do not have a bibdatabasecontext
    ///
    /// @param path The path to relativize
    /// @return A relative path or the original one if it could not be made relative
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

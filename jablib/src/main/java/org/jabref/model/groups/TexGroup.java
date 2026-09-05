package org.jabref.model.groups;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.Set;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;

import org.jabref.architecture.AllowedToUseLogic;
import org.jabref.logic.auxparser.AuxParser;
import org.jabref.logic.auxparser.AuxParserResult;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateListener;
import org.jabref.model.util.FileUpdateMonitor;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AllowedToUseLogic("because it needs access to aux parser")
@NullMarked
public class TexGroup extends AbstractGroup implements FileUpdateListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(TexGroup.class);

    /// The path exactly as stored in the `.bib` file. It can be relative or absolute.
    ///
    /// The path is not resolved in the constructor. When the library is parsed, the location of the
    /// `.bib` file is not known yet, and it changes again on "Save as". Resolution is therefore done
    /// on demand in `getFilePathResolved()` and is dropped when the library or LaTeX directory changes.
    private final Path storedPath;
    private final FileUpdateMonitor fileMonitor;
    private final AuxParser auxParser;
    private MetaData metaData;
    private final String user;

    /// Lazily computed value, therefore, nullable.
    private @Nullable Set<String> keysUsedInAux;

    /// Lazily computed value, therefore, nullable. Dropped when the library path changes or when a
    /// watched aux file is created or updated.
    private @Nullable Path resolvedPath;

    /// Relative aux paths can appear later in any configured directory. We therefore watch every
    /// absolute candidate path so a newly created higher-priority aux file can take over.
    private final SequencedSet<Path> monitoredPaths = new LinkedHashSet<>();

    /// Whether this group was created with a real file monitor.
    private boolean monitorWanted;

    /// Held strongly, because they are registered as weak listeners.
    private final ChangeListener<@Nullable Path> libraryPathListener = (_, _, _) -> pathDependenciesChanged();
    private final WeakChangeListener<@Nullable Path> weakLibraryPathListener = new WeakChangeListener<>(libraryPathListener);
    private final ChangeListener<Number> latexFileDirectoryVersionListener = (_, _, _) -> pathDependenciesChanged();
    private final WeakChangeListener<Number> weakLatexFileDirectoryVersionListener = new WeakChangeListener<>(latexFileDirectoryVersionListener);

    TexGroup(String name,
             GroupHierarchyType context,
             Path filePath,
             AuxParser auxParser,
             FileUpdateMonitor fileMonitor,
             MetaData metaData,
             String user) {
        super(name, context);
        this.storedPath = filePath;
        this.auxParser = auxParser;
        this.fileMonitor = fileMonitor;
        this.metaData = metaData;
        this.user = user;
        registerMetaDataListeners();
    }

    public static TexGroup create(String name,
                                  GroupHierarchyType context,
                                  Path filePath,
                                  AuxParser auxParser,
                                  FileUpdateMonitor fileMonitor,
                                  MetaData metaData,
                                  String userAndHost) throws IOException {
        TexGroup group = new TexGroup(name, context, filePath, auxParser, fileMonitor, metaData, userAndHost);
        group.monitorWanted = true;
        // Arms the monitor if the file can already be resolved. If it cannot, the monitor is armed
        // as soon as the library path or LaTeX directory becomes known.
        group.refreshResolvedPath();
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

    /// The directories a relative aux file path is resolved against, in order of precedence.
    ///
    /// 1. The LaTeX file directory of the library, which is specific to user and host.
    ///    It comes first, because paths written by earlier JabRef versions are relative to it.
    /// 2. The directory of the `.bib` file. This makes the path work on every computer that
    ///    syncs the library, without any per-host setting.
    // [impl->req~ux.groups.cited-entries.aux-path-resolution~1]
    public static List<Path> auxFileDirectories(MetaData metaData, String userAndHost) {
        SequencedSet<Path> directories = new LinkedHashSet<>();
        metaData.getLatexFileDirectory(userAndHost).ifPresent(directories::add);
        metaData.getLibraryPath()
                .flatMap(path -> java.util.Optional.ofNullable(path.getParent()))
                .ifPresent(directories::add);
        return new ArrayList<>(directories);
    }

    /// Converts an absolute aux path to a portable stored path when doing so keeps the same file
    /// selected after applying the LaTeX-directory lookup order.
    public static Path toStoredPath(Path auxFile, MetaData metaData, String userAndHost) {
        return toStoredPath(auxFile, auxFileDirectories(metaData, userAndHost));
    }

    private static Path toStoredPath(Path auxFile, List<Path> directories) {
        if (!auxFile.isAbsolute()) {
            return auxFile;
        }

        Path relativePath = FileUtil.relativize(auxFile, directories);
        if (relativePath.isAbsolute()) {
            return relativePath;
        }

        return FileUtil.find(relativePath.toString(), directories)
                       .filter(resolvedPath -> refersToSameFile(resolvedPath, auxFile))
                       .map(_ -> relativePath)
                       .orElse(auxFile);
    }

    private static boolean refersToSameFile(Path first, Path second) {
        try {
            return Files.isSameFile(first, second);
        } catch (IOException exception) {
            LOGGER.debug("Could not compare aux file targets {} and {}", first, second, exception);
            return first.toAbsolutePath().normalize().equals(second.toAbsolutePath().normalize());
        }
    }

    /// Rebinds this group to the metadata instance that currently owns its group tree.
    /// Only [MetaData] is expected to call this when groups are attached to a library.
    public void setMetaData(MetaData metaData) {
        if (this.metaData == metaData) {
            return;
        }

        unregisterMetaDataListeners();
        this.metaData = metaData;
        registerMetaDataListeners();
        pathDependenciesChanged();
    }

    /// The path currently used for parsing: the first matching aux file, or the highest-priority
    /// absolute candidate while the file is still missing.
    public Path getFilePathResolved() {
        Path currentPath = resolvedPath;
        if (currentPath == null) {
            currentPath = refreshResolvedPath();
        }
        return currentPath;
    }

    /// The path to write to the `.bib` file. It is kept relative if that still resolves to the
    /// same aux file with the library's directory precedence.
    public Path getFilePath() {
        if (!storedPath.isAbsolute()) {
            return storedPath;
        }

        return toStoredPath(getFilePathResolved(), metaData, user);
    }

    @Override
    public boolean contains(BibEntry entry) {
        if (keysUsedInAux == null) {
            AuxParserResult auxResult = auxParser.parse(getFilePathResolved());
            keysUsedInAux = auxResult.getUniqueKeys();
        }

        return entry.getCitationKey().map(keysUsedInAux::contains).orElse(false);
    }

    @Override
    public boolean isDynamic() {
        return false;
    }

    @Override
    public AbstractGroup deepCopy() {
        return new TexGroup(name.getValue(), context, storedPath, auxParser, fileMonitor, metaData, user);
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
        return Objects.equals(storedPath, group.storedPath);
    }

    @Override
    public String toString() {
        return "TexGroup{" +
                "storedPath=" + storedPath +
                ", resolvedPath=" + resolvedPath +
                ", keysUsedInAux=" + keysUsedInAux +
                ", auxParser=" + auxParser +
                ", fileMonitor=" + fileMonitor +
                "} " + super.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), storedPath);
    }

    @Override
    public void fileUpdated() {
        resolvedPath = null;
        keysUsedInAux = null;
        metaData.groupsBinding().invalidate();
    }

    private void registerMetaDataListeners() {
        metaData.libraryPathProperty().addListener(weakLibraryPathListener);
        metaData.latexFileDirectoryVersionProperty().addListener(weakLatexFileDirectoryVersionListener);
    }

    private void unregisterMetaDataListeners() {
        metaData.libraryPathProperty().removeListener(weakLibraryPathListener);
        metaData.latexFileDirectoryVersionProperty().removeListener(weakLatexFileDirectoryVersionListener);
    }

    private Path refreshResolvedPath() {
        Path currentPath = resolveExistingPath().orElseGet(this::firstCandidatePath);
        resolvedPath = currentPath;
        updateFileMonitor(candidateMonitorPaths());
        return currentPath;
    }

    /// Called when the `.bib` file is loaded or moved, or when the LaTeX directory changes.
    private void pathDependenciesChanged() {
        resolvedPath = null;
        keysUsedInAux = null;
        updateFileMonitor(List.of());
        if (monitorWanted) {
            refreshResolvedPath();
        }
        metaData.groupsBinding().invalidate();
    }

    private Optional<Path> resolveExistingPath() {
        return FileUtil.find(storedPath.toString(), auxFileDirectories(metaData, user));
    }

    private Path firstCandidatePath() {
        return candidateMonitorPaths().stream().findFirst().orElse(storedPath);
    }

    private List<Path> candidateMonitorPaths() {
        if (storedPath.isAbsolute()) {
            return List.of(storedPath);
        }

        return auxFileDirectories(metaData, user).stream()
                                                 .map(directory -> directory.resolve(storedPath))
                                                 .filter(Path::isAbsolute)
                                                 .distinct()
                                                 .toList();
    }

    private void updateFileMonitor(List<Path> newPaths) {
        if (!monitorWanted) {
            return;
        }

        SequencedSet<Path> targetPaths = new LinkedHashSet<>(newPaths);
        if (monitoredPaths.equals(targetPaths)) {
            return;
        }

        List<Path> pathsToRemove = monitoredPaths.stream()
                                                 .filter(path -> !targetPaths.contains(path))
                                                 .toList();
        for (Path path : pathsToRemove) {
            fileMonitor.removeListener(path, this);
            monitoredPaths.remove(path);
        }

        for (Path path : targetPaths) {
            if (monitoredPaths.contains(path)) {
                continue;
            }

            try {
                fileMonitor.addListenerForFile(path, this);
                monitoredPaths.add(path);
            } catch (IOException exception) {
                LOGGER.warn("Could not access file {}. The group {} will not reflect changes to the aux file.",
                        path, name.getValue(), exception);
            }
        }
    }
}

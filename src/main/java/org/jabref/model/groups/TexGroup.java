package org.jabref.model.groups;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.jabref.architecture.AllowedToUseLogic;
import org.jabref.logic.auxparser.AuxParser;
import org.jabref.logic.auxparser.AuxParserResult;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.util.FileHelper;
import org.jabref.model.util.FileUpdateListener;
import org.jabref.model.util.FileUpdateMonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AllowedToUseLogic("because it needs access to aux parser")
public class TexGroup extends AbstractGroup implements FileUpdateListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(TexGroup.class);

    private final Path filePath;
    private Set<String> keysUsedInAux;
    private final FileUpdateMonitor fileMonitor;
    private final AuxParser auxParser;
    private final MetaData metaData;
    private final String user;

    TexGroup(String name, GroupHierarchyType context, Path filePath, AuxParser auxParser, FileUpdateMonitor fileMonitor, MetaData metaData, String user) {
        super(name, context);
        this.metaData = metaData;
        this.user = user;
        this.filePath = expandPath(Objects.requireNonNull(filePath));
        this.auxParser = auxParser;
        this.fileMonitor = fileMonitor;
    }

    TexGroup(String name, GroupHierarchyType context, Path filePath, AuxParser auxParser, FileUpdateMonitor fileMonitor, MetaData metaData) throws IOException {
        this(name, context, filePath, auxParser, fileMonitor, metaData, System.getProperty("user.name") + '-' + InetAddress.getLocalHost().getHostName());
    }

    public static TexGroup create(String name, GroupHierarchyType context, Path filePath, AuxParser auxParser, FileUpdateMonitor fileMonitor, MetaData metaData) throws IOException {
        TexGroup group = new TexGroup(name, context, filePath, auxParser, fileMonitor, metaData);
        fileMonitor.addListenerForFile(group.getFilePathResolved(), group);
        return group;
    }

    public static TexGroup createWithoutFileMonitoring(String name, GroupHierarchyType context, Path filePath, AuxParser auxParser, FileUpdateMonitor fileMonitor, MetaData metaData) throws IOException {
        return new TexGroup(name, context, filePath, auxParser, fileMonitor, metaData);
    }

    public Path getFilePathResolved() {
        return this.filePath;
    }

    @Override
    public boolean contains(BibEntry entry) {
        if (keysUsedInAux == null) {
            AuxParserResult auxResult = auxParser.parse(filePath);
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
        try {
            return new TexGroup(name.getValue(), context, filePath, auxParser, fileMonitor, metaData);
        } catch (IOException ex) {
            // This should never happen because we were able to monitor the file just fine until now
            LOGGER.error("Problem creating copy of group", ex);
            return null;
        }
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
        keysUsedInAux = null;
        metaData.groupsBinding().invalidate();
    }

    private Path relativize(Path path) {
        List<Path> fileDirectories = getFileDirectoriesAsPaths();
        return FileHelper.relativize(path, fileDirectories);
    }

    private Path expandPath(Path path) {
        List<Path> fileDirectories = getFileDirectoriesAsPaths();
        return FileHelper.find(path.toString(), fileDirectories).orElse(path);
    }

    private List<Path> getFileDirectoriesAsPaths() {
        return metaData.getLatexFileDirectory(user)
                       .map(List::of)
                       .orElse(Collections.emptyList());
    }
}

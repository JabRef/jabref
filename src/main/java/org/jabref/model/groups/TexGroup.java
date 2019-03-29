package org.jabref.model.groups;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.jabref.model.auxparser.AuxParser;
import org.jabref.model.auxparser.AuxParserResult;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.util.FileHelper;
import org.jabref.model.util.FileUpdateListener;
import org.jabref.model.util.FileUpdateMonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TexGroup extends AbstractGroup implements FileUpdateListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(TexGroup.class);

    private Path filePath;
    private Set<String> keysUsedInAux = null;
    private final FileUpdateMonitor fileMonitor;
    private AuxParser auxParser;
    private final MetaData metaData;
    private String user;

    public TexGroup(String name, GroupHierarchyType context, Path filePath, AuxParser auxParser, FileUpdateMonitor fileMonitor, MetaData metaData, String user) throws IOException {
        super(name, context);
        this.metaData = metaData;
        this.user = user;
        this.filePath = expandPath(Objects.requireNonNull(filePath));
        this.auxParser = auxParser;
        this.fileMonitor = fileMonitor;
        fileMonitor.addListenerForFile(this.filePath, this);
    }

    public TexGroup(String name, GroupHierarchyType context, Path filePath, AuxParser auxParser, FileUpdateMonitor fileMonitor, MetaData metaData) throws IOException {
        this(name, context, filePath, auxParser, fileMonitor, metaData, System.getProperty("user.name") + '-' + InetAddress.getLocalHost().getHostName());
    }

    @Override
    public boolean contains(BibEntry entry) {
        if (keysUsedInAux == null) {
            AuxParserResult auxResult = auxParser.parse(filePath);
            keysUsedInAux = auxResult.getUniqueKeys();
        }

        return entry.getCiteKeyOptional().map(keysUsedInAux::contains).orElse(false);
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
    }

    private Path relativize(Path path) {
        List<Path> fileDirectories = getFileDirectoriesAsPaths();
        return FileHelper.relativize(path, fileDirectories);
    }

    private Path expandPath(Path path) {
        List<Path> fileDirectories = getFileDirectoriesAsPaths();
        return FileHelper.expandFilenameAsPath(path.toString(), fileDirectories).orElse(path);
    }

    private List<Path> getFileDirectoriesAsPaths() {
        List<Path> fileDirs = new ArrayList<>();

        metaData.getLaTexFileDirectory(user)
                .ifPresent(laTexFileDirectory -> fileDirs.add(laTexFileDirectory));

        return fileDirs;
    }
}

package org.jabref.model.groups;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;

import org.jabref.model.auxparser.AuxParser;
import org.jabref.model.auxparser.AuxParserResult;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.FileUpdateListener;
import org.jabref.model.util.FileUpdateMonitor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TexGroup extends AbstractGroup implements FileUpdateListener {

    private static final Log LOGGER = LogFactory.getLog(TexGroup.class);

    private Path filePath;
    private Set<String> keysUsedInAux = null;
    private final FileUpdateMonitor fileMonitor;
    private AuxParser auxParser;

    public TexGroup(String name, GroupHierarchyType context, Path filePath, AuxParser auxParser, FileUpdateMonitor fileMonitor) throws IOException {
        super(name, context);
        this.filePath = Objects.requireNonNull(filePath);
        this.auxParser = auxParser;
        this.fileMonitor = fileMonitor;
        fileMonitor.addListenerForFile(filePath, this);
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
            return new TexGroup(name, context, filePath, auxParser, fileMonitor);
        } catch (IOException ex) {
            // This should never happen because we were able to monitor the file just fine until now
            LOGGER.error(ex);
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
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
        return filePath;
    }

    @Override
    public void fileUpdated() {
        // Reset previous parse result
        keysUsedInAux = null;
    }
}

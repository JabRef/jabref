package org.jabref.model.groups;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Set;

import org.jabref.logic.auxparser.AuxParser;
import org.jabref.logic.auxparser.AuxParserResult;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

public class TexGroup extends AbstractGroup {
    private Path filePath;
    private Set<String> keysUsedInAux = null;

    public TexGroup(String name, GroupHierarchyType context, String filePath) {
        this(name, context, Paths.get(filePath));
    }

    public TexGroup(String name, GroupHierarchyType context, Path filePath) {
        super(name, context);
        this.filePath = filePath;
    }

    @Override
    public boolean contains(BibEntry entry) {
        if (keysUsedInAux == null) {
            AuxParser auxParser = new AuxParser(filePath, new BibDatabase());
            AuxParserResult auxResult = auxParser.parse();
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
        return new TexGroup(name, context, filePath.toString());
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
    public int hashCode() {
        return Objects.hash(super.hashCode(), filePath);
    }

    public Path getFilePath() {
        return filePath;
    }
}

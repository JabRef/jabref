package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.util.FileType;

/**
 * Object with data for a custom importer.
 *
 * <p>Is also responsible for instantiating the class loader.</p>
 */
public class CustomImporter extends Importer {

    private final String className;
    private final Path basePath;

    private Importer importer;

    public CustomImporter(String basePath, String className) throws ClassNotFoundException {
        this.basePath = Paths.get(basePath);
        this.className = className;
        try {
            importer = load(this.basePath.toUri().toURL(), this.className);
        } catch (IOException | InstantiationException | IllegalAccessException exception) {
            throw new ClassNotFoundException("", exception);
        }
    }

    private static Importer load(URL basePathURL, String className)
            throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        try (URLClassLoader cl = new URLClassLoader(new URL[] {basePathURL})) {
            Class<?> clazz = Class.forName(className, true, cl);
            return (Importer) clazz.newInstance();
        }
    }

    public List<String> getAsStringList() {
        return Arrays.asList(basePath.toString().replace('\\', '/'), className);
    }

    public String getClassName() {
        return className;
    }

    public Path getBasePath() {
        return basePath;
    }

    @Override
    public boolean equals(Object other) {

        if (this == other) {
            return true;
        }

        if (!(other instanceof CustomImporter)) {
            return false;
        }

        CustomImporter otherImporter = (CustomImporter) other;
        return Objects.equals(className, otherImporter.className) && Objects.equals(basePath, otherImporter.basePath);
    }

    @Override
    public boolean isRecognizedFormat(BufferedReader input) throws IOException {
        return importer.isRecognizedFormat(input);
    }

    @Override
    public ParserResult importDatabase(BufferedReader input) throws IOException {
        return importer.importDatabase(input);
    }

    @Override
    public String getName() {
        return importer.getName();
    }

    @Override
    public FileType getFileType() {
        return importer.getFileType();
    }

    @Override
    public String getId() {
        return importer.getId();
    }

    @Override
    public String getDescription() {
        return importer.getDescription();
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, basePath);
    }

    @Override
    public String toString() {
        return this.getName();
    }
}

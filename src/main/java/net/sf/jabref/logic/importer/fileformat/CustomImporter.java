package net.sf.jabref.logic.importer.fileformat;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Object with data for a custom importer.
 *
 * <p>Is also responsible for instantiating the class loader.</p>
 */
public class CustomImporter implements Comparable<CustomImporter> {

    private String name;
    private String cliId;
    private String className;
    private String basePath;


    public CustomImporter() {
        super();
    }

    public CustomImporter(List<String> data) {
        this(data.get(0), data.get(1), data.get(2), data.get(3));
    }

    public CustomImporter(String name, String cliId, String className, String basePath) {
        this();
        this.name = name;
        this.cliId = cliId;
        this.className = className;
        this.basePath = basePath;
    }

    public CustomImporter(ImportFormat importer) {
        this(importer.getFormatName(), importer.getId(), importer.getClass().getName(),
                "src/main/java/net/sf/jabref/logic/importer/fileformat/" + importer.getFormatName() + "Importer.java");
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClidId() {
        return this.cliId;
    }

    public void setCliId(String cliId) {
        this.cliId = cliId;
    }

    public String getClassName() {
        return this.className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String getBasePath() {
        return basePath;
    }

    public File getFileFromBasePath() {
        return new File(basePath);
    }

    public URL getBasePathUrl() throws MalformedURLException {
        return getFileFromBasePath().toURI().toURL();
    }

    public List<String> getAsStringList() {
        return Arrays.asList(name, cliId, className, basePath);
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
        return Objects.equals(name, otherImporter.name) && Objects.equals(cliId, otherImporter.cliId)
                && Objects.equals(className, otherImporter.className)
                && Objects.equals(basePath, otherImporter.basePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, cliId, className, basePath);
    }

    @Override
    public int compareTo(CustomImporter o) {
        return this.getName().compareTo(o.getName());
    }

    @Override
    public String toString() {
        return this.name;
    }

    public ImportFormat getInstance() throws IOException, ClassNotFoundException,
            InstantiationException, IllegalAccessException {
        try (URLClassLoader cl = new URLClassLoader(new URL[] {getBasePathUrl()})) {
            Class<?> clazz = Class.forName(className, true, cl);
            ImportFormat importFormat = (ImportFormat) clazz.newInstance();
            return importFormat;
        }
    }
}
package org.jabref.logic.openoffice.style;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.jabref.logic.bst.BstVM;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// An [OOStyle] backed by a `.bst` file - either an internal (classpath) style bundled with
/// JabRef or an external (filesystem) style supplied by the user.
@NullMarked
public class BstStyle implements OOStyle {

    /// Classpath resource paths for the built-in BST styles.
    public static final String INTERNAL_IEEETRAN_PATH = "/resource/openoffice/IEEEtran.bst";
    public static final String INTERNAL_ABBRV_PATH = "/resource/openoffice/abbrv.bst";
    public static final String INTERNAL_APA_PATH = "/resource/openoffice/apa.bst";

    private final boolean internal;
    /// Classpath resource path for internal styles; `null` for external styles.
    private final @Nullable String resourcePath;
    /// Filesystem path for external styles; `null` for internal styles.
    private final @Nullable Path filePath;
    private final String name;

    /// Creates an external (user-supplied) style backed by a filesystem path.
    public BstStyle(Path path) {
        this.internal = false;
        this.filePath = path;
        this.resourcePath = null;
        this.name = stripBstExtension(path.getFileName().toString());
    }

    private BstStyle(String resourcePath) {
        this.internal = true;
        this.resourcePath = resourcePath;
        this.filePath = null;
        this.name = stripBstExtension(Path.of(resourcePath).getFileName().toString());
    }

    /// Creates an internal style loaded from a classpath resource (e.g. `/resource/openoffice/IEEEtran.bst`).
    public static BstStyle createInternal(String resourcePath) {
        return new BstStyle(resourcePath);
    }

    /// Creates a [BstVM] for this style, reading from the filesystem or classpath as appropriate.
    public BstVM createBstVM() throws IOException {
        if (filePath != null) {
            return new BstVM(filePath);
        }
        assert resourcePath != null;
        try (InputStream is = BstStyle.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Internal BST resource not found: " + resourcePath);
            }
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return new BstVM(content);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isInternalStyle() {
        return internal;
    }

    /// For external styles returns the absolute filesystem path.
    /// For internal styles returns the classpath resource path (starts with `/resource/openoffice/`).
    /// This value is persisted by [JabRefCliPreferences] and used to reconstruct the style on startup.
    @Override
    public String getPath() {
        if (filePath != null) {
            return filePath.toString();
        }
        assert resourcePath != null;
        return resourcePath;
    }

    /// Returns the filesystem [Path] for external styles, or `null` for internal styles.
    public @Nullable Path getFilePath() {
        return filePath;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof BstStyle other) && getPath().equals(other.getPath());
    }

    @Override
    public int hashCode() {
        return getPath().hashCode();
    }

    @Override
    public String toString() {
        return "BstStyle{path=" + getPath() + ", internal=" + internal + "}";
    }

    private static String stripBstExtension(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".bst")) {
            return filename.substring(0, filename.length() - 4);
        }
        return filename;
    }
}

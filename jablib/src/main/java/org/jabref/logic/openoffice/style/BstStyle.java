package org.jabref.logic.openoffice.style;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jabref.logic.bst.BstVM;

import org.antlr.v4.runtime.misc.ParseCancellationException;
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
    private final String source;
    /// Parsed executable program for this specific `.bst` style.
    ///
    /// [BstVM] is **not** style-agnostic: it represents the parsed program of one concrete `.bst`
    /// file. We keep it on the style so repeated preview/citation/bibliography operations can reuse
    /// the parsed program and avoid reparsing and reallocating a new [BstVM] on each operation.
    private final BstVM bstVM;
    private final boolean hasSortCommand;

    private BstStyle(@Nullable String resourcePath, @Nullable Path filePath, boolean internal, String source, BstVM bstVM) {
        this.internal = internal;
        this.resourcePath = resourcePath;
        this.filePath = filePath;
        String filename = internal ? Path.of(resourcePath).getFileName().toString() : filePath.getFileName().toString();
        this.name = stripBstExtension(filename);
        this.source = source;
        this.bstVM = bstVM;
        this.hasSortCommand = bstVM.hasSortCommand();
    }

    /// Creates an external (user-supplied) style backed by a filesystem path.
    public static BstStyle loadExternal(Path path) throws IOException {
        String source = Files.readString(path);
        try {
            return new BstStyle(null, path, false, source, new BstVM(path));
        } catch (ParseCancellationException e) {
            throw new IOException("Could not parse BST style: " + path, e);
        }
    }

    /// Creates an internal style loaded from a classpath resource (e.g. `/resource/openoffice/IEEEtran.bst`).
    public static BstStyle createInternal(String resourcePath) throws IOException {
        try (InputStream is = BstStyle.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Internal BST resource not found: " + resourcePath);
            }
            String source = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            try {
                return new BstStyle(resourcePath, null, true, source, new BstVM(source));
            } catch (ParseCancellationException e) {
                throw new IOException("Could not parse internal BST style: " + resourcePath, e);
            }
        }
    }

    /// Returns the parsed [BstVM] for this style.
    public BstVM createBstVM() {
        return bstVM;
    }

    @Override
    public String getName() {
        return name;
    }

    public boolean hasSortCommand() {
        return hasSortCommand;
    }

    public String getSource() {
        return source;
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

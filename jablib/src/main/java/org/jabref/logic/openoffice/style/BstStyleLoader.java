package org.jabref.logic.openoffice.style;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.jabref.logic.openoffice.OpenOfficePreferences;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Loads and persists the list of external `.bst` style files, mirroring the pattern of [JStyleLoader].
/// There are no internal BST styles — all are user-supplied.
@NullMarked
public class BstStyleLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(BstStyleLoader.class);

    private final OpenOfficePreferences openOfficePreferences;
    private final List<BstStyle> styles = new ArrayList<>();

    public BstStyleLoader(OpenOfficePreferences openOfficePreferences) {
        this.openOfficePreferences = openOfficePreferences;
        loadStyles();
    }

    public List<BstStyle> getStyles() {
        return List.copyOf(styles);
    }

    /// Adds a `.bst` file to the list if it is valid (exists and has the right extension).
    ///
    /// @return `true` if the style was added, `false` if it was invalid, missing, or already present.
    public boolean addStyleIfValid(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        if (!name.endsWith(".bst")) {
            LOGGER.warn("Not a .bst file: {}", path);
            return false;
        }
        if (!Files.exists(path)) {
            LOGGER.warn("BST style file does not exist: {}", path);
            return false;
        }
        BstStyle style = new BstStyle(path);
        if (styles.contains(style)) {
            LOGGER.info("BST style already in list: {}", path);
            return false;
        }
        styles.add(style);
        storeStyles();
        return true;
    }

    public boolean removeStyle(BstStyle style) {
        boolean removed = styles.remove(style);
        if (removed) {
            storeStyles();
        }
        return removed;
    }

    private void loadStyles() {
        styles.clear();
        for (String pathStr : openOfficePreferences.getExternalBstStyles()) {
            Path p = Path.of(pathStr);
            if (Files.exists(p)) {
                styles.add(new BstStyle(p));
            } else {
                LOGGER.warn("BST style file not found, skipping: {}", pathStr);
            }
        }
    }

    private void storeStyles() {
        List<String> paths = styles.stream()
                                   .map(BstStyle::getPath)
                                   .toList();
        openOfficePreferences.setExternalBstStyles(paths);
    }
}

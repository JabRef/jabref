package org.jabref.logic.openoffice.style;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.jabref.logic.openoffice.OpenOfficePreferences;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Manages the list of available BST styles, mirroring [JStyleLoader].
///
/// Internal styles (IEEEtran, abbrv, APA) are bundled with JabRef and always available.
/// External styles are user-supplied `.bst` files stored in preferences.
@NullMarked
public class BstStyleLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(BstStyleLoader.class);

    private static final List<String> INTERNAL_STYLE_PATHS = List.of(
            BstStyle.INTERNAL_IEEETRAN_PATH,
            BstStyle.INTERNAL_ABBRV_PATH,
            BstStyle.INTERNAL_APA_PATH
    );

    private final OpenOfficePreferences openOfficePreferences;
    private final List<BstStyle> internalStyles = new ArrayList<>();
    private final List<BstStyle> externalStyles = new ArrayList<>();

    public BstStyleLoader(OpenOfficePreferences openOfficePreferences) {
        this.openOfficePreferences = openOfficePreferences;
        loadInternalStyles();
        loadExternalStyles();
    }

    /// Returns all styles - internal first, then external.
    public List<BstStyle> getStyles() {
        List<BstStyle> all = new ArrayList<>(internalStyles);
        all.addAll(externalStyles);
        return List.copyOf(all);
    }

    /// Adds a user-supplied `.bst` file if it exists, is not already present, and has the right extension.
    ///
    /// @return `true` if the style was added
    public boolean addStyleIfValid(Path path) {
        if (!path.getFileName().toString().toLowerCase().endsWith(".bst")) {
            LOGGER.warn("Not a .bst file: {}", path);
            return false;
        }
        if (!Files.exists(path)) {
            LOGGER.warn("BST style file does not exist: {}", path);
            return false;
        }
        BstStyle style = new BstStyle(path);
        if (externalStyles.contains(style)) {
            LOGGER.info("BST style already in list: {}", path);
            return false;
        }
        externalStyles.add(style);
        storeExternalStyles();
        return true;
    }

    public boolean removeStyle(BstStyle style) {
        if (style.isInternalStyle()) {
            return false;
        }
        boolean removed = externalStyles.remove(style);
        if (removed) {
            storeExternalStyles();
        }
        return removed;
    }

    private void loadInternalStyles() {
        internalStyles.clear();
        for (String resourcePath : INTERNAL_STYLE_PATHS) {
            internalStyles.add(BstStyle.createInternal(resourcePath));
        }
    }

    private void loadExternalStyles() {
        externalStyles.clear();
        for (String pathStr : openOfficePreferences.getExternalBstStyles()) {
            Path p = Path.of(pathStr);
            if (Files.exists(p)) {
                externalStyles.add(new BstStyle(p));
            } else {
                LOGGER.warn("BST style file not found, skipping: {}", pathStr);
            }
        }
    }

    private void storeExternalStyles() {
        List<String> paths = externalStyles.stream()
                                           .map(BstStyle::getPath)
                                           .toList();
        openOfficePreferences.setExternalBstStyles(paths);
    }
}

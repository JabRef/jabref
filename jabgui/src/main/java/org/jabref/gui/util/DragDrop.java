package org.jabref.gui.util;

import java.nio.file.Path;
import java.util.List;

import javafx.scene.input.TransferMode;

import org.jabref.gui.externalfiles.ExternalFilesEntryLinker;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DragDrop {
    private static final Logger LOGGER = LoggerFactory.getLogger(DragDrop.class);

    public static void handleDropOfFiles(List<Path> files, TransferMode transferMode, ExternalFilesEntryLinker fileLinker, BibEntry entry) {
        List<Path> nonBibFiles = files.stream().filter(file -> !FileUtil.isBibFile(file)).toList();
        if (nonBibFiles.isEmpty()) {
            return;
        }

        // Depending on the pressed modifier, move/copy/link files to drop target
        // Modifiers do not work on macOS: https://bugs.openjdk.org/browse/JDK-8264172
        switch (transferMode) {
            case COPY -> {
                LOGGER.debug("Mode Copy"); // ctrl on win, no modifier on Xubuntu
                fileLinker.coveOrMoveFilesSteps(entry, nonBibFiles, false);
            }
            case MOVE -> {
                LOGGER.debug("Mode MOVE"); // shift on win or no modifier
                fileLinker.coveOrMoveFilesSteps(entry, nonBibFiles, true);
            }
            case LINK -> {
                LOGGER.debug("Node LINK"); // alt on win
                fileLinker.linkFilesToEntry(entry, nonBibFiles);
            }
        }
    }
}

package org.jabref.gui;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javafx.scene.input.Dragboard;

import org.jabref.logic.util.io.FileUtil;

public class DragAndDropHelper {

    public static boolean hasBibFiles(Dragboard dragboard) {
        return !getBibFiles(dragboard).isEmpty();
    }

    public static List<Path> getBibFiles(Dragboard dragboard) {
        if (!dragboard.hasFiles()) {
            return Collections.emptyList();
        } else {
            return dragboard.getFiles().stream().map(File::toPath).filter(FileUtil::isBibFile).collect(Collectors.toList());
        }
    }
}

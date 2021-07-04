package org.jabref.gui.slr;

import java.nio.file.Path;

import org.jabref.model.study.Study;

public class SlrStudyAndDirectory {
    private final Study study;
    private final Path studyDirectory;

    public SlrStudyAndDirectory(Study study, Path studyDirectory) {
        this.study = study;
        this.studyDirectory = studyDirectory;
    }

    public Path getStudyDirectory() {
        return studyDirectory;
    }

    public Study getStudy() {
        return study;
    }
}

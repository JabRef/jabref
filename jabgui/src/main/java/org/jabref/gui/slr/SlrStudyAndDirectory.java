package org.jabref.gui.slr;

import java.nio.file.Path;

import org.jabref.model.study.Study;

import org.jspecify.annotations.NonNull;

public class SlrStudyAndDirectory {
    private final Study study;
    private final Path studyDirectory;

    public SlrStudyAndDirectory(@NonNull Study study, @NonNull Path studyDirectory) {
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

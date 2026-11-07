package org.jabref.model.undo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
class UndoableFileMoveTest {

    @TempDir
    private Path tempDir;

    // [utest->req~logic.pdf.split-proceedings-outline~1]
    @Test
    void undoingMovesTheFileAwayAndRedoingPutsItBack() throws IOException {
        Path paper = Files.writeString(tempDir.resolve("paper.pdf"), "paper");
        Path held = tempDir.resolve("held.pdf");
        UndoableFileMove move = new UndoableFileMove(List.of(new UndoableFileMove.Move(held, paper, Files.getLastModifiedTime(paper))));

        move.inverted().apply();
        List<Boolean> afterUndo = List.of(Files.exists(paper), Files.exists(held));
        move.apply();

        assertEquals(List.of(List.of(false, true), List.of(true, false), "paper"),
                List.of(afterUndo, List.of(Files.exists(paper), Files.exists(held)), Files.readString(paper)));
    }

    // [utest->req~logic.pdf.split-proceedings-outline~1]
    @Test
    void leavesAFileThatChangedSinceInPlace() throws IOException {
        Path paper = Files.writeString(tempDir.resolve("paper.pdf"), "paper");
        UndoableFileMove move = new UndoableFileMove(List.of(new UndoableFileMove.Move(tempDir.resolve("held.pdf"), paper, FileTime.fromMillis(0))));

        ApplyResult result = move.inverted().apply();

        assertEquals(List.of(true, false), List.of(Files.exists(paper), result.complete()));
    }

    @Test
    void leavesAFileWhoseTargetIsTakenInPlace() throws IOException {
        Path paper = Files.writeString(tempDir.resolve("paper.pdf"), "paper");
        Path held = Files.writeString(tempDir.resolve("held.pdf"), "other");
        UndoableFileMove move = new UndoableFileMove(List.of(new UndoableFileMove.Move(held, paper, Files.getLastModifiedTime(paper))));

        move.inverted().apply();

        assertEquals(List.of("paper", "other"), List.of(Files.readString(paper), Files.readString(held)));
    }
}

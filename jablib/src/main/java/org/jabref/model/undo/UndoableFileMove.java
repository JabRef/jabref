package org.jabref.model.undo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.NullMarked;

/// Moves files that a change to the library wrote, so that undoing the change takes the files out of their folder and redoing it puts them back.
///
/// A file that is no longer at its source, or that was written to since, is left where it is, and so is a file whose target is taken:
/// the user may have moved, edited or replaced it outside the library, and that work must not be moved out of sight.
///
/// @param moves the files to move, each from its `source` to its `target`
// [impl->req~logic.pdf.split-proceedings-outline~1]
@NullMarked
public record UndoableFileMove(List<Move> moves) implements BibChange {

    public UndoableFileMove {
        moves = List.copyOf(moves);
    }

    @Override
    public UndoableFileMove inverted() {
        return new UndoableFileMove(moves.stream().map(Move::inverted).toList());
    }

    @Override
    public ApplyResult apply() {
        List<String> reasonsLeftInPlace = moves.stream().flatMap(move -> move.perform().stream()).toList();
        if (reasonsLeftInPlace.isEmpty()) {
            return ApplyResult.SUCCESS;
        }
        return ApplyResult.of(this, String.join("; ", reasonsLeftInPlace));
    }

    /// @param lastModified when the file was last written, which tells whether it changed since
    public record Move(Path source, Path target, FileTime lastModified) {
        Move inverted() {
            return new Move(target, source, lastModified);
        }

        /// @return why the file was left in place, or empty when it was moved
        Optional<String> perform() {
            try {
                if (!Files.exists(source) || !Files.getLastModifiedTime(source).equals(lastModified)) {
                    return Optional.of(source + " is gone or was changed");
                }
                if (Files.exists(target)) {
                    return Optional.of(target + " already exists");
                }
                Files.move(source, target);
                return Optional.empty();
            } catch (IOException exception) {
                return Optional.of(source + " could not be moved: " + exception.getMessage());
            }
        }
    }
}

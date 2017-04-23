package org.jabref.logic.exporter;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jabref.model.FieldChange;

public abstract class SaveSession {

    protected boolean backup;
    protected final Charset encoding;
    protected final VerifyingWriter writer;
    private final List<FieldChange> undoableFieldChanges = new ArrayList<>();

    protected SaveSession(Charset encoding, boolean backup, VerifyingWriter writer) {
        this.encoding = Objects.requireNonNull(encoding);
        this.backup = backup;
        this.writer = Objects.requireNonNull(writer);
    }

    public VerifyingWriter getWriter() {
        return writer;
    }

    public Charset getEncoding() {
        return encoding;
    }

    public void setUseBackup(boolean useBackup) {
        this.backup = useBackup;
    }

    public abstract void commit(Path file) throws SaveException;

    public void commit(String path) throws SaveException {
        commit(Paths.get(path));
    }

    public abstract void cancel();

    public List<FieldChange> getFieldChanges() {
        return undoableFieldChanges;
    }

    public void addFieldChanges(List<FieldChange> newUndoableFieldChanges) {
        this.undoableFieldChanges.addAll(newUndoableFieldChanges);
    }
}

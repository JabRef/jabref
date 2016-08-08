/*
 * Copyright (C) 2003-2016 JabRef contributors.
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package net.sf.jabref.logic.exporter;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.sf.jabref.model.FieldChange;

public abstract class SaveSession {

    protected final Charset encoding;
    protected final VerifyingWriter writer;
    private final List<FieldChange> undoableFieldChanges = new ArrayList<>();
    protected boolean backup;

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

/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.
    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.logic.cleanup;

import java.util.List;

import net.sf.jabref.logic.FieldChange;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.util.CleanupUtil;

public class RenamePdfCleanup implements CleanupJob {

    private final List<String> paths;
    private final BibDatabase database;
    private final Boolean onlyRelativePaths;
    private int unsuccessfulRenames;


    public RenamePdfCleanup(List<String> paths, Boolean onlyRelativePaths, BibDatabase database) {
        this.paths = paths;
        this.database = database;
        this.onlyRelativePaths = onlyRelativePaths;
    }

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        return CleanupUtil.renameFiles(entry, onlyRelativePaths, database, paths);
    }

    public int getUnsuccessfulRenames() {
        return unsuccessfulRenames;
    }
}

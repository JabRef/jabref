/**
 * Copyright (C) 2015 JabRef contributors
 *
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
package net.sf.jabref.imports;

import java.io.File;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.external.ExternalFileType;

/** EntryCreator for any predefined ExternalFileType.
 * This Creator accepts all files with the extension defined in the ExternalFileType.
 */
public class EntryFromExternalFileCreator extends EntryFromFileCreator {

    public EntryFromExternalFileCreator(ExternalFileType externalFileType) {
        super(externalFileType);
    }

    @Override
    public boolean accept(File f) {
        return f.getName().endsWith("." + externalFileType.getExtension());
    }

    @Override
    protected BibtexEntry createBibtexEntry(File file) {
        if (!accept(file)) {
            return null;
        }

        return new BibtexEntry();
    }

    @Override
    public String getFormatName() {
        return externalFileType.getName();
    }
}

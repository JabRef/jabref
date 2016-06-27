/*  Copyright (C) 2003-2016 JabRef contributors.
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
package net.sf.jabref.importer;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.sf.jabref.external.ExternalFileType;
import net.sf.jabref.external.ExternalFileTypes;
import net.sf.jabref.logic.l10n.Localization;

/**
 * The class EntryFromFileCreatorManager manages entry creators.
 * The manager knows all existing implementations of the interface EntryFromFileCreator.
 * Given a file, the manager can then provide a creator, which is able to create a Bibtex entry for his file.
 * Knowing all implementations of the interface, the manager also knows the set of all files, of which Bibtex entries can be created.
 * The GUI uses this capability for offering the user only such files, of which entries could actually be created.
 * @author Dan&Nosh
 *
 */
public final class EntryFromFileCreatorManager {

    private final List<EntryFromFileCreator> entryCreators;


    public EntryFromFileCreatorManager() {

        entryCreators = new ArrayList<>(10);
        entryCreators.add(new EntryFromPDFCreator());

        // add a creator for each ExternalFileType if there is no specialized
        // creator existing.
        Collection<ExternalFileType> fileTypes = ExternalFileTypes.getInstance().getExternalFileTypeSelection();

        for (ExternalFileType exFileType : fileTypes) {
            if (!hasSpecialisedCreatorForExternalFileType(exFileType)) {
                entryCreators.add(new EntryFromExternalFileCreator(exFileType));
            }
        }
    }

    private boolean hasSpecialisedCreatorForExternalFileType(ExternalFileType externalFileType) {
        for (EntryFromFileCreator entryCreator : entryCreators) {
            if ((entryCreator.getExternalFileType() == null)
                    || (entryCreator.getExternalFileType().getExtension().isEmpty())) {
                continue;
            }
            if (entryCreator.getExternalFileType().getExtension().equals(externalFileType.getExtension())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a EntryFromFileCreator object that is capable of creating a
     * BibEntry for the given File.
     *
     * @param file the pdf file
     * @return null if there is no EntryFromFileCreator for this File.
     */
    public EntryFromFileCreator getEntryCreator(File file) {
        if ((file == null) || !file.exists()) {
            return null;
        }
        for (EntryFromFileCreator creator : entryCreators) {
            if (creator.accept(file)) {
                return creator;
            }
        }
        return null;
    }

    /**
     * Returns a {@link FileFilter} instance which will accept all files, for
     * which a {@link EntryFromFileCreator} exists, that accepts the files. <br>
     * <br>
     * This {@link FileFilter} will be displayed in the GUI as
     * "All supported files".
     *
     * @return A {@link FileFilter} that accepts all files for which creators
     *         exist.
     */
    private FileFilter getFileFilter() {
        return new FileFilter() {

            /**
             * Accepts all files, which are accepted by any known creator.
             */
            @Override
            public boolean accept(File file) {
                for (EntryFromFileCreator creator : entryCreators) {
                    if (creator.accept(file)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public String toString() {
                return Localization.lang("All external files");
            }
        };
    }

    /**
     * Returns a list of all {@link FileFilter} instances (i.e.
     * {@link EntryFromFileCreator}, plus the file filter that comes with the
     * {@link #getFileFilter()} method.
     *
     * @return A List of all known possible file filters.
     */
    public List<FileFilter> getFileFilterList() {

        List<FileFilter> filters = new ArrayList<>();
        filters.add(getFileFilter());
        for (FileFilter creator : entryCreators) {
            filters.add(creator);
        }
        return filters;
    }
}

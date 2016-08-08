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

package net.sf.jabref.logic.importer;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import net.sf.jabref.model.entry.BibEntry;

/**
 * This interface is used for classes that try to resolve a full-text PDF url for a BibTex entry.
 * Implementing classes should specialize on specific article sites.
 * See e.g. @link{http://libguides.mit.edu/apis}.
 */
@FunctionalInterface
public interface FulltextFetcher {
    /**
     * Tries to find a fulltext URL for a given BibTex entry.
     *
     * @param entry The Bibtex entry
     * @return The fulltext PDF URL Optional, if found, or an empty Optional if not found.
     * @throws NullPointerException if no BibTex entry is given
     * @throws java.io.IOException
     */
    Optional<URL> findFullText(BibEntry entry) throws IOException;
}

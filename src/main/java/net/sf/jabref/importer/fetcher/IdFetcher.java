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

package net.sf.jabref.importer.fetcher;

import net.sf.jabref.model.entry.BibEntry;

/**
 * Looks for article identifier based on already present bibliographic information.
 */
public interface IdFetcher {

    /**
     * Looks for an identifier based on the information stored in the given {@link BibEntry} and
     * then updates the {@link BibEntry} with the found id.
     *
     * @param entry the {@link BibEntry} for which an identifier should be found
     * @return an updated {@link BibEntry} containing the identifier (if an ID was found, otherwise the {@link BibEntry}
     *         is left unchanged)
     */
    BibEntry updateIdentfier(BibEntry entry);
}

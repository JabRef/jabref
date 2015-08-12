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

import net.sf.jabref.OutputPrinter;
import net.sf.jabref.gui.FetcherPreviewDialog;

import java.util.Map;

/**
 *
 */
interface PreviewEntryFetcher extends EntryFetcher {

    boolean processQueryGetPreview(String query, FetcherPreviewDialog preview,
                                   OutputPrinter status);

    void getEntries(Map<String, Boolean> selection, ImportInspector inspector);

    /**
     * The number of entries a user can select for download without getting a warning message.
     * @return the warning limit
     */
    int getWarningLimit();

    /**
     * The preferred table row height for the previews.
     * @return the preferred height
     */
    int getPreferredPreviewHeight();

}

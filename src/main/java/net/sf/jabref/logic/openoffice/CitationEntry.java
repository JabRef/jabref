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
package net.sf.jabref.logic.openoffice;

import java.util.Optional;

public class CitationEntry implements Comparable<CitationEntry> {

    private final String refMarkName;
    private Optional<String> pageInfo;
    private final String context;
    private final Optional<String> origPageInfo;


    // Only used for testing...
    public CitationEntry(String refMarkName, String context) {
        this(refMarkName, context, Optional.empty());
    }

    // Only used for testing...
    public CitationEntry(String refMarkName, String context, String pageInfo) {
        this(refMarkName, context, Optional.ofNullable(pageInfo));
    }

    public CitationEntry(String refMarkName, String context, Optional<String> pageInfo) {
        this.refMarkName = refMarkName;
        this.context = context;
        this.pageInfo = pageInfo;
        this.origPageInfo = pageInfo;
    }

    public Optional<String> getPageInfo() {
        return pageInfo;
    }

    public String getRefMarkName() {
        return refMarkName;
    }

    public boolean pageInfoChanged() {
        if (pageInfo.isPresent() ^ origPageInfo.isPresent()) {
            return true;
        }
        if (pageInfo.isPresent()) {
            // This means that origPageInfo.isPresent is also true here
            return pageInfo.get().compareTo(origPageInfo.get()) != 0;
        } else {
            // So origPageInfo.isPresent is false here
            return false;
        }
    }

    @Override
    public int compareTo(CitationEntry other) {
        return this.refMarkName.compareTo(other.refMarkName);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof CitationEntry) {
            CitationEntry other = (CitationEntry) o;
            return this.refMarkName.equals(other.refMarkName);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.refMarkName.hashCode();
    }

    public String getContext() {
        return context;
    }

    public void setPageInfo(String trim) {
        pageInfo = Optional.ofNullable(trim);
    }
}

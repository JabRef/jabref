package org.jabref.gui.openoffice;

/**
 * Identifies a citation with the citation group containing it and
 * its storage index within.
 */
class CitationPath {
    CitationGroupID group;
    int storageIndexInGroup;
    CitationPath(CitationGroupID group,
                 int storageIndexInGroup) {
        this.group = group;
        this.storageIndexInGroup = storageIndexInGroup;
    }
}

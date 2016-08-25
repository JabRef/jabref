package net.sf.jabref.logic.groups;

import net.sf.jabref.logic.l10n.Localization;

class UnsupportedVersionException extends Exception {

    public UnsupportedVersionException(String groupType, int version) {
        super(Localization.lang("Unsupported version of class %0: %1", groupType, Integer.toString(version)));
    }
}

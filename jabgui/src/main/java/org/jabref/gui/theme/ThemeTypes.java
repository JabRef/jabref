package org.jabref.gui.theme;

import org.jabref.logic.l10n.Localization;

public enum ThemeTypes {

        LIGHT(Localization.lang("Light")),
        DARK(Localization.lang("Dark")),
        CUSTOM(Localization.lang("Custom..."));

        private final String displayName;

        ThemeTypes(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
}

// src/test/java/org/jabref/gui/preferences/websearch/WebSearchPrefChainTest.java
package org.jabref.gui.preferences.websearch;

import org.jabref.logic.preferences.JabRefCliPreferences;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class WebSearchPrefChainTest {

    @Test
    void cliDefaults_propagateToImporterPreferences() {
        var cli = new JabRefCliPreferences();
        var importer = cli.getImporterPreferences();
        assertThat(importer.isPreferInspireTexkeys()).isTrue(); // 来自 defaults
    }

    @Test
    void changeProp_writesBackViaEasyBind() {
        var cli = new JabRefCliPreferences();
        var importer = cli.getImporterPreferences();
        importer.setPreferInspireTexkeys(false);

        var reloaded = cli.getImporterPreferences();
        assertThat(reloaded.isPreferInspireTexkeys()).isFalse();
    }
}


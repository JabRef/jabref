package net.sf.jabref.logic.l10n;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Checks that all property files are correctly encoded and can be loaded without errors.
 */
public class PropertiesLocaleCompletenessTest {

    @Test
    public void testi10nFiles() throws IOException {
        for(Path p : Files.list(Paths.get("src/main/resources/l10n")).collect(Collectors.toList())) {
            String[] parts = p.getFileName().toString().split("\\.")[0].split("_");
            String prefix = "l10n/" + parts[0];
            Locale locale;
            if(parts.length == 3) {
                locale = new Locale(parts[1], parts[2]);
            }  else {
                locale = new Locale(parts[1]);
            }

            checkPropertiesFile(locale, prefix);
        }
    }

    @Test
    public void testCompletenessOfBundles() {
        for(String lang : Languages.LANGUAGES.values()) {
            Path menuPropertyFile = Paths.get("src/main/resources").resolve(Localization.MENU_RESOURCE_PREFIX + "_" + lang + ".properties");
            assertTrue(Files.exists(menuPropertyFile));
            Path messagePropertyFile = Paths.get("src/main/resources").resolve(Localization.RESOURCE_PREFIX + "_" + lang + ".properties");
            assertTrue(Files.exists(messagePropertyFile));
        }
    }

    private void checkPropertiesFile(Locale locale, String prefix) {
        Locale oldLocale = Locale.getDefault();
        try {
            Locale.setDefault(locale);

            ResourceBundle.getBundle(prefix, locale, new EncodingControl(StandardCharsets.UTF_8));
        } finally {
            Locale.setDefault(oldLocale);
        }
    }

}

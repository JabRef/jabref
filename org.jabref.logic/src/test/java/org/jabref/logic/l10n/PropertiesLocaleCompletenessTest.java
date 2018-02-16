package org.jabref.logic.l10n;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Splitter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that all property files are correctly encoded and can be loaded without errors.
 */
public class PropertiesLocaleCompletenessTest {

    @Test
    public void testi10nFiles() throws IOException {
        try (Stream<Path> pathStream = Files.list(Paths.get("src/main/resources/l10n"))) {
            for (Path p : pathStream.collect(Collectors.toList())) {

                String[] parts = getParts(p);
                String prefix = "l10n/" + parts[0];
                Locale locale;
                if (parts.length == 3) {
                    locale = new Locale(parts[1], parts[2]);
                } else {
                    locale = new Locale(parts[1]);
                }

                checkPropertiesFile(locale, prefix);
            }
        }
    }

    private String[] getParts(Path p) {
        List<String> elements = Splitter.on("_").splitToList(p.getFileName().toString().split("\\.")[0]);
        String[] parts = new String[elements.size()];
        elements.toArray(parts);
        return parts;
    }

    @Test
    public void testCompletenessOfBundles() {
        for (String lang : Languages.LANGUAGES.values()) {
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

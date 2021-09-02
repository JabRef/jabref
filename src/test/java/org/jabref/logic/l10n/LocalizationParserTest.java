package org.jabref.logic.l10n;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

public class LocalizationParserTest {

    @Test
    public void simpleLangCallHasNoComments() {
        List<LocalizationParser.LocalizationLangCallData> localizationParametersInJavaFile = LocalizationParser.getLocalizationParametersInJavaFile(Path.of("src/test/java/org/jabref/logic/l10n/parsing/ParseTestClass.java"), LocalizationBundleForTest.LANG);
    }

}

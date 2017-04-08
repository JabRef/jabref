package org.jabref;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jabref.model.strings.StringUtil;

import org.junit.Assert;
import org.junit.Test;

public class CodeStyleTests {

    @Test
    public void StringUtilClassIsSmall() throws Exception {
        Path path = Paths.get("src", "main", "java", StringUtil.class.getName().replace('.', '/') + ".java");
        int lineCount = Files.readAllLines(path, StandardCharsets.UTF_8).size();

        Assert.assertTrue("StringUtil increased in size. "
                + "We try to keep this class as small as possible. "
                + "Thus think twice if you add something to StringUtil.", lineCount <= 715);
    }
}

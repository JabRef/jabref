package org.jabref.gui.externalfiles;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import org.jabref.logic.l10n.Localization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum FileIgnoreUnlinkedFiles {
    DEFAULT(Localization.lang("Default")),
    INCLUDE_ALL(Localization.lang("Include All Files"));

    private static FileIgnoreUnlinkedFiles instance;
    private static String gitIgnorePath = ".gitignore";
    private static Set<String> IgnoreFileSet = new HashSet<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(FileIgnoreUnlinkedFiles.class);

    private final String fileIgnoreOption;
    FileIgnoreUnlinkedFiles(String fileIgnoreOption) {
        this.fileIgnoreOption = fileIgnoreOption;
    }

    public String getFileIgnoreOption() {
        return fileIgnoreOption;
    }

    public static Set<String> getIgnoreFileSet() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(gitIgnorePath)
        ))) {
            String line = br.readLine();
            while (line != null) {
                line = br.readLine();
                if (line != null && line.length() > 2 && line.charAt(0) == '*') {
                    IgnoreFileSet.add(line.substring(2));
                } else if (line != null && line.length() > 1 && line.charAt(0) == '.') {
                    IgnoreFileSet.add(line.substring(1));
                } else if (line != null && line.length() > 1 && (!line.contains("/"))) {
                    IgnoreFileSet.add(line);
                }
            }
        } catch (IOException e) {
            LOGGER.error("No such file.");
        }
        return IgnoreFileSet;
    }
}

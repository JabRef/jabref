package org.jabref.gui.externalfiles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

public class FileIgnore {

    private static FileIgnore instance;
    private static Set<String> IgnoreFileSet = new HashSet<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(FileIgnore.class);
    private String gitIgnorePath = ".gitignore";

    public FileIgnore() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(gitIgnorePath)
        ))) {
            String line = "";
            line = br.readLine();
            while (line != null) {
                line = br.readLine();
                if (line != null && line.length() > 2 && line.charAt(0) == '*') {
                    IgnoreFileSet.add(line.substring(2));
                }else if (line != null && line.length() > 1 && line.charAt(0) == '.'){
                    IgnoreFileSet.add(line.substring(1));
                }
            }
        } catch (IOException e) {
            LOGGER.error("No such file.");
        }
    }

    public Set<String> getIgnoreFileSet(){
        return IgnoreFileSet;
    }

    public static FileIgnore getInstance(){
        if (instance == null){
            return new FileIgnore();
        }
        return instance;
    }
}

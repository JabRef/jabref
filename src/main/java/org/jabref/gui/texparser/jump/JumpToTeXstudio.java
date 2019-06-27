package org.jabref.gui.texparser.jump;

import java.io.IOException;
import java.nio.file.Path;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;

public class JumpToTeXstudio {

    public JumpToTeXstudio() {
    }

    public String getApplicationName() {
        return "TeXstudio";
    }

    public JabRefIcon getIcon() {
        return IconTheme.JabRefIcons.APPLICATION_TEXSTUDIO;
    }

    public void run(Path file, int line, int column) throws IOException {
        String command = String.format("texstudio --line %s:%s %s", line, column, file.toAbsolutePath());
        Runtime.getRuntime().exec(command);
    }
}

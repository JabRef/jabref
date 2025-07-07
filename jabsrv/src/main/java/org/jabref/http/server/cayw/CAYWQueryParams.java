package org.jabref.http.server.cayw;

import java.util.Optional;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;

public class CAYWQueryParams {

    /// See documentation here https://retorque.re/zotero-better-bibtex/citing/cayw/index.html#diy

    @QueryParam("probe")
    private String probe;

    @QueryParam("format")
    @DefaultValue("biblatex")
    private String format;

    @QueryParam("clipboard")
    private String clipboard;

    @QueryParam("command")
    @DefaultValue("autocite")
    private String command;

    @QueryParam("minimize")
    private String minimize;

    @QueryParam("texstudio")
    private String texstudio;

    @QueryParam("selected")
    private String selected;

    @QueryParam("select")
    private String select;

    @QueryParam("librarypath")
    private String libraryPath;

    @QueryParam("libraryid")
    private String libraryId;

    public String getCommand() {
        return command;
    }

    public boolean isClipboard() {
        return clipboard != null;
    }

    public boolean isTexstudio() {
        return texstudio != null;
    }

    public boolean isSelected() {
        return selected != null;
    }

    public boolean isSelect() {
        return "true".equalsIgnoreCase(select);
    }

    public boolean isProbe() {
        return probe != null;
    }

    public boolean isMinimize() {
        return minimize != null;
    }

    public String getFormat() {
        return format;
    }

    public Optional<String> getLibraryPath() {
        return Optional.ofNullable(libraryPath);
    }

    public Optional<String> getLibraryId() {
        return Optional.ofNullable(libraryId);
    }
}

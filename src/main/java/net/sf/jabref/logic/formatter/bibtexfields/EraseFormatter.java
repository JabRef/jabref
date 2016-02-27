package net.sf.jabref.logic.formatter.bibtexfields;

import net.sf.jabref.logic.formatter.Formatter;

public class EraseFormatter implements Formatter {

    @Override
    public String getName() {
        return "Erase all";
    }

    @Override
    public String getKey() {
        return "EraseFormatter";
    }

    @Override
    public String format(String oldString) {
        return null;
    }
}

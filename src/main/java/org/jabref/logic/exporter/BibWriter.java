package org.jabref.logic.exporter;

import java.io.IOException;
import java.io.Writer;

import org.jabref.model.strings.StringUtil;

/**
 * Class to write to a .bib file. Used by {@link BibtexDatabaseWriter}
 */
public class BibWriter {

    private final String newLineSeparator;
    private final Writer writer;

    private boolean precedingNewLineRequired = false;
    private boolean somethingWasWritten = false;
    private boolean lastWriteWasNewline = false;

    /**
     * @param newLineSeparator the string used for a line break
     */
    public BibWriter(Writer writer, String newLineSeparator) {
        this.writer = writer;
        this.newLineSeparator = newLineSeparator;
    }

    /**
     * Writes the given string. The newlines of the given string are converted to the newline set for this clas
     */
    public void write(String string) throws IOException {
        if (precedingNewLineRequired) {
            writer.write(newLineSeparator);
            precedingNewLineRequired = false;
        }
        string = StringUtil.unifyLineBreaks(string, newLineSeparator);
        writer.write(string);
        lastWriteWasNewline = string.endsWith(newLineSeparator);
        somethingWasWritten = true;
    }

    /**
     * Writes the given string and finishes it with a line break
     */
    public void writeLine(String string) throws IOException {
        this.write(string);
        this.finishLine();
    }

    /**
     * Finishes a line
     */
    public void finishLine() throws IOException {
        if (!this.lastWriteWasNewline) {
            this.write(newLineSeparator);
        }
    }

    /**
     * Finishes a block
     */
    public void finishBlock() throws IOException {
        if (!somethingWasWritten) {
            return;
        }
        if (!lastWriteWasNewline) {
            this.finishLine();
        }
        this.somethingWasWritten = false;
        this.precedingNewLineRequired = true;
    }
}

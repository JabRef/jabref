package net.sf.jabref.gui.journals;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import net.sf.jabref.Globals;
import net.sf.jabref.logic.journals.Abbreviation;
import net.sf.jabref.logic.journals.JournalAbbreviationLoader;

import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

public class AbbreviationsFile extends File {

    private final SimpleListProperty<Abbreviation> abbreviations = new SimpleListProperty<>(
            FXCollections.observableArrayList());


    public AbbreviationsFile(File file) {
        super(file.getAbsolutePath());
    }

    public void readAbbreviations() throws FileNotFoundException {
        abbreviations.addAll(JournalAbbreviationLoader.readJournalListFromFile(this));
    }

    public SimpleListProperty<Abbreviation> abbreviationsProperty() {
        return this.abbreviations;
    }

    public void WriteOrCreate() throws FileNotFoundException, IOException {
        try (FileOutputStream stream = new FileOutputStream(this, false);
                OutputStreamWriter writer = new OutputStreamWriter(stream, Globals.prefs.getDefaultEncoding())) {
            for (Abbreviation entry : abbreviations.get()) {
                writer.write(entry.getName());
                writer.write(" = ");
                writer.write(entry.getAbbreviation());
                writer.write(Globals.NEWLINE);
            }
        }
    }

}

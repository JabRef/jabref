package net.sf.jabref.gui.journals;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import net.sf.jabref.Globals;
import net.sf.jabref.logic.journals.Abbreviation;
import net.sf.jabref.logic.journals.JournalAbbreviationLoader;

public class AbbreviationsFile {

    private final SimpleListProperty<Abbreviation> abbreviations = new SimpleListProperty<>(
            FXCollections.observableArrayList());
    private final Path path;


    public AbbreviationsFile(String filePath) {
        path = Paths.get(filePath);
    }

    public void readAbbreviations() throws FileNotFoundException {
        abbreviations.addAll(JournalAbbreviationLoader.readJournalListFromFile(path.toFile()));
    }

    public SimpleListProperty<Abbreviation> abbreviationsProperty() {
        return this.abbreviations;
    }

    public void WriteOrCreate() throws IOException {
        try (OutputStream outStream = Files.newOutputStream(path);
                OutputStreamWriter writer = new OutputStreamWriter(outStream, Globals.prefs.getDefaultEncoding())) {
            for (Abbreviation entry : abbreviations.get()) {
                writer.write(entry.getName());
                writer.write(" = ");
                writer.write(entry.getAbbreviation());
                writer.write(Globals.NEWLINE);
            }
        }
    }

    public boolean exists() {
        return Files.exists(path);
    }

    public String getAbsolutePath() {
        return path.toString();
    }

    private Path getPath() {
        return this.path;
    }

    @Override
    public String toString() {
        return path.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AbbreviationsFile) {
            return Objects.equals(this.path, ((AbbreviationsFile) obj).getPath());
        } else {
            return false;
        }
    }

}

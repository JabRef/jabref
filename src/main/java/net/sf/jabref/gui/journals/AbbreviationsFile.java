/*  Copyright (C) 2016 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.gui.journals;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import net.sf.jabref.Globals;
import net.sf.jabref.logic.journals.Abbreviation;
import net.sf.jabref.logic.journals.JournalAbbreviationLoader;

/**
 * This class provides a model for abbreviation files.
 * It actually doesn't save the files as objects but rather saves
 * their paths. This also allows to specify pseudo files as placeholder objects.
 */
public class AbbreviationsFile {

    private final SimpleListProperty<AbbreviationViewModel> abbreviations = new SimpleListProperty<>(
            FXCollections.observableArrayList());
    private final ReadOnlyBooleanProperty isPseudoFile;
    private final String name;
    private final Path path;


    public AbbreviationsFile(String filePath) {
        this.path = Paths.get(filePath);
        this.name = path.toString();
        this.isPseudoFile = new SimpleBooleanProperty(false);
        this.abbreviations.add(new AbbreviationViewModel(null));
    }

    /**
     * This constructor should only be called to create a pseudo abbreviation file for built in lists.
     * This means it is a placeholder and it's path will be null meaning it has no place on the filesystem.
     * It's isPseudoFile property will therefore be set to true.
     */
    public AbbreviationsFile(List<AbbreviationViewModel> abbreviations, String name) {
        this.abbreviations.addAll(abbreviations);
        this.name = name;
        this.path = null;
        this.isPseudoFile = new SimpleBooleanProperty(true);
    }

    public void readAbbreviations() throws FileNotFoundException {
        List<Abbreviation> abbreviationList = JournalAbbreviationLoader.readJournalListFromFile(path.toFile());
        abbreviationList.forEach(abbreviation -> abbreviations.addAll(new AbbreviationViewModel(abbreviation)));
    }

    /**
     * This method will write all abbreviations of this abbreviation file to the file on the file system.
     * If the file exists its content will be overridden, otherwise a new file will be created.
     *
     * @throws IOException
     */
    public void WriteOrCreate() throws IOException {
        if (!isPseudoFile.get()) {
            try (OutputStream outStream = Files.newOutputStream(path);
                    OutputStreamWriter writer = new OutputStreamWriter(outStream, Globals.prefs.getDefaultEncoding())) {
                for (AbbreviationViewModel entry : abbreviations.get()) {
                    if (!entry.isPseudoAbbreviation()) {
                        writer.write(entry.getName());
                        writer.write(" = ");
                        writer.write(entry.getAbbreviation());
                        writer.write(Globals.NEWLINE);
                    }
                }
            }
        }
    }

    public SimpleListProperty<AbbreviationViewModel> abbreviationsProperty() {
        return this.abbreviations;
    }

    public boolean exists() {
        return Files.exists(path);
    }

    public String getAbsolutePath() {
        return path.toString();
    }

    public ReadOnlyBooleanProperty isPseudoFileProperty() {
        return this.isPseudoFile;
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AbbreviationsFile) {
            return Objects.equals(this.name, ((AbbreviationsFile) obj).name);
        } else {
            return false;
        }
    }

}

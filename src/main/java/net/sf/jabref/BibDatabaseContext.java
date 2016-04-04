package net.sf.jabref;

import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.database.BibDatabaseModeDetection;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents everything related to a .bib file.
 * <p>
 * The entries are stored in BibDatabase, the other data in MetaData and the options relevant for this file in Defaults.
 */
public class BibDatabaseContext {

    private final BibDatabase database;
    private final MetaData metaData;
    private final Defaults defaults;

    public BibDatabaseContext() {
        this(new Defaults());
    }

    public BibDatabaseContext(Defaults defaults) {
        this(new BibDatabase(), defaults);
    }

    public BibDatabaseContext(BibDatabase database, Defaults defaults) {
        this(database, new MetaData(), defaults);
    }

    public BibDatabaseContext(BibDatabase database, MetaData metaData, Defaults defaults) {
        this.defaults = Objects.requireNonNull(defaults);
        this.database = Objects.requireNonNull(database);
        this.metaData = Objects.requireNonNull(metaData);
    }

    public BibDatabaseContext(BibDatabase database, MetaData metaData, File file, Defaults defaults) {
        this(database, metaData, defaults);

        this.metaData.setFile(file);
    }

    public BibDatabaseMode getMode() {
        Optional<BibDatabaseMode> mode = metaData.getMode();

        if (!mode.isPresent()) {
            BibDatabaseMode inferredMode = BibDatabaseModeDetection.inferMode(database);
            BibDatabaseMode newMode = BibDatabaseMode.BIBTEX;
            if ((defaults.mode == BibDatabaseMode.BIBLATEX) || (inferredMode == BibDatabaseMode.BIBLATEX)) {
                newMode =  BibDatabaseMode.BIBLATEX;
            }
            this.setMode(newMode);
            return newMode;
        }
        return mode.get();
    }

    public void setMode(BibDatabaseMode bibDatabaseMode) {
        metaData.setMode(bibDatabaseMode);
    }

    /**
     * Get the file where this database was last saved to or loaded from, if any.
     *
     * @return The relevant File, or null if none is defined.
     */
    public File getDatabaseFile() {
        return metaData.getFile();
    }

    public BibDatabase getDatabase() {
        return database;
    }

    public MetaData getMetaData() {
        return metaData;
    }

    public boolean isBiblatexMode() {
        return getMode() == BibDatabaseMode.BIBLATEX;
    }
}

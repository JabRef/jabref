package net.sf.jabref;

import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.database.BibDatabaseModeDetection;

import java.io.File;
import java.util.Objects;
import java.util.Vector;

public class LoadedDatabase {

    private final BibDatabase database;
    private final MetaData metaData;

    public LoadedDatabase() {
        this(new BibDatabase());
    }

    public LoadedDatabase(BibDatabase database) {
        this(database, new MetaData());
    }

    public LoadedDatabase(BibDatabase database, MetaData metaData) {
        this.database = Objects.requireNonNull(database);
        this.metaData = Objects.requireNonNull(metaData);

        this.setMode(getMode());
    }

    public LoadedDatabase(BibDatabase database, MetaData metaData, File file) {
        this(database, metaData);

        this.metaData.setFile(file);
    }

    public BibDatabaseMode getMode() {
        Vector<String> data = metaData.getData(MetaData.DATABASE_TYPE);
        if(data == null) {
            return BibDatabaseModeDetection.inferMode(database);
        }
        return BibDatabaseMode.valueOf(data.get(0));
    }

    public void setMode(BibDatabaseMode bibDatabaseMode) {
        Vector<String> list = new Vector<>();
        list.add(bibDatabaseMode.name());
        metaData.putData(MetaData.DATABASE_TYPE, list);
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

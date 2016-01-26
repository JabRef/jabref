package net.sf.jabref;

import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.BibDatabaseType;

import java.io.File;
import java.util.Objects;
import java.util.Vector;

public class LoadedDatabase {

    private static final String DATABASE_TYPE = "DATABASE_TYPE";

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
    }

    public LoadedDatabase(BibDatabase database, MetaData metaData, File file) {
        this(database, metaData);
        this.metaData.setFile(file);
    }

    public BibDatabaseType getType() {
        return BibDatabaseType.valueOf(metaData.getData(DATABASE_TYPE).get(0));
    }

    public void setType(BibDatabaseType type) {
        Vector<String> list = new Vector<>();
        list.add(type.name());
        metaData.putData(DATABASE_TYPE, list);
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
}

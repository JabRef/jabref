package org.jabref.gui.importer;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.logic.importer.fileformat.CustomImporter;

public class ImporterViewModel {
    private final CustomImporter importer;
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty classname = new SimpleStringProperty();
    private final StringProperty basePath = new SimpleStringProperty();

    public ImporterViewModel(CustomImporter importer) {
        this.importer = importer;
        this.name.setValue(importer.getName());
        this.classname.setValue(importer.getClassName());
        this.basePath.setValue(importer.getBasePath().toString());
    }

    public CustomImporter getLogic() {
        return this.importer;
    }

    public StringProperty name() {
        return this.name;
    }

    public StringProperty className() {
        return this.classname;
    }

    public StringProperty basePath() {
        return this.basePath;
    }
}

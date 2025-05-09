package org.jabref.logic.exporter;

import org.jabref.model.metadata.SaveOrder;
import org.jabref.model.metadata.SelfContainedSaveOrder;

/**
 * This is a {@link SaveOrder} where the contained saveConfiguration is a {@link org.jabref.model.metadata.SelfContainedSaveOrder}
 */
public class SelfContainedSaveConfiguration extends SaveConfiguration {
    public SelfContainedSaveConfiguration() {
        super();
    }

    public SelfContainedSaveConfiguration(
            SelfContainedSaveOrder saveOrder,
            Boolean makeBackup,
            BibDatabaseWriter.SaveType saveType,
            Boolean reformatFile) {
        super(saveOrder, makeBackup, saveType, reformatFile);
    }

    public SelfContainedSaveOrder getSelfContainedSaveOrder() {
        return (SelfContainedSaveOrder) getSaveOrder();
    }
}

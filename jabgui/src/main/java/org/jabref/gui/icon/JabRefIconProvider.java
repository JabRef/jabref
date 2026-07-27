package org.jabref.gui.icon;

import org.jspecify.annotations.NullMarked;
import org.kordamp.ikonli.IkonProvider;

@NullMarked
public class JabRefIconProvider implements IkonProvider<JabRefMaterialDesignIcon> {

    @Override
    public Class<JabRefMaterialDesignIcon> getIkon() {
        return JabRefMaterialDesignIcon.class;
    }
}

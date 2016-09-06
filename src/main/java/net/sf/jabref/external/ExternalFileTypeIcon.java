package net.sf.jabref.external;

import javax.swing.Icon;
import javax.swing.JLabel;

import net.sf.jabref.gui.IconTheme;

public class ExternalFileTypeIcon {

    public static Icon getIcon(ExternalFileType fileType) {
        return new IconTheme.FontBasedIcon(fileType.getMaterialDesignIconCodePoint(), IconTheme.DEFAULT_COLOR,
                IconTheme.SMALL_SIZE);
    }

    public static JLabel getIconLabel(ExternalFileType fileType) {
        JLabel label = new JLabel(getIcon(fileType));
        label.setText(null);
        label.setToolTipText(fileType.getName());
        return label;
    }
}

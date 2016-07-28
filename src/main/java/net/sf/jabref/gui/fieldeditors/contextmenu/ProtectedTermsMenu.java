package net.sf.jabref.gui.fieldeditors.contextmenu;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.text.JTextComponent;

import net.sf.jabref.Globals;
import net.sf.jabref.logic.formatter.casechanger.ProtectTermsFormatter;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.protectedterms.ProtectedTermsList;

public class ProtectedTermsMenu extends JMenu {

    private static final ProtectTermsFormatter formatter = new ProtectTermsFormatter();
    private final JMenu externalFiles;
    private final JTextComponent opener;

    public ProtectedTermsMenu(JTextComponent opener) {
        super(Localization.lang("Protect terms"));
        this.opener = opener;
        JMenuItem protectItem = new JMenuItem(Localization.lang("Add {} around selected text"));
        protectItem.addActionListener(event -> {
            String selectedText = opener.getSelectedText();
            if ((selectedText != null) && !selectedText.isEmpty()) {
                opener.replaceSelection("{" + selectedText + "}");
            }
        });

        JMenuItem formatItem = new JMenuItem(Localization.lang("Format field"));
        formatItem.addActionListener(event -> opener.setText(formatter.format(opener.getText())));

        externalFiles = new JMenu(Localization.lang("Add selected text to list"));
        updateFiles();

        /* It would be nice to add a new file from the menu, but I do not realize how to get hold of JabRefFrame from here as a parent JFrame
        externalFiles.addSeparator();
        JMenuItem addToNewFileItem = new JMenuItem("New file" + "...");
        addToNewFileItem.addActionListener(event -> {
            NewProtectedTermsFileDialog dialog = new NewProtectedTermsFileDialog();
        });

        */

        this.add(protectItem);
        this.add(externalFiles);
        this.addSeparator();
        this.add(formatItem);
    }

    public void updateFiles() {
        externalFiles.removeAll();
        for (ProtectedTermsList list : Globals.protectedTermsLoader.getProtectedTermsLists()) {
            if (!list.isInternalList()) {
                JMenuItem fileItem = new JMenuItem(list.getDescription());
                externalFiles.add(fileItem);
                fileItem.addActionListener(event -> {String selectedText = opener.getSelectedText();
                if((selectedText != null) && !selectedText.isEmpty()) {
                    list.addProtectedTerm(selectedText);
                    }
                });
            }
        }

    }

}
